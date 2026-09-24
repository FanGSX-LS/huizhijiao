package com.gxjzy.huizhijiao.ui.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.data.AppDataCache
import com.gxjzy.huizhijiao.model.CheckinRecord
import com.gxjzy.huizhijiao.ui.components.AppAlertDialog
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppLinearProgressIndicator
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.components.AmapView
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.map.CoordTransform
import com.gxjzy.huizhijiao.ui.theme.*
import com.gxjzy.huizhijiao.R
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.util.*

@Composable
fun HomeTab() {
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf("") }
    var signedDays by remember { mutableIntStateOf(0) }
    var leastSignIn by remember { mutableIntStateOf(1) }
    var internshipName by remember { mutableStateOf("") }
    var internType by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var credit by remember { mutableStateOf("") }
    var classHour by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var weekFilled by remember { mutableStateOf(false) }
    var monthFilled by remember { mutableStateOf(false) }
    val now = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var checkedDays by remember { mutableStateOf(setOf<Int>()) }
    var allRecords by remember { mutableStateOf<List<CheckinRecord>>(emptyList()) }
    var showDayDetail by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(-1) }

    val progressPct by remember(signedDays, leastSignIn) {
        derivedStateOf { if (leastSignIn > 0) (signedDays * 100 / leastSignIn).coerceAtMost(100) else 0 }
    }
    val progressFraction by remember(signedDays, leastSignIn) {
        derivedStateOf { if (leastSignIn > 0) signedDays.toFloat() / leastSignIn else 0f }
    }

    suspend fun loadMonthRecords(year: Int, month: Int) {
        if (year == AppDataCache.calendarYear && month == AppDataCache.calendarMonth && AppDataCache.calendarEntries != null && AppDataCache.checkinRecords != null) {
            checkedDays = AppDataCache.calendarEntries!!.mapNotNull { entry ->
                val parts = entry.date.split("-")
                if (parts.size >= 3) parts[2].toIntOrNull() else null
            }.toSet()
            allRecords = AppDataCache.checkinRecords!!
            return
        }
        try {
            val entries = ApiClient.fetchCheckinCalendar(year, month)
            checkedDays = entries.mapNotNull { entry ->
                val parts = entry.date.split("-")
                if (parts.size >= 3) parts[2].toIntOrNull() else null
            }.toSet()
        } catch (_: Exception) {
        }
        try {
            allRecords = ApiClient.fetchCheckinRecords(
                String.format("%d-%02d-01", year, month),
                String.format("%d-%02d-%02d", year, month, getDaysInMonth(year, month))
            )
        } catch (_: Exception) {
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        val user = AppDataCache.userProfile
        if (user != null) {
            userName = user.name
            user.internship?.let { intern ->
                signedDays = intern.signedDays
                leastSignIn = intern.leastSignIn.coerceAtLeast(1)
                internshipName = intern.internshipName
                internType = intern.internType
                teacherName = intern.teacherName
                companyName = intern.companyName
                credit = intern.credit.toString()
                classHour = intern.classHour.toString()
                startDate = intern.startDate
                endDate = intern.endDate
            }
        }
        AppDataCache.checkinProgress?.let { progress ->
            signedDays = progress.signedDays
            leastSignIn = progress.leastSignIn.coerceAtLeast(1)
        }
        AppDataCache.writeRecords?.let { records ->
            weekFilled = records.weekResult
            monthFilled = records.monthResult
        }
        if (AppDataCache.calendarYear > 0 && AppDataCache.calendarMonth > 0) {
            calYear = AppDataCache.calendarYear
            calMonth = AppDataCache.calendarMonth
        }
        loadMonthRecords(calYear, calMonth)
    }

    suspend fun refreshData() {
        isRefreshing = true
        try {
            val user = ApiClient.fetchMe()
            if (user != null) {
                userName = user.name
                user.internship?.let { intern ->
                    signedDays = intern.signedDays
                    leastSignIn = intern.leastSignIn.coerceAtLeast(1)
                    internshipName = intern.internshipName
                    internType = intern.internType
                    teacherName = intern.teacherName
                    companyName = intern.companyName
                    credit = intern.credit.toString()
                    classHour = intern.classHour.toString()
                    startDate = intern.startDate
                    endDate = intern.endDate
                }
            }
        } catch (_: Exception) {}
        try {
            val progress = ApiClient.fetchCheckinProgress()
            if (progress != null) {
                signedDays = progress.signedDays
                leastSignIn = progress.leastSignIn.coerceAtLeast(1)
            }
        } catch (_: Exception) {}
        scope.launch { loadMonthRecords(calYear, calMonth) }
        try {
            val records = ApiClient.fetchWriteRecords()
            weekFilled = records.weekResult
            monthFilled = records.monthResult
        } catch (_: Exception) {}
        isRefreshing = false
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { refreshData() } },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        pullToRefreshState = pullToRefreshState,
        contentPadding = PaddingValues(bottom = 104.dp),
    ) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).appOverScrollVertical().padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "首页",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appOnSurface(),
            modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 12.dp, bottom = 0.dp)
        )

        GradientHeader(userName)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("已签到", signedDays.toString(), appPrimaryContainer(), Color(0xFF1565C0), Modifier.weight(1f))
            StatCard("要求天数", leastSignIn.toString(), appSecondaryContainer(), Color(0xFF7B1FA2), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("周报状态", if (weekFilled) "已填写" else "待填写", if (weekFilled) CardGreen else CardOrange, if (weekFilled) Color(0xFF2E7D32) else WarningOrange, Modifier.weight(1f))
            StatCard("月报状态", if (monthFilled) "已填写" else "待填写", if (monthFilled) CardCyan else CardRed, if (monthFilled) Color(0xFF00695C) else appError(), Modifier.weight(1f))
        }

        CalendarCard(calYear, calMonth, checkedDays, onMonthChange = { y, m -> calYear = y; calMonth = m; scope.launch { loadMonthRecords(y, m) } }, onDayClick = { day ->
            selectedDay = day; showDayDetail = true
        })

        GlassCard {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("签到进度", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = appOnSurface())
                    Text("$signedDays / $leastSignIn 天", fontSize = 15.sp, color = appPrimary(), fontWeight = FontWeight.Bold)
                    Text("$progressPct%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (progressPct >= 100) Color(0xFF2E7D32) else appPrimary())
                }
                Spacer(modifier = Modifier.height(12.dp))
                AppLinearProgressIndicator(
                    progress = progressFraction,
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                    color = if (signedDays >= leastSignIn) Color(0xFF2E7D32) else appPrimary(), trackColor = Color(0xFFE0E0E0)
                )
            }
        }

        GlassCard {
            Column {
                Text("实习信息", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = appOnSurface())
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow("实习名称", internshipName.ifEmpty { "-" })
                InfoRow("实习类型", internType.ifEmpty { "-" })
                InfoRow("指导老师", teacherName.ifEmpty { "-" })
                InfoRow("实习企业", companyName.ifEmpty { "-" })
                InfoRow("学分/学时", if (credit.isNotEmpty()) "$credit / $classHour" else "-")
                InfoRow("起止时间", if (startDate.isNotEmpty()) "$startDate ~ $endDate" else "-")
            }
        }
    }
    }

    val dayRecords = remember(allRecords, selectedDay) {
        allRecords.filter { record ->
            val ts = if (record.sendTime > 0) record.sendTime else record.createTime
            if (ts > 0) Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.DAY_OF_MONTH) == selectedDay else false
        }
    }
    DayDetailSheet(showDayDetail, calMonth, selectedDay, dayRecords, onDismiss = { showDayDetail = false })
}

