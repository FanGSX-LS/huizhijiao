package com.gxjzy.huizhijiao.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Contacts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.data.AppDataCache
import com.gxjzy.huizhijiao.model.ManageItem
import com.gxjzy.huizhijiao.model.MonthItem
import com.gxjzy.huizhijiao.model.SummaryItem
import com.gxjzy.huizhijiao.model.WeekItem
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import com.gxjzy.huizhijiao.ui.components.AppAlertDialog
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppCircularProgressIndicator
import com.gxjzy.huizhijiao.ui.components.AppSmallFAB
import com.gxjzy.huizhijiao.ui.components.AppSurface
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.GlassCard
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*

private val SDF_DATE_TIME = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

object ManageDataCache {
    var weekItems: List<ManageItem>? = null
    var monthItems: List<ManageItem>? = null
    var summaryItem: SummaryItem? = null
    var summaryLoaded: Boolean = false
    private val _contentCache = java.util.LinkedHashMap<Long, String>(16, 0.75f, true)
    val contentCache: MutableMap<Long, String> get() = _contentCache

    fun putContent(id: Long, content: String) {
        if (_contentCache.size > 50) {
            val first = _contentCache.keys.first()
            _contentCache.remove(first)
        }
        _contentCache[id] = content
    }

    fun clear() {
        weekItems = null
        monthItems = null
        summaryItem = null
        summaryLoaded = false
        _contentCache.clear()
    }
}

@Composable
fun ManageTab(
    onNavigateToWeekManage: () -> Unit,
    onNavigateToMonthManage: () -> Unit,
    onNavigateToReportDetail: (type: String, id: String, title: String) -> Unit,
    onNavigateToWeeklyReport: () -> Unit = {},
    onNavigateToMonthlySummary: () -> Unit = {},
    onNavigateToInternshipSummary: () -> Unit = {}
) {
    var currentSubTab by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "管理",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appOnSurface(),
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
            )

            TabRowWithContour(
                tabs = listOf("周报管理", "月报管理", "实习总结"),
                selectedTabIndex = currentSubTab,
                onTabSelected = { currentSubTab = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when (currentSubTab) {
                0 -> WeekManageContent(onNavigateToReportDetail = onNavigateToReportDetail)
                1 -> MonthManageContent(onNavigateToReportDetail = onNavigateToReportDetail)
                2 -> SummaryManageContent(onNavigateToReportDetail = onNavigateToReportDetail, onNavigateToInternshipSummary = onNavigateToInternshipSummary)
            }
        }

        AppSmallFAB(
            onClick = {
                when (currentSubTab) {
                    0 -> onNavigateToWeeklyReport()
                    1 -> onNavigateToMonthlySummary()
                    2 -> onNavigateToInternshipSummary()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 120.dp),
            containerColor = appPrimary(),
            contentColor = appOnPrimary()
        ) {
            Icon(MiuixIcons.Add, contentDescription = "发布")
        }
    }
}

