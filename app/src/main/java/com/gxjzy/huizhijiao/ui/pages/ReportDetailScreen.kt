package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.foundation.background
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Contacts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.GlassCard
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
import com.gxjzy.huizhijiao.model.MonthlyDetail
import com.gxjzy.huizhijiao.model.SummaryItem
import com.gxjzy.huizhijiao.model.WeeklyDetail
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SDF_DATE_TIME = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
fun ReportDetailScreen(
    type: String,
    id: String,
    title: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    var weeklyDetail by remember { mutableStateOf<WeeklyDetail?>(null) }
    var monthlyDetail by remember { mutableStateOf<MonthlyDetail?>(null) }
    var summaryDetail by remember { mutableStateOf<SummaryItem?>(null) }

    val user = ApiClient.getCachedUser()
    val rawAvatarUrl = user?.avatarUrl ?: ""
    val avatarUrl = if (rawAvatarUrl.startsWith("http")) rawAvatarUrl
                    else if (rawAvatarUrl.isNotEmpty()) "${ApiClient.getBaseUrl().trimEnd('/')}/${rawAvatarUrl.trimStart('/')}"
                    else ""
    val userName = user?.name ?: ""
    val companyName = user?.internship?.companyName ?: ""

    suspend fun loadDetail() {
        loading = true
        try {
            when (type) {
                "weekly" -> {
                    val detail = ApiClient.fetchWeeklyDetail(id)
                    if (detail != null) weeklyDetail = detail
                    else statusText = "获取详情失败"
                }
                "monthly" -> {
                    val detail = ApiClient.fetchMonthlyDetail(id)
                    if (detail != null) monthlyDetail = detail
                    else statusText = "获取详情失败"
                }
                "summary" -> {
                    val internshipId = user?.internship?.internshipId?.toString() ?: ""
                    if (internshipId.isNotEmpty()) {
                        val s = ApiClient.fetchSummary(internshipId)
                        if (s != null) summaryDetail = s
                        else statusText = "获取详情失败"
                    } else statusText = "无实习信息"
                }
            }
        } catch (_: Exception) { statusText = "获取详情失败" }
        loading = false
    }

    LaunchedEffect(Unit) { loadDetail() }

    val secondaryText = when (type) {
        "weekly" -> if (companyName.isNotEmpty()) "周记 · $companyName" else "周记"
        "monthly" -> if (companyName.isNotEmpty()) "月记 · $companyName" else "月记"
        "summary" -> if (companyName.isNotEmpty()) "实习总结 · $companyName" else "实习总结"
        else -> ""
    }

    val contentText = when {
        weeklyDetail != null -> weeklyDetail!!.content
        monthlyDetail != null -> monthlyDetail!!.content
        summaryDetail != null -> summaryDetail!!.content
        else -> ""
    }

    val publishTime = when {
        weeklyDetail != null -> weeklyDetail!!.createTime
        monthlyDetail != null -> monthlyDetail!!.createTime
        summaryDetail != null -> summaryDetail!!.createTime
        else -> 0L
    }

     val timeText = if (publishTime > 0) {
         try {
             SDF_DATE_TIME.format(Date(publishTime))
        } catch (_: Exception) { "" }
    } else ""

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = title,
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    if (type != "summary") {
                        OverlayIconDropdownMenu(
                            entry = DropdownEntry(
                                items = listOf(
                                    DropdownItem(text = "删除", onClick = { showDeleteDialog = true })
                                )
                            )
                        ) {
                            Icon(MiuixIcons.More, contentDescription = "更多")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(appBackground()).padding(innerPadding)) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                cardPadding = 12
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(MiuixIcons.Contacts, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF757575))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(userName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = appOnSurface(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(secondaryText, fontSize = 12.sp, color = appOnSurfaceVariant(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (deleted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("已删除", fontSize = 16.sp, color = appOnSurfaceVariant())
                }
            } else if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("加载中...", fontSize = 15.sp, color = appOnSurfaceVariant())
                    }
                }
            } else if (statusText.isNotEmpty() && weeklyDetail == null && monthlyDetail == null && summaryDetail == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(statusText, fontSize = 15.sp, color = appOnSurfaceVariant())
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()).appOverScrollVertical()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cardPadding = 16
                    ) {
                        Column {
                            SelectionContainer {
                                Text(
                                    contentText.ifEmpty { "暂无内容" },
                                    fontSize = 16.sp,
                                    color = if (contentText.isNotEmpty()) appOnSurface() else appOnSurfaceVariant(),
                                    lineHeight = 26.sp
                                )
                            }

                            if (timeText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(timeText, fontSize = 12.sp, color = appOnSurfaceVariant(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    OverlayDialog(
        show = showDeleteDialog,
        onDismissRequest = { showDeleteDialog = false },
        title = "确认删除",
    ) {
        Text("删除后数据将无法恢复，确认删除吗？")
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = false })
            AppButton(onClick = {
                scope.launch {
                    val success = when (type) {
                        "weekly" -> ApiClient.deleteWeekly(id)
                        "monthly" -> ApiClient.deleteMonthly(id)
                        else -> false
                    }
                    if (success) {
                        deleted = true
                        showDeleteDialog = false
                    } else {
                        statusText = "删除失败"
                        showDeleteDialog = false
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), containerColor = appError()) { Text("删除") }
        }
    }
}
