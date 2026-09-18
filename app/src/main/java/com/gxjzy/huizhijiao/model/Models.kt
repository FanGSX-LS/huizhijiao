package com.gxjzy.huizhijiao.model

import com.google.gson.annotations.SerializedName

data class UserInfo(
    val id: Long = 0,
    val name: String = "",
    val number: String = "",
    @SerializedName("studentId") val studentId: String = "",
    @SerializedName("clazzName") val clazzName: String = "",
    @SerializedName("mobilePhone") val mobilePhone: String = "",
    @SerializedName("avatarUrl") val avatarUrl: String = "",
    @SerializedName("advancedModeStart") val advancedModeStart: String = "",
    @SerializedName("advancedModeEnd") val advancedModeEnd: String = "",
    @SerializedName("advancedModeExpired") val advancedModeExpired: Boolean = false,
    val internship: InternshipInfo? = null
)

data class InternshipInfo(
    @SerializedName("internshipId") val internshipId: Long = 0,
    @SerializedName("internshipName") val internshipName: String = "",
    @SerializedName("internType") val internType: String = "",
    @SerializedName("classHour") val classHour: Int = 0,
    val credit: Double = 0.0,
    @SerializedName("teacherName") val teacherName: String = "",
    @SerializedName("startDate") val startDate: String = "",
    @SerializedName("endDate") val endDate: String = "",
    @SerializedName("companyName") val companyName: String = "",
    @SerializedName("leastSignIn") val leastSignIn: Int = 0,
    @SerializedName("signedDays") val signedDays: Int = 0,
    @SerializedName("studentInternshipId") val studentInternshipId: Long = 0
)

data class LoginResult(
    var success: Boolean = false,
    var role: String = "",
    var token: String = "",
    var user: UserInfo? = null,
    var msg: String = ""
)

data class AdminLoginResult(
    var success: Boolean = false,
    var role: String = "",
    var token: String = "",
    var msg: String = ""
)

data class SavedCredentials(
    val loginName: String = "",
    val password: String = "",
    val schoolId: String = ""
)

data class CheckinProgressData(
    @SerializedName("signedDays") val signedDays: Int = 0,
    @SerializedName("leastSignIn") val leastSignIn: Int = 0
) {
    constructor(signedDays: String, leastSignIn: String) : this(
        signedDays.toIntOrNull() ?: 0,
        leastSignIn.toIntOrNull() ?: 0
    )
}

data class CheckinLog(
    val entry: String = "",
    val time: String = ""
)

data class CheckinRecord(
    val id: Long = 0,
    @SerializedName("studentId") val studentId: Long = 0,
    @SerializedName("studentName") val studentName: String = "",
    @SerializedName("checkType") val checkType: String = "",
    val status: Boolean = true,
    @SerializedName("isAbnormal") val isAbnormal: Boolean = false,
    @SerializedName("isReissue") val isReissue: Boolean = false,
    @SerializedName("isEvection") val isEvection: Boolean = false,
    @SerializedName("sendTime") val sendTime: Long = 0,
    @SerializedName("createTime") val createTime: Long = 0,
    @SerializedName("locationX") val locationX: String = "",
    @SerializedName("locationY") val locationY: String = "",
    val label: String = "",
    @SerializedName("mapType") val mapType: String = "",
    val scale: Double? = null,
    val content: String = "",
    @SerializedName("auditOpinion") val auditOpinion: String = "",
    @SerializedName("isIgnore") val isIgnore: Boolean = false
)

data class AutoCheckinConfig(
    @SerializedName("isRunning") val isRunning: Boolean = false,
    @SerializedName("presetId") val presetId: Long = 0,
    @SerializedName("presetName") val presetName: String = "",
    @SerializedName("startHour") val startHour: Int = 0,
    @SerializedName("startMin") val startMin: Int = 0,
    @SerializedName("endHour") val endHour: Int = 0,
    @SerializedName("endMin") val endMin: Int = 0,
    @SerializedName("skipDays") val skipDays: String = "",
    @SerializedName("lastCheckinTime") val lastCheckinTime: Long = 0,
    @SerializedName("lastCheckinStatus") val lastCheckinStatus: String? = null
)

data class AutoCheckinLog(
    val id: Long = 0,
    val entry: String = "",
    val time: Long = 0
)