@Composable
private fun WeekManageContent(onNavigateToReportDetail: (String, String, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(ManageDataCache.weekItems ?: emptyList()) }
    var loading by remember { mutableStateOf(ManageDataCache.weekItems == null) }
    var statusText by remember { mutableStateOf(if (ManageDataCache.weekItems != null) "共 ${ManageDataCache.weekItems!!.size} 条周记" else "正在加载周记列表...") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf("") }

    suspend fun fetchList(forceRefresh: Boolean = false) {
        if (!forceRefresh && ManageDataCache.weekItems != null) {
            items = ManageDataCache.weekItems!!
            statusText = "共 ${items.size} 条周记"
            return
        }
        loading = true
        try {
            var weekItems: List<WeekItem> = AppDataCache.weekItems ?: emptyList()
            var rawItems: List<ManageItem> = AppDataCache.weekManageItems ?: emptyList()
            if (forceRefresh || weekItems.isEmpty() || rawItems.isEmpty()) {
                coroutineScope {
                    val weekDeferred = async { ApiClient.fetchWeeks() }
                    val rawDeferred = async { ApiClient.fetchWeekList() }
                    weekItems = weekDeferred.await()
                    rawItems = rawDeferred.await()
                    AppDataCache.weekItems = weekItems
                    AppDataCache.weekManageItems = rawItems
                }
            }
            val weekMap = weekItems.associateBy { it.week }
            items = rawItems.map { item ->
                val w = weekMap[item.week.toIntOrNull() ?: -1]
                val sd = item.startDate.ifEmpty { w?.startDate ?: "" }
                val ed = item.endDate.ifEmpty { w?.endDate ?: "" }
                val dateRange = if (sd.isNotEmpty() && ed.isNotEmpty()) "$sd-$ed" else ""
                val title = if (item.week.isNotEmpty() && dateRange.isNotEmpty()) "第${item.week}周 $dateRange" else "第${item.week}周"
                val subtitle = if (sd.isNotEmpty()) "$sd ~ $ed${if (item.isDraft) " · 草稿" else " · 已提交"}" else if (item.isDraft) "草稿" else "已提交"
                item.copy(title = title, subtitle = subtitle, startDate = sd, endDate = ed)
            }
            ManageDataCache.weekItems = items
            statusText = if (items.isNotEmpty()) "共 ${items.size} 条周记" else "暂无周记"
            scope.launch {
                val needsPrefetch = items.filter { it.content.isEmpty() && !ManageDataCache.contentCache.containsKey(it.id) }
                for (item in needsPrefetch) {
                    try {
                        val detail = ApiClient.fetchWeeklyDetail(item.id.toString())
                        ManageDataCache.putContent(item.id, detail?.content ?: "")
                    } catch (_: Exception) {
                        ManageDataCache.putContent(item.id, "")
                    }
                }
                if (needsPrefetch.isNotEmpty()) {
                    items = items.map { item ->
                        val cached = ManageDataCache.contentCache[item.id]
                        if (item.content.isEmpty() && cached != null && cached.isNotEmpty()) {
                            item.copy(content = cached)
                        } else item
                    }
                    ManageDataCache.weekItems = items
                }
            }
        } catch (_: Exception) { statusText = "获取周记列表失败" }
        loading = false
    }

    LaunchedEffect(Unit) { fetchList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) AppCircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(statusText, fontSize = 15.sp, color = appOnSurfaceVariant())
        }

        if (!loading && items.isEmpty()) {
            Text("暂无已发布的周记", fontSize = 15.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
        }

        if (!loading) {
            items.forEach { item ->
                val cachedContent = ManageDataCache.contentCache[item.id]
                ReportCard(
                    title = item.title.ifEmpty { "周报" },
                    content = item.content.ifEmpty { cachedContent ?: "" },
                    publishTime = item.createTime,
                    onClick = { onNavigateToReportDetail("weekly", item.id.toString(), item.title.ifEmpty { "周报" }) },
                    onDelete = {
                        pendingDeleteId = item.id.toString()
                        showDeleteDialog = true
                }
            )
        }
        }
    }

    AppAlertDialog(
        show = showDeleteDialog,
        onDismissRequest = { showDeleteDialog = false; pendingDeleteId = "" },
        title = "确认删除",
        text = { Text("删除后数据将无法恢复，确认删除吗？") },
        confirmButton = {
            AppButton(
                onClick = {
                    scope.launch {
                        val success = ApiClient.deleteWeekly(pendingDeleteId)
                        statusText = if (success) "删除成功" else "删除失败"
                        showDeleteDialog = false
                        pendingDeleteId = ""
                        fetchList(forceRefresh = true)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = appError(),
                contentColor = appOnPrimary()
            ) { Text("删除") }
        },
        dismissButton = { AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = false; pendingDeleteId = "" }) }
    )
}