@Composable
private fun GradientHeader(userName: String) {
    val shape = RoundedCornerShape(24.dp)
    val colorsList = listOf(appPrimary(), appPrimary().copy(alpha = 0.7f))
    val now = remember { Calendar.getInstance() }
    val weekDays = remember { listOf("日", "一", "二", "三", "四", "五", "六") }
    val dateText = remember {
        "${now.get(Calendar.YEAR)}年${now.get(Calendar.MONTH) + 1}月${now.get(Calendar.DAY_OF_MONTH)}日 星期${weekDays[now.get(Calendar.DAY_OF_WEEK) - 1]}"
    }
    Box(modifier = Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(colorsList), shape).padding(24.dp)) {
        Column {
            Text("欢迎回来，${userName.ifEmpty { "同学" }}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = appOnPrimary())
            Text(dateText, fontSize = 14.sp, color = appOnPrimary().copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    val resolvedBg = appSurfaceContainerHigh()
    val labelColor = appOnSurfaceVariant()
    val valueColor = appOnSurface()
    Row(modifier = modifier.clip(shape).background(resolvedBg, shape).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = labelColor)
            Text(value.ifEmpty { "-" }, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun CalendarCard(calYear: Int, calMonth: Int, checkedDays: Set<Int>, onMonthChange: (Int, Int) -> Unit, onDayClick: (Int) -> Unit) {
    var slideDirection by remember { mutableIntStateOf(1) }

    GlassCard {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "上一月",
                    tint = appPrimary(),
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        slideDirection = -1
                        var m = calMonth - 1; var y = calYear; if (m < 1) { m = 12; y-- }; onMonthChange(y, m)
                    }.padding(6.dp)
                )
                AnimatedContent(
                    targetState = "${calYear}年${calMonth}月",
                    transitionSpec = {
                        if (slideDirection >= 0) {
                            slideInHorizontally(tween(300)) { it } togetherWith slideOutHorizontally(tween(300)) { -it }
                        } else {
                            slideInHorizontally(tween(300)) { -it } togetherWith slideOutHorizontally(tween(300)) { it }
                        }.using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.weight(1f),
                    label = "monthTitle"
                ) { title ->
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = appOnSurface(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "下一月",
                    tint = appPrimary(),
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable {
                        slideDirection = 1
                        var m = calMonth + 1; var y = calYear; if (m > 12) { m = 1; y++ }; onMonthChange(y, m)
                    }.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { name -> Text(name, fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            Spacer(modifier = Modifier.height(6.dp))

            val year = calYear
            val month = calMonth
            val key = year * 100 + month
            val firstDow = remember(key) { getFirstDayOfWeek(year, month) }
            val daysInMonth = remember(key) { getDaysInMonth(year, month) }
            val today = remember { Calendar.getInstance() }
            val isCurrentMonth = year == today.get(Calendar.YEAR) && month == today.get(Calendar.MONTH) + 1
            val todayDay = today.get(Calendar.DAY_OF_MONTH)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                var day = 1
                for (week in 0..5) {
                    if (day > daysInMonth) break
                    Row(modifier = Modifier.fillMaxHeight(1f / 6f)) {
                        for (col in 0..6) {
                            if (week == 0 && col < firstDow) { Box(modifier = Modifier.weight(1f).height(36.dp)) }
                            else if (day <= daysInMonth) {
                                val d = day; val checked = checkedDays.contains(d); val isToday = isCurrentMonth && d == todayDay
                                Box(modifier = Modifier.weight(1f).height(36.dp).clickable(indication = null, interactionSource = null) { onDayClick(d) }, contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(if (checked) appPrimary() else Color.Transparent).then(if (isToday) Modifier.border(2.dp, appPrimary(), RoundedCornerShape(16.dp)) else Modifier), contentAlignment = Alignment.Center) {
                                        Text("$d", fontSize = 14.sp, color = if (checked) appOnPrimary() else appOnSurface())
                                    }
                                }
                                day++
                            } else { Box(modifier = Modifier.weight(1f).height(36.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailSheet(show: Boolean, month: Int, day: Int, records: List<CheckinRecord>, onDismiss: () -> Unit) {
    val validRecords = records.filter { 
        it.locationX.toDoubleOrNull()?.let { x -> x != 0.0 } == true && 
        it.locationY.toDoubleOrNull()?.let { y -> y != 0.0 } == true 
    }
    val firstRecord = validRecords.firstOrNull()
    val gcjCoord = firstRecord?.let {
        val bdLng = it.locationX.toDoubleOrNull() ?: 0.0
        val bdLat = it.locationY.toDoubleOrNull() ?: 0.0
        CoordTransform.bd09ToGcj02(bdLng, bdLat)
    }

    OverlayBottomSheet(
        show = show,
        onDismissRequest = onDismiss,
        title = "${month}月${day}日 签到记录",
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (gcjCoord != null) {
                AmapView(
                    longitude = gcjCoord[0],
                    latitude = gcjCoord[1],
                    label = firstRecord.label,
                    mode = "show",
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                )
            }
            if (records.isEmpty()) {
                Text("暂无记录", fontSize = 15.sp, color = appOnSurfaceVariant())
            } else {
                records.forEach { record ->
                    Column(
                        modifier = Modifier.fillMaxWidth().background(appSurfaceVariant(), RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val typeLabel = when {
                                record.isAbnormal -> "异常"
                                record.isEvection -> "出差"
                                record.isReissue -> "补签"
                                else -> if (record.checkType.equals("CHECKIN", ignoreCase = true)) "签到" else "签退"
                            }
                            val typeColor = when {
                                record.isAbnormal -> appError()
                                record.isEvection -> WarningOrange
                                else -> appPrimary()
                            }
                            Text(typeLabel, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.background(typeColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                            Text(formatTime(record.createTime), fontSize = 14.sp, color = appOnSurface(), modifier = Modifier.padding(start = 12.dp))
                            if (!record.status) {
                                Text("异常", fontSize = 12.sp, color = appError(), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("地点", fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.width(36.dp))
                            Text(if (record.label.isNotEmpty()) record.label else "-", fontSize = 13.sp, color = appOnSurface().copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            AppTextButton(text = "关闭", modifier = Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.width(80.dp))
        Text(value, fontSize = 14.sp, color = appOnSurface(), modifier = Modifier.weight(1f))
    }
}

private fun getDaysInMonth(year: Int, month: Int): Int = Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val cal = Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1) }
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    return if (dow == Calendar.SUNDAY) 6 else dow - 2
}

private fun formatTime(createTime: Long): String {
    if (createTime <= 0) return "-"
    val cal = Calendar.getInstance().apply { timeInMillis = createTime }
    return String.format("%02d:%02d:%02d",
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
}
