package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.ManageItem
import com.gxjzy.huizhijiao.model.MonthlyDetail
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MonthManageScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ManageItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var statusText by remember { mutableStateOf("正在加载月记列表...") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf("") }
    var detailData by remember { mutableStateOf<MonthlyDetail?>(null) }
    var detailTitle by remember { mutableStateOf("") }
    var showDetailDialog by remember { mutableStateOf(false) }

    suspend fun fetchList() {
        loading = true
        try {
            items = ApiClient.fetchMonthList()
            statusText = if (items.isNotEmpty()) "共 ${items.size} 条月记" else "暂无月记"
        } catch (_: Exception) { statusText = "获取月记列表失败" }
        loading = false
    }

    LaunchedEffect(Unit) { fetchList() }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "月记管理",
                navigationIcon = { AppIconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).appOverScrollVertical().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(statusText, fontSize = 15.sp, color = appOnSurfaceVariant())
                }
                if (!loading && items.isEmpty()) {
                    Text("暂无已发布的月记", fontSize = 15.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
                }
                items.forEach { item ->
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title.ifEmpty { "月报" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = appPrimary())
                                if (item.subtitle.isNotEmpty()) Text(item.subtitle, fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 6.dp))
                            }
                            Text("查看", fontSize = 13.sp, color = appPrimary(),
                                modifier = Modifier.border(1.dp, appPrimary(), RoundedCornerShape(16.dp)).clickable {
                                    scope.launch {
                                        val detail = ApiClient.fetchMonthlyDetail(item.id.toString())
                                        if (detail != null) { detailData = detail; detailTitle = item.title.ifEmpty { "月报" }; showDetailDialog = true }
                                        else statusText = "获取详情失败"
                                    }
                                }.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("删除", fontSize = 13.sp, color = appError(),
                                modifier = Modifier.border(1.dp, appError(), RoundedCornerShape(16.dp)).clickable { pendingDeleteId = item.id.toString(); showDeleteDialog = true }.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
    }

    OverlayDialog(
        show = showDeleteDialog,
        onDismissRequest = { showDeleteDialog = false; pendingDeleteId = "" },
        title = "确认删除",
    ) {
        Text("删除后数据将无法恢复，确认删除吗？")
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = false; pendingDeleteId = "" })
            AppButton(onClick = {
                scope.launch {
                    val success = ApiClient.deleteMonthly(pendingDeleteId)
                    statusText = if (success) "删除成功" else "删除失败"
                    showDeleteDialog = false; pendingDeleteId = ""
                    fetchList()
                }
            }, modifier = Modifier.fillMaxWidth(), containerColor = appError()) { Text("删除") }
        }
    }

    OverlayDialog(
        show = showDetailDialog && detailData != null,
        onDismissRequest = { showDetailDialog = false },
        title = detailTitle,
    ) {
        Column {
            DetailRow3("实习课程", detailData!!.internshipName)
            DetailRow3("实习单位", detailData!!.companyName)
            Text("月记内容", fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            Text(detailData!!.content.ifEmpty { "无内容" }, fontSize = 15.sp, color = appOnSurface(),
                modifier = Modifier.fillMaxWidth().background(appBackground(), RoundedCornerShape(14.dp)).padding(14.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "关闭", modifier = Modifier.fillMaxWidth(), onClick = { showDetailDialog = false })
        }
    }
}

@Composable
private fun DetailRow3(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.width(72.dp))
        Text(value.ifEmpty { "-" }, fontSize = 14.sp, color = appOnSurface(), modifier = Modifier.weight(1f))
    }
}