@Composable
private fun MonthManageContent(onNavigateToReportDetail: (String, String, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(ManageDataCache.monthItems ?: emptyList()) }
    var loading by remember { mutableStateOf(ManageDataCache.monthItems == null) }
    var statusText by remember { mutableStateOf(if (ManageDataCache.monthItems != null) "共 ${ManageDataCache.monthItems!!.size} 条月记" else "正在加载月记列表...") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf("") }

    suspend fun fetchList(forceRefresh: Boolean = false) {
        if (!forceRefresh && ManageDataCache.monthItems != null) {
            items = ManageDataCache.monthItems!!
            statusText = "共 ${items.size} 条月记"
            return
        }
        loading = true
        try {
            var monthItems: List<MonthItem> = AppDataCache.monthItems ?: emptyList()
            var rawItems: List<ManageItem> = AppDataCache.monthManageItems ?: emptyList()
            if (forceRefresh || monthItems.isEmpty() || rawItems.isEmpty()) {
                coroutineScope {
                    val monthDeferred = async { ApiClient.fetchMonths() }
                    val rawDeferred = async { ApiClient.fetchMonthList() }
                    monthItems = monthDeferred.await()
                    rawItems = rawDeferred.await()
                    AppDataCache.monthItems = monthItems
                    AppDataCache.monthManageItems = rawItems
                }
            }
            val monthMap = monthItems.associateBy { it.month }
            items = rawItems.map { item ->
                val m = monthMap[item.month.toIntOrNull() ?: -1]
                val sd = item.startDate.ifEmpty { m?.startDate ?: "" }
                val ed = item.endDate.ifEmpty { m?.endDate ?: "" }
                val dateRange = if (sd.isNotEmpty() && ed.isNotEmpty()) "$sd-$ed" else ""
                val title = if (item.month.isNotEmpty() && dateRange.isNotEmpty()) "第${item.month}月 $dateRange" else "第${item.month}月"
                val subtitle = if (sd.isNotEmpty()) "$sd ~ $ed${if (item.isDraft) " · 草稿" else " · 已提交"}" else if (item.isDraft) "草稿" else "已提交"
                item.copy(title = title, subtitle = subtitle, startDate = sd, endDate = ed)
            }
            ManageDataCache.monthItems = items
            statusText = if (items.isNotEmpty()) "共 ${items.size} 条月记" else "暂无月记"
            scope.launch {
                val needsPrefetch = items.filter { it.content.isEmpty() && !ManageDataCache.contentCache.containsKey(it.id) }
                for (item in needsPrefetch) {
                    try {
                        val detail = ApiClient.fetchMonthlyDetail(item.id.toString())
                        ManageDataCache.putContent(item.id, detail?.content ?: "")
                    } catch (_: Exception) {
                        ManageDataCache.putContent(item.id, "")
                    }
                }
                if (needsPrefetch.isNotEmpty()) {
                    items = items.map { item ->
                        val cached = ManageDataCache.contentCache[item.id]
                        if (item.content.isEmpty() && cached != null && cached.isNotEmpty()) {
                            item.copy(content = cached)
                        } else item
                    }
                    ManageDataCache.monthItems = items
                }
            }
        } catch (_: Exception) { statusText = "获取月记列表失败" }
        loading = false
    }

    LaunchedEffect(Unit) { fetchList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) AppCircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(statusText, fontSize = 15.sp, color = appOnSurfaceVariant())
        }

        if (!loading && items.isEmpty()) {
            Text("暂无已发布的月记", fontSize = 15.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
        }

        if (!loading) {
        items.forEach { item ->
            ReportCard(
                title = item.title.ifEmpty { "月报" },
                content = item.content,
                publishTime = item.createTime,
                onClick = { onNavigateToReportDetail("monthly", item.id.toString(), item.title.ifEmpty { "月报" }) },
                onDelete = {
                    pendingDeleteId = item.id.toString()
                    showDeleteDialog = true
                }
            )
        }
        }
    }

    AppAlertDialog(
        show = showDeleteDialog,
        onDismissRequest = { showDeleteDialog = false; pendingDeleteId = "" },
        title = "确认删除",
        text = { Text("删除后数据将无法恢复，确认删除吗？") },
        confirmButton = {
            AppButton(
                onClick = {
                    scope.launch {
                        val success = ApiClient.deleteMonthly(pendingDeleteId)
                        statusText = if (success) "删除成功" else "删除失败"
                        showDeleteDialog = false
                        pendingDeleteId = ""
                        fetchList(forceRefresh = true)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = appError(),
                contentColor = appOnPrimary()
            ) { Text("删除") }
        },
        dismissButton = { AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = false; pendingDeleteId = "" }) }
    )
}