data class PresetData(
    @SerializedName("locationX") val locationX: Double = 0.0,
    @SerializedName("locationY") val locationY: Double = 0.0,
    val label: String = "",
    val scale: Double = 0.0,
    @SerializedName("mapType") val mapType: String = "",
    @SerializedName("isAbnormal") val isAbnormal: Boolean = false,
    @SerializedName("isEvection") val isEvection: Boolean = false
)

data class PresetItem(
    val id: Long = 0,
    val name: String = "",
    val data: PresetData = PresetData()
)

data class TemplateData(
    val label: String = "",
    val content: String = "",
    @SerializedName("siteInstruction") val siteInstruction: String = "",
    @SerializedName("contactTimes") val contactTimes: String = "",
    @SerializedName("week") val week: Int = 0,
    @SerializedName("months") val months: Int = 0
)

data class TemplateItem(
    val id: Long = 0,
    @SerializedName("sortKey") val sortKey: Int = 0,
    @SerializedName("tplType") val tplType: String = "week",
    val data: TemplateData = TemplateData()
)

data class WeeklyDetail(
    @SerializedName("internshipName") val internshipName: String = "",
    @SerializedName("companyName") val companyName: String = "",
    val content: String = "",
    @SerializedName("siteInstruction") val siteInstruction: String = "",
    @SerializedName("contactTimes") val contactTimes: String = "",
    val createTime: Long = 0
)

data class MonthlyDetail(
    @SerializedName("internshipName") val internshipName: String = "",
    @SerializedName("companyName") val companyName: String = "",
    val content: String = "",
    val createTime: Long = 0
)

data class ManageItem(
    val id: Long = 0,
    val title: String = "",
    val subtitle: String = "",
    val content: String = "",
    val week: String = "",
    val month: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isDraft: Boolean = false,
    val createTime: Long = 0
)

data class WeekItem(
    val week: Int = 0,
    val startDate: String = "",
    val endDate: String = ""
)

data class MonthItem(
    val month: Int = 0,
    val startDate: String = "",
    val endDate: String = ""
)

data class WriteRecordsResult(
    val weekResult: Boolean = false,
    val monthResult: Boolean = false
)

data class AdminUser(
    val id: Long = 0,
    @SerializedName("loginName") val loginName: String = "",
    @SerializedName("studentName") val studentName: String = "",
    @SerializedName("studentNumber") val studentNumber: String = "",
    @SerializedName("clazzName") val clazzName: String = "",
    @SerializedName("internshipName") val internshipName: String = "",
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("lastLogin") val lastLogin: String? = null,
    @SerializedName("advancedModeStart") val advancedModeStart: String = "",
    @SerializedName("advancedModeEnd") val advancedModeEnd: String = "",
    val autoCheckin: AutoCheckinConfig? = null
)

data class AdminAutoCheckin(
    val id: Long = 0,
    val userId: Long = 0,
    val loginName: String = "",
    val studentName: String = "",
    @SerializedName("presetName") val presetName: String = "",
    @SerializedName("lastStatus") val lastStatus: String? = null,
    @SerializedName("lastTime") val lastTime: String? = null
)

data class FuncItem(
    val id: String = "",
    val label: String = "",
    val icon: String = "",
    val page: String = ""
)

data class ApiResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null
)

data class CalendarEntry(
    val date: String = "",
    val checkType: String = "",
    val status: Boolean = true,
    val isAbnormal: Boolean = false,
    val isReissue: Boolean = false,
    val label: String = "",
    val createTime: Long = 0
)

data class AutoReportConfig(
    val isRunning: Boolean = false,
    val triggerMode: String = "auto",
    val startHour: Int = 8,
    val startMin: Int = 0,
    val endHour: Int = 20,
    val endMin: Int = 0,
    val selectedPeriods: String = "",
    val lastReportTime: Long = 0,
    val lastReportStatus: String = ""
)

data class AutoReportStatus(
    val week: AutoReportConfig = AutoReportConfig(),
    val month: AutoReportConfig = AutoReportConfig()
)

data class AutoReportLog(
    val entry: String = "",
    val time: Long = 0
)

data class CheckinResult(
    val success: Boolean = false,
    val msg: String = ""
)

data class SummaryItem(
    val id: Long = 0,
    val content: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val companyEval: String = "3",
    val isDraft: Boolean = true,
    val createTime: Long = 0
)

data class Announcement(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = 0
)
