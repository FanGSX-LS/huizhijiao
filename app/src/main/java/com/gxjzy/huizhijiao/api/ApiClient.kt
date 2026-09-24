package com.gxjzy.huizhijiao.api

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.gxjzy.huizhijiao.BuildConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.gxjzy.huizhijiao.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var baseUrl = "http://lt.gecho.cn:8011/"
    private var token: String? = null
    private var cachedUser: UserInfo? = null
    var expiredDialogShownThisSession = false

    private var _service: ApiService? = null
    val service: ApiService
        get() {
            val current = _service
            if (current != null && currentBaseUrl == baseUrl) return current
            createService()
            return _service!!
        }

    val gson = GsonBuilder().setLenient().create()

    private var currentBaseUrl: String = baseUrl

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun setBaseUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        baseUrl = "$trimmed/"
    }

    fun setToken(t: String?) { token = t }
    fun getBaseUrl(): String = baseUrl
    fun setCachedUser(user: UserInfo?) { cachedUser = user }
    fun getCachedUser(): UserInfo? = cachedUser

    fun clearCache() {
        cachedUser = null
        _service = null
    }

    private fun createService() {
        currentBaseUrl = baseUrl
        _service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    private suspend fun proxyCall(urlSuffix: String, data: Map<String, String>): JsonObject? {
        if (token.isNullOrEmpty()) return null
        return try {
            val resp = service.proxy(ProxyRequestBody(urlSuffix, data))
            if (resp.isSuccessful) resp.body() else null
        } catch (_: Exception) { null }
    }

    private suspend fun proxyCallWithError(urlSuffix: String, data: Map<String, String>): Pair<JsonObject?, String> {
        if (token.isNullOrEmpty()) return Pair(null, "未登录")
        return try {
            val resp = service.proxy(ProxyRequestBody(urlSuffix, data))
            if (resp.isSuccessful) Pair(resp.body(), "") else Pair(null, "HTTP ${resp.code()}")
        } catch (e: Exception) { Pair(null, e.message ?: "网络错误") }
    }

    private suspend fun ensureUser(): UserInfo? {
        if (cachedUser != null) return cachedUser
        if (token.isNullOrEmpty()) return null
        return fetchMe()
    }

    private fun parseUser(raw: JsonObject): UserInfo {
        val intern = raw.getAsJsonObject("internship")
        return UserInfo(
            id = safeLong(raw.get("id")),
            name = safeString(raw.get("name")),
            number = safeString(raw.get("number")),
            studentId = safeString(raw.get("studentId")),
            clazzName = safeString(raw.get("clazzName")),
            mobilePhone = safeString(raw.get("mobilePhone")),
            avatarUrl = safeString(raw.get("avatarUrl")),
            advancedModeStart = safeString(raw.get("advancedModeStart")),
            advancedModeEnd = safeString(raw.get("advancedModeEnd")),
            advancedModeExpired = safeBool(raw.get("advancedModeExpired")),
            internship = if (intern != null) InternshipInfo(
                internshipId = safeLong(intern.get("internshipId")),
                internshipName = safeString(intern.get("internshipName")),
                internType = safeString(intern.get("internType")),
                classHour = safeInt(intern.get("classHour")),
                credit = safeDouble(intern.get("credit")),
                teacherName = safeString(intern.get("teacherName")),
                startDate = safeString(intern.get("startDate")),
                endDate = safeString(intern.get("endDate")),
                companyName = safeString(intern.get("companyName")),
                leastSignIn = safeInt(intern.get("leastSignIn")),
                signedDays = safeInt(intern.get("signedDays")),
                studentInternshipId = safeLong(intern.get("studentInternshipId"))
            ) else null
        )
    }

    suspend fun login(loginName: String, password: String, schoolId: String): LoginResult {
        val result = LoginResult()
        try {
            val resp = service.login(mapOf("loginName" to loginName, "password" to password, "schoolId" to schoolId))
            if (resp.isSuccessful) {
                val body = resp.body() ?: return result
                result.success = safeBool(body.get("success"))
                result.msg = safeString(body.get("msg"))
                if (result.success) {
                    result.token = safeString(body.get("token"))
                    result.role = safeString(body.get("role"))
                    val userObj = body.getAsJsonObject("user")
                    if (userObj != null) {
                        result.user = parseUser(userObj)
                        cachedUser = result.user
                    }
                }
            }
        } catch (e: Exception) { result.msg = e.message ?: "网络错误" }
        return result
    }

    suspend fun adminLogin(username: String, password: String): AdminLoginResult {
        val result = AdminLoginResult()
        try {
            val resp = service.adminLogin(mapOf("username" to username, "password" to password))
            if (resp.isSuccessful) {
                val body = resp.body() ?: return result
                result.success = safeBool(body.get("success"))
                result.msg = safeString(body.get("msg"))
                if (result.success) {
                    result.token = safeString(body.get("token"))
                    result.role = safeString(body.get("role"), "admin")
                }
            }
        } catch (e: Exception) { result.msg = e.message ?: "网络错误" }
        return result
    }

    suspend fun fetchMe(): UserInfo? {
        if (token.isNullOrEmpty()) return null
        return try {
            val resp = service.getMe()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return null
                if (safeBool(body.get("success"))) {
                    val userObj = body.getAsJsonObject("user")
                    if (userObj != null) {
                        val user = parseUser(userObj)
                        cachedUser = user
                        user
                    } else null
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun fetchCheckinProgress(): CheckinProgressData? {
        return try {
            val resp = service.getCheckinProgress()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return null
                if (safeBool(body.get("success"))) {
                    val d = body.getAsJsonObject("data") ?: return null
                    CheckinProgressData(
                        signedDays = safeInt(d.get("signedDays")),
                        leastSignIn = safeInt(d.get("leastSignIn"))
                    )
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun fetchCheckinDates(year: Int, month: Int): List<Int> {
        return try {
            val resp = service.getCheckinDates(year, month)
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { safeInt(it) }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchCheckinCalendar(year: Int, month: Int): List<CalendarEntry> {
        return try {
            val resp = service.getCheckinCalendar(year, month)
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        CalendarEntry(
                            date = obj.get("date")?.asString ?: "",
                            checkType = obj.get("checkType")?.asString ?: "",
                            status = safeBool(obj.get("status"), true),
                            isAbnormal = safeBool(obj.get("isAbnormal")),
                            isReissue = safeBool(obj.get("isReissue")),
                            label = obj.get("label")?.asString ?: "",
                            createTime = safeLong(obj.get("createTime"))
                        )
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchWeeks(): List<WeekItem> {
        val user = ensureUser() ?: return emptyList()
        val data = mapOf(
            "studentId" to user.studentId,
            "internshipId" to (user.internship?.internshipId?.toString() ?: ""),
            "studentInternshipId" to (user.internship?.studentInternshipId?.toString() ?: "")
        )
        val res = proxyCall("process/weekly-report/get-stuIntern-weaks", data) ?: return emptyList()
        return try {
            val arr = res.getAsJsonArray("data") ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem.asJsonObject
                WeekItem(
                    week = safeInt(obj.get("week")),
                    startDate = safeString(obj.get("startDate")).ifEmpty { safeString(obj.get("weekStartDate")) },
                    endDate = safeString(obj.get("endDate")).ifEmpty { safeString(obj.get("weekEndDate")) }
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchMonths(): List<MonthItem> {
        val user = ensureUser() ?: return emptyList()
        val data = mapOf(
            "studentId" to user.studentId,
            "internshipId" to (user.internship?.internshipId?.toString() ?: ""),
            "studentInternshipId" to (user.internship?.studentInternshipId?.toString() ?: "")
        )
        val res = proxyCall("process/month-summary/get-stuIntern-months", data) ?: return emptyList()
        return try {
            val arr = res.getAsJsonArray("data") ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem.asJsonObject
                MonthItem(month = safeInt(obj.get("month")), startDate = safeString(obj.get("startDate")), endDate = safeString(obj.get("endDate")))
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun saveWeekly(content: String, week: String, weekStartDate: String, weekEndDate: String, isDraft: String, siteInstruction: String, contactTimes: String): Pair<Boolean, String> {
        val data = mapOf("content" to content, "week" to week, "weekStartDate" to weekStartDate, "weekEndDate" to weekEndDate, "isDraft" to isDraft, "siteInstruction" to siteInstruction, "contactTimes" to contactTimes, "attachIds" to "", "delAttachIds" to "")
        val res = proxyCallWithError("process/weekly-report/save", data)
        if (res.first == null) return Pair(false, res.second.ifEmpty { "网络错误" })
        return try {
            val success = safeBool(res.first!!.get("success"))
            val msg = safeString(res.first!!.get("msg")).ifEmpty { if (success) "提交成功" else "提交失败" }
            Pair(success, msg)
        } catch (_: Exception) { Pair(false, "解析错误") }
    }

    suspend fun saveMonthly(content: String, month: String, startDate: String, endDate: String, isDraft: String): Pair<Boolean, String> {
        val data = mapOf("content" to content, "month" to month, "startDate" to startDate, "endDate" to endDate, "isDraft" to isDraft, "attachIds" to "", "delAttachIds" to "")
        val res = proxyCallWithError("process/month-summary/save", data)
        if (res.first == null) return Pair(false, res.second.ifEmpty { "网络错误" })
        return try {
            val success = safeBool(res.first!!.get("success"))
            val msg = safeString(res.first!!.get("msg")).ifEmpty { if (success) "提交成功" else "提交失败" }
            Pair(success, msg)
        } catch (_: Exception) { Pair(false, "解析错误") }
    }

    suspend fun doCheckin(presetData: PresetData, checkType: String): CheckinResult {
        val fmtScale = if (presetData.scale == presetData.scale.toInt().toDouble()) presetData.scale.toInt().toString() else presetData.scale.toString()
        val data = mapOf("locationX" to presetData.locationX.toString(), "locationY" to presetData.locationY.toString(), "label" to presetData.label, "scale" to fmtScale, "mapType" to presetData.mapType, "isAbnormal" to if (presetData.isAbnormal) "1" else "0", "isEvection" to if (presetData.isEvection) "1" else "0", "checkType" to checkType, "content" to "", "attachIds" to "")
        val (res, err) = proxyCallWithError("process/stu-location/save", data)
        if (res == null) return CheckinResult(success = false, msg = err.ifEmpty { "网络错误" })
        return try {
            val success = safeBool(res.get("success"))
            val msg = safeString(res.get("msg")).ifEmpty { if (success) "操作成功" else "操作失败" }
            CheckinResult(success = success, msg = msg)
        } catch (_: Exception) { CheckinResult(success = false, msg = "解析错误") }
    }

    suspend fun fetchCheckinRecords(startDate: String, endDate: String): List<CheckinRecord> {
        val data = mapOf("startDate" to startDate, "endDate" to endDate)
        val res = proxyCall("process/stu-location/getMylistByDate", data)
        if (res == null) return emptyList()
        return try {
            val d = res.getAsJsonObject("data") ?: return emptyList()
            val arr = d.getAsJsonArray("list") ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem.asJsonObject
                CheckinRecord(
                    id = safeLong(obj.get("id")), studentId = safeLong(obj.get("studentId")),
                    studentName = safeString(obj.get("studentName")), checkType = safeString(obj.get("checkType")),
                    status = safeBool(obj.get("status"), true), isAbnormal = safeBool(obj.get("isAbnormal")),
                    isReissue = safeBool(obj.get("isReissue")), isEvection = safeBool(obj.get("isEvection")),
                    sendTime = safeLong(obj.get("sendTime")), createTime = safeLong(obj.get("createTime")),
                    locationX = safeString(obj.get("locationX")), locationY = safeString(obj.get("locationY")),
                    label = safeString(obj.get("label")), mapType = safeString(obj.get("mapType")),
                    scale = safeDouble(obj.get("scale")), content = safeString(obj.get("content")),
                    auditOpinion = safeString(obj.get("auditOpinion")), isIgnore = safeBool(obj.get("isIgnore"))
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun deleteWeekly(id: String): Boolean {
        val res = proxyCall("process/weekly-report/delete", mapOf("id" to id)) ?: return false
        return try { safeBool(res.get("success")) } catch (_: Exception) { false }
    }

    suspend fun deleteMonthly(id: String): Boolean {
        val res = proxyCall("process/month-summary/delete", mapOf("id" to id)) ?: return false
        return try { safeBool(res.get("success")) } catch (_: Exception) { false }
    }

    suspend fun fetchWeekList(): List<ManageItem> {
        val user = ensureUser() ?: return emptyList()
        val data = mapOf("internshipId" to (user.internship?.internshipId?.toString() ?: ""))
        val res = proxyCall("process/weekly-report/find-app-write-week-maps", data) ?: return emptyList()
        return try {
            val arr = res.getAsJsonArray("data") ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem.asJsonObject
                val weekNum = safeString(obj.get("week"))
                val startDate = safeString(obj.get("startDate")).ifEmpty { safeString(obj.get("weekStartDate")) }
                val endDate = safeString(obj.get("endDate")).ifEmpty { safeString(obj.get("weekEndDate")) }
                val isDraft = obj.get("isDraft")?.asBoolean == true
                ManageItem(id = safeLong(obj.get("id")), title = "", subtitle = "", content = safeString(obj.get("content")), week = weekNum, month = "", startDate = startDate, endDate = endDate, isDraft = isDraft, createTime = safeLong(obj.get("createTime")))
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchMonthList(): List<ManageItem> {
        val user = ensureUser() ?: return emptyList()
        val data = mapOf("internshipId" to (user.internship?.internshipId?.toString() ?: ""))
        val res = proxyCall("process/month-summary/find-app-write-month-maps", data) ?: return emptyList()
        return try {
            val arr = res.getAsJsonArray("data") ?: return emptyList()
            arr.mapNotNull { elem ->
                val obj = elem.asJsonObject
                val monthNum = safeString(obj.get("month"))
                val startDate = safeString(obj.get("startDate"))
                val endDate = safeString(obj.get("endDate"))
                val isDraft = obj.get("isDraft")?.asBoolean == true
                ManageItem(id = safeLong(obj.get("id")), title = "", subtitle = "", content = safeString(obj.get("content")), week = "", month = monthNum, startDate = startDate, endDate = endDate, isDraft = isDraft, createTime = safeLong(obj.get("createTime")))
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchWeeklyDetail(id: String): WeeklyDetail? {
        val res = proxyCall("process/weekly-report/view", mapOf("id" to id)) ?: return null
        return try {
            val d = res.getAsJsonObject("data") ?: return null
            val intern = d.getAsJsonObject("internship")
            WeeklyDetail(
                internshipName = safeString(intern?.get("name")), companyName = safeString(d.get("companyName")),
                content = safeString(d.get("content")), siteInstruction = safeString(d.get("siteInstruction")),
                contactTimes = safeString(d.get("contactTimes")), createTime = safeLong(d.get("createTime"))
            )
        } catch (_: Exception) { null }
    }

    suspend fun fetchMonthlyDetail(id: String): MonthlyDetail? {
        val res = proxyCall("process/month-summary/view", mapOf("id" to id)) ?: return null
        return try {
            val d = res.getAsJsonObject("data") ?: return null
            val intern = d.getAsJsonObject("internship")
            MonthlyDetail(internshipName = safeString(intern?.get("name")), companyName = safeString(d.get("companyName")), content = safeString(d.get("content")), createTime = safeLong(d.get("createTime")))
        } catch (_: Exception) { null }
    }

    suspend fun fetchWriteRecords(): WriteRecordsResult {
        val user = ensureUser() ?: return WriteRecordsResult()
        val data = mapOf("studentId" to user.studentId, "internshipId" to (user.internship?.internshipId?.toString() ?: ""), "studentInternshipId" to (user.internship?.studentInternshipId?.toString() ?: ""))
        var weekRes: JsonObject? = null
        var monthRes: JsonObject? = null
        coroutineScope {
            val weekDeferred = async { proxyCall("process/weekly-report/find-app-write-week-maps", data) }
            val monthDeferred = async { proxyCall("process/month-summary/find-app-write-month-maps", data) }
            weekRes = weekDeferred.await()
            monthRes = monthDeferred.await()
        }
        var weekFilled = false; var monthFilled = false
        try { if (weekRes != null) { val arr = weekRes.getAsJsonArray("data"); weekFilled = arr != null && arr.size() > 0 } } catch (_: Exception) {}
        try { if (monthRes != null) { val arr = monthRes.getAsJsonArray("data"); monthFilled = arr != null && arr.size() > 0 } } catch (_: Exception) {}
        return WriteRecordsResult(weekResult = weekFilled, monthResult = monthFilled)
    }

    suspend fun getPresets(presetType: String): List<PresetItem> {
        return try {
            val resp = service.getPresets(presetType)
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        val rawPresetData = obj.getAsJsonObject("data")
                        PresetItem(
                            id = safeLong(obj.get("id")), name = safeString(obj.get("name")),
                            data = if (rawPresetData != null) PresetData(
                                locationX = safeDouble(rawPresetData.get("locationX")),
                                locationY = safeDouble(rawPresetData.get("locationY")),
                                label = safeString(rawPresetData.get("label")),
                                scale = safeDouble(rawPresetData.get("scale")),
                                mapType = safeString(rawPresetData.get("mapType")),
                                isAbnormal = safeBool(rawPresetData.get("isAbnormal")),
                                isEvection = safeBool(rawPresetData.get("isEvection"))
                            ) else PresetData()
                        )
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getAutoCheckin(): AutoCheckinConfig? {
        return try {
            val resp = service.getAutoCheckin()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return null
                if (safeBool(body.get("success"))) {
                    val d = body.getAsJsonObject("data") ?: return null
                    AutoCheckinConfig(
                        isRunning = safeBool(d.get("isRunning")),
                        presetId = safeLong(d.get("presetId")),
                        presetName = safeString(d.get("presetName")),
                        startHour = safeInt(d.get("startHour"), 8), startMin = safeInt(d.get("startMin")),
                        endHour = safeInt(d.get("endHour"), 12), endMin = safeInt(d.get("endMin")),
                        skipDays = safeString(d.get("skipDays")), lastCheckinTime = safeLong(d.get("lastCheckinTime")), lastCheckinStatus = safeString(d.get("lastCheckinStatus")).ifEmpty { null }
                    )
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun startAutoCheckin(presetId: Long, startHour: Int, startMin: Int, endHour: Int, endMin: Int, skipDays: String): String? {
        return try {
            val json = gson.toJson(mapOf(
                "presetId" to presetId, "startHour" to startHour, "startMin" to startMin,
                "endHour" to endHour, "endMin" to endMin, "skipDays" to skipDays
            ))
            val body = json.toRequestBody("application/json".toMediaType())
            val resp = service.startAutoCheckin(body)
            if (resp.isSuccessful) {
                val b = resp.body()
                if (safeBool(b?.get("success"))) null
                else safeString(b?.get("message")).ifEmpty { "启动失败" }
            } else {
                try { resp.errorBody()?.string()?.let { "HTTP ${resp.code()}: $it" } } catch (_: Exception) { "HTTP ${resp.code()}" }
            }
        } catch (e: Exception) { "网络错误: ${e.message}" }
    }

    suspend fun stopAutoCheckin(): Boolean {
        return try {
            val resp = service.stopAutoCheckin()
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    suspend fun getAutoCheckinLogs(): List<AutoCheckinLog> {
        return try {
            val resp = service.getAutoCheckinLogs()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        AutoCheckinLog(id = safeLong(obj.get("id")), entry = safeString(obj.get("entry")), time = safeLong(obj.get("time")))
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun clearAutoCheckinLogs(): Boolean {
        return try {
            val resp = service.clearAutoCheckinLogs()
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    private fun parseAutoReportConfig(obj: JsonObject): AutoReportConfig {
        return AutoReportConfig(
            isRunning = safeBool(obj.get("isRunning")),
            triggerMode = safeString(obj.get("triggerMode")).ifEmpty { "auto" },
            startHour = safeInt(obj.get("startHour"), 8),
            startMin = safeInt(obj.get("startMin")),
            endHour = safeInt(obj.get("endHour"), 20),
            endMin = safeInt(obj.get("endMin")),
            selectedPeriods = safeString(obj.get("selectedPeriods")),
            lastReportTime = safeLong(obj.get("lastReportTime")),
            lastReportStatus = safeString(obj.get("lastReportStatus"))
        )
    }

    suspend fun getAutoReport(): AutoReportStatus? {
        return try {
            val resp = service.getAutoReport()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return null
                if (safeBool(body.get("success"))) {
                    val d = body.getAsJsonObject("data") ?: return null
                    val w = d.getAsJsonObject("week")
                    val m = d.getAsJsonObject("month")
                    AutoReportStatus(
                        week = if (w != null) parseAutoReportConfig(w) else AutoReportConfig(),
                        month = if (m != null) parseAutoReportConfig(m) else AutoReportConfig()
                    )
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun startAutoReport(reportType: String, triggerMode: String, startHour: Int, startMin: Int, endHour: Int, endMin: Int, selectedPeriods: String): String? {
        return try {
            val json = gson.toJson(mapOf(
                "reportType" to reportType, "triggerMode" to triggerMode,
                "startHour" to startHour, "startMin" to startMin,
                "endHour" to endHour, "endMin" to endMin,
                "selectedPeriods" to selectedPeriods
            ))
            val body = json.toRequestBody("application/json".toMediaType())
            val resp = service.startAutoReport(body)
            if (resp.isSuccessful) {
                val b = resp.body()
                if (safeBool(b?.get("success"))) null
                else safeString(b?.get("message")).ifEmpty { "启动失败" }
            } else {
                try { resp.errorBody()?.string()?.let { "HTTP ${resp.code()}: $it" } } catch (_: Exception) { "HTTP ${resp.code()}" }
            }
        } catch (e: Exception) { "网络错误: ${e.message}" }
    }

    suspend fun stopAutoReport(reportType: String): Boolean {
        return try {
            val json = gson.toJson(mapOf("reportType" to reportType))
            val body = json.toRequestBody("application/json".toMediaType())
            val resp = service.stopAutoReport(body)
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    suspend fun getAutoReportLogs(type: String? = null): List<AutoReportLog> {
        return try {
            val resp = service.getAutoReportLogs(type)
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        AutoReportLog(entry = safeString(obj.get("entry")), time = safeLong(obj.get("time")))
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun clearAutoReportLogs(type: String? = null): Boolean {
        return try {
            val resp = service.clearAutoReportLogs(type)
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    private fun millisToDateString(ms: Long): String {
        if (ms <= 0) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date(ms))
    }

    suspend fun fetchSummary(internshipId: String): SummaryItem? {
        val res = proxyCall("process/summary/stu-list", mapOf("limit" to "10", "start" to "0", "conds[internshipId]" to internshipId)) ?: return null
        return try {
            val d = res.getAsJsonObject("data") ?: return null
            val arr = d.getAsJsonArray("result") ?: return null
            if (arr.size() == 0) return null
            val obj = arr[0].asJsonObject
            SummaryItem(
                id = safeLong(obj.get("id")),
                content = safeString(obj.get("content")),
                startDate = millisToDateString(safeLong(obj.get("startDate"))),
                endDate = millisToDateString(safeLong(obj.get("endDate"))),
                companyEval = safeString(obj.get("companyEval")).ifEmpty { "3" },
                isDraft = safeBool(obj.get("isDraft"), true),
                createTime = safeLong(obj.get("createTime"))
            )
        } catch (_: Exception) { null }
    }

    suspend fun saveSummary(content: String, startDate: String, endDate: String, companyEval: String, isDraft: String): Boolean {
        val data = mapOf(
            "content" to content, "startDate" to startDate, "endDate" to endDate,
            "companyEval" to companyEval, "isDraft" to isDraft,
            "attachIds" to "", "delAttachIds" to ""
        )
        val res = proxyCall("process/summary/save", data) ?: return false
        return try { safeBool(res.get("success")) } catch (_: Exception) { false }
    }

    suspend fun getTemplates(tplType: String): List<TemplateItem> {
        return try {
            val resp = service.getTemplates(tplType)
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                val arr = when {
                    body.isJsonArray -> body.asJsonArray
                    body.isJsonObject -> {
                        val obj = body.asJsonObject
                        if (safeBool(obj.get("success"))) obj.getAsJsonArray("data") ?: return emptyList()
                        else return emptyList()
                    }
                    else -> return emptyList()
                }
                arr.mapNotNull { elem ->
                    val obj = elem.asJsonObject
                    val dataElement = obj.get("data")
                    val dataObj = if (dataElement != null && !dataElement.isJsonNull) {
                        if (dataElement.isJsonObject) dataElement.asJsonObject
                        else if (dataElement.isJsonPrimitive && dataElement.asJsonPrimitive.isString) {
                            try { com.google.gson.JsonParser.parseString(dataElement.asString).asJsonObject } catch (_: Exception) { null }
                        } else null
                    } else null
                    TemplateItem(
                        id = safeLong(obj.get("id")),
                        sortKey = safeInt(obj.get("sort_key")),
                        tplType = safeString(obj.get("tpl_type")).ifEmpty { tplType },
                        data = if (dataObj != null) TemplateData(
                            label = safeString(dataObj.get("label")),
                            content = safeString(dataObj.get("content")),
                            siteInstruction = safeString(dataObj.get("siteInstruction")),
                            contactTimes = safeString(dataObj.get("contactTimes")),
                            week = safeInt(dataObj.get("week")),
                            months = safeInt(dataObj.get("months"))
                        ) else TemplateData(
                            label = run {
                                val w = safeInt(obj.get("week"))
                                val m = safeInt(obj.get("months"))
                                val num = if (tplType == "week") w else m
                                "第${num}${if (tplType == "week") "周" else "月"}"
                            },
                            content = safeString(obj.get("content")),
                            siteInstruction = safeString(obj.get("siteInstruction")),
                            contactTimes = safeString(obj.get("contactTimes")),
                            week = safeInt(obj.get("week")),
                            months = safeInt(obj.get("months"))
                        )
                    )
                }
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun saveTemplatesRemote(tplType: String, items: List<TemplateItem>): Boolean {
        return try {
            val jsonItems = items.mapIndexed { idx, item ->
                mapOf(
                    "sort_key" to idx,
                    "data" to mapOf(
                        "label" to item.data.label,
                        "content" to item.data.content,
                        "siteInstruction" to item.data.siteInstruction,
                        "contactTimes" to item.data.contactTimes,
                        "week" to item.data.week,
                        "months" to item.data.months
                    )
                )
            }
            val json = gson.toJson(mapOf("items" to jsonItems))
            val body = json.toRequestBody("application/json".toMediaType())
            val resp = service.saveTemplates(tplType, body)
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    suspend fun importTemplatesRemote(tplType: String, items: List<Map<String, Any?>>): Boolean {
        return try {
            val json = gson.toJson(mapOf("items" to items))
            val body = json.toRequestBody("application/json".toMediaType())
            val resp = service.importTemplates(tplType, body)
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    suspend fun getAdminUsers(): List<AdminUser> {
        return try {
            val resp = service.getAdminUsers()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        val ac = obj.getAsJsonObject("autoCheckin")
                        AdminUser(
                            id = safeLong(obj.get("id")), loginName = safeString(obj.get("loginName")),
                            studentName = safeString(obj.get("studentName")), studentNumber = safeString(obj.get("studentNumber")),
                            clazzName = safeString(obj.get("clazzName")), internshipName = safeString(obj.get("internshipName")),
                            isActive = safeBool(obj.get("isActive"), true), lastLogin = safeString(obj.get("lastLogin")).ifEmpty { null },
                            advancedModeStart = safeString(obj.get("advancedModeStart")),
                            advancedModeEnd = safeString(obj.get("advancedModeEnd")),
                            autoCheckin = if (ac != null) AutoCheckinConfig(
                                isRunning = safeBool(ac.get("isRunning")), skipDays = safeString(ac.get("skipDays")), lastCheckinTime = safeLong(ac.get("lastCheckinTime")), lastCheckinStatus = safeString(ac.get("lastStatus")).ifEmpty { null }
                            ) else null
                        )
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getAdminAutoCheckins(): List<AdminAutoCheckin> {
        return try {
            val resp = service.getAdminAutoCheckins()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        AdminAutoCheckin(
                            id = safeLong(obj.get("id")), userId = safeLong(obj.get("userId")),
                            loginName = safeString(obj.get("loginName")), studentName = safeString(obj.get("studentName")),
                            presetName = safeString(obj.get("presetName")), lastStatus = safeString(obj.get("lastStatus")).ifEmpty { null }, lastTime = safeString(obj.get("lastTime")).ifEmpty { null }
                        )
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun adminSetAdvancedMode(userId: Long, startDate: String, endDate: String, preset: String = ""): String? {
        return try {
            val data = mutableMapOf<String, String>("startDate" to startDate, "endDate" to endDate)
            if (preset.isNotEmpty()) data["preset"] = preset
            val json = gson.toJson(data)
            val body = json.toRequestBody("application/json".toMediaType())
            val resp = service.setAdvancedMode(userId, body)
            if (resp.isSuccessful) {
                val b = resp.body()
                if (safeBool(b?.get("success"))) null
                else safeString(b?.get("msg")).ifEmpty { "设置失败" }
            } else "HTTP ${resp.code()}"
        } catch (e: Exception) { "网络错误: ${e.message}" }
    }

    suspend fun adminClearAdvancedMode(userId: Long): String? {
        return try {
            val resp = service.clearAdvancedMode(userId)
            if (resp.isSuccessful) {
                val b = resp.body()
                if (safeBool(b?.get("success"))) null
                else "清除失败"
            } else "HTTP ${resp.code()}"
        } catch (e: Exception) { "网络错误: ${e.message}" }
    }

    suspend fun fetchUnreadAnnouncements(): List<Announcement> {
        return try {
            val resp = service.getAnnouncements()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return emptyList()
                if (safeBool(body.get("success"))) {
                    val arr = body.getAsJsonArray("data") ?: return emptyList()
                    arr.mapNotNull { elem ->
                        val obj = elem.asJsonObject
                        Announcement(
                            id = safeLong(obj.get("id")),
                            title = safeString(obj.get("title")),
                            content = safeString(obj.get("content")),
                            createdAt = safeLong(obj.get("createdAt"))
                        )
                    }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun markAnnouncementRead(announcementId: Long): Boolean {
        return try {
            val resp = service.markAnnouncementRead(announcementId)
            if (resp.isSuccessful) safeBool(resp.body()?.get("success")) else false
        } catch (_: Exception) { false }
    }

    private fun safeString(element: com.google.gson.JsonElement?, default: String = ""): String {
        if (element == null || element.isJsonNull) return default
        return try { element.asString } catch (_: Exception) { default }
    }

    private fun safeInt(element: com.google.gson.JsonElement?, default: Int = 0): Int {
        if (element == null || element.isJsonNull) return default
        return try {
            if (element.isJsonPrimitive) {
                val p = element.asJsonPrimitive
                when { p.isNumber -> p.asInt; p.isString -> p.asString.toIntOrNull() ?: default; else -> default }
            } else default
        } catch (_: Exception) { default }
    }

    private fun safeLong(element: com.google.gson.JsonElement?, default: Long = 0): Long {
        if (element == null || element.isJsonNull) return default
        return try {
            if (element.isJsonPrimitive) {
                val p = element.asJsonPrimitive
                when { p.isNumber -> p.asLong; p.isString -> p.asString.toLongOrNull() ?: default; else -> default }
            } else default
        } catch (_: Exception) { default }
    }

    private fun safeDouble(element: com.google.gson.JsonElement?, default: Double = 0.0): Double {
        if (element == null || element.isJsonNull) return default
        return try {
            if (element.isJsonPrimitive) {
                val p = element.asJsonPrimitive
                when { p.isNumber -> p.asDouble; p.isString -> p.asString.toDoubleOrNull() ?: default; else -> default }
            } else default
        } catch (_: Exception) { default }
    }

    private fun safeBool(element: com.google.gson.JsonElement?, default: Boolean = false): Boolean {
        if (element == null || element.isJsonNull) return default
        return try {
            if (element.isJsonPrimitive) {
                val p = element.asJsonPrimitive
                when { p.isBoolean -> p.asBoolean; p.isNumber -> p.asInt != 0; p.isString -> p.asString.lowercase() == "true" || p.asString == "1"; else -> default }
            } else default
        } catch (_: Exception) { default }
    }

    private fun parseTimestamp(element: com.google.gson.JsonElement?): Long = safeLong(element)
}