@Composable
private fun ReportCard(
    title: String,
    content: String,
    publishTime: Long = 0,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val user = ApiClient.getCachedUser()
    val rawAvatarUrl = user?.avatarUrl ?: ""
    val avatarUrl = if (rawAvatarUrl.startsWith("http")) rawAvatarUrl
                    else if (rawAvatarUrl.isNotEmpty()) "${ApiClient.getBaseUrl().trimEnd('/')}/${rawAvatarUrl.trimStart('/')}"
                    else ""
    val userName = user?.name ?: ""
    val companyName = user?.internship?.companyName ?: ""

    val timeText = if (publishTime > 0) {
        try {
            val sdf = SDF_DATE_TIME
            sdf.format(Date(publishTime))
        } catch (_: Exception) { "" }
    } else ""

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cardPadding = 0
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(appSurfaceVariant()),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(appPrimaryContainer()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(MiuixIcons.Contacts, contentDescription = null, modifier = Modifier.size(14.dp), tint = appOnPrimaryContainer())
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(userName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = appOnSurface(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (companyName.isNotEmpty()) {
                        Text(companyName, fontSize = 10.sp, color = appOnSurfaceVariant(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OverlayIconDropdownMenu(
                    entry = DropdownEntry(
                        items = listOf(
                            DropdownItem(text = "删除", onClick = { onDelete() })
                        )
                    ),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(MiuixIcons.More, contentDescription = "更多", tint = appOnSurfaceVariant(), modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = appOnSurfaceVariant().copy(alpha = 0.12f), thickness = 0.5.dp)

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appOnSurface(), maxLines = 2, overflow = TextOverflow.Ellipsis)

                if (content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(content.take(60) + if (content.length > 60) "..." else "", fontSize = 13.sp, color = appOnSurfaceVariant(), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                }

                if (timeText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(timeText, fontSize = 11.sp, color = appOutlineVariant(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun SummaryManageContent(onNavigateToReportDetail: (String, String, String) -> Unit, onNavigateToInternshipSummary: () -> Unit) {
    var summary by remember { mutableStateOf(ManageDataCache.summaryItem) }
    var loading by remember { mutableStateOf(!ManageDataCache.summaryLoaded) }
    var statusText by remember { mutableStateOf(if (ManageDataCache.summaryLoaded) (if (ManageDataCache.summaryItem != null) "已有总结" else "暂无总结") else "正在加载实习总结...") }

    suspend fun fetchSummary(forceRefresh: Boolean = false) {
        if (!forceRefresh && ManageDataCache.summaryLoaded) {
            summary = ManageDataCache.summaryItem
            statusText = if (summary != null) "已有总结" else "暂无总结"
            return
        }
        loading = true
        try {
            if (!forceRefresh && AppDataCache.summaryItem != null) {
                summary = AppDataCache.summaryItem
                ManageDataCache.summaryItem = summary
                ManageDataCache.summaryLoaded = true
                statusText = "已有总结"
            } else {
                val user = ApiClient.getCachedUser()
                val internshipId = user?.internship?.internshipId?.toString() ?: ""
                if (internshipId.isNotEmpty()) {
                    val s = ApiClient.fetchSummary(internshipId)
                    summary = s
                    ManageDataCache.summaryItem = s
                    ManageDataCache.summaryLoaded = true
                    AppDataCache.summaryItem = s
                    statusText = if (s != null) "已有总结" else "暂无总结"
                } else {
                    statusText = "无实习信息"
                }
            }
        } catch (_: Exception) { statusText = "获取总结失败" }
        loading = false
    }

    LaunchedEffect(Unit) { fetchSummary() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) AppCircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(statusText, fontSize = 15.sp, color = appOnSurfaceVariant())
        }

        if (!loading && summary == null) {
            Text("暂未提交实习总结，点击下方按钮提交", fontSize = 15.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
        }

        summary?.let { s ->
             val user = ApiClient.getCachedUser()
             val rawAvatarUrl = user?.avatarUrl ?: ""
             val avatarUrl = if (rawAvatarUrl.startsWith("http")) rawAvatarUrl
                             else if (rawAvatarUrl.isNotEmpty()) "${ApiClient.getBaseUrl().trimEnd('/')}/${rawAvatarUrl.trimStart('/')}"
                             else ""
            val userName = user?.name ?: ""
            val companyName = user?.internship?.companyName ?: ""
            val period = if (s.startDate.isNotEmpty() || s.endDate.isNotEmpty()) "${s.startDate} ~ ${s.endDate}" else ""
            val timeText = if (s.createTime > 0) {
                try {
                    val sdf = SDF_DATE_TIME
                    sdf.format(Date(s.createTime))
                } catch (_: Exception) { "" }
            } else ""

            val statusColor = if (s.isDraft) Color(0xFFFF9800) else Color(0xFF4CAF50)
            val tagColor = Color(0xFFAB47BC)

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReportDetail("summary", "0", "实习总结") },
                cardPadding = 0
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppSurface(
                            color = tagColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "实习总结",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = tagColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(appSurfaceVariant()),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(appPrimaryContainer()),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(MiuixIcons.Contacts, contentDescription = null, modifier = Modifier.size(14.dp), tint = appOnPrimaryContainer())
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = appOnSurface(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (companyName.isNotEmpty()) {
                                Text(companyName, fontSize = 10.sp, color = appOnSurfaceVariant(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        AppSurface(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (s.isDraft) "草稿" else "已提交",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = appOnSurfaceVariant().copy(alpha = 0.12f), thickness = 0.5.dp)

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("实习总结", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appOnSurface(), maxLines = 2, overflow = TextOverflow.Ellipsis)

                        if (period.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(period, fontSize = 13.sp, color = appOnSurfaceVariant(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (s.content.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(s.content.take(80) + if (s.content.length > 80) "..." else "", fontSize = 13.sp, color = appOnSurfaceVariant(), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                        }

                        if (timeText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(timeText, fontSize = 11.sp, color = appOutlineVariant(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}
