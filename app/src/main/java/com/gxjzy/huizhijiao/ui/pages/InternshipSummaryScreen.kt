package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.SummaryItem
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton

@Composable
fun InternshipSummaryScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<SummaryItem?>(null) }
    var content by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var companyEval by remember { mutableIntStateOf(3) }
    var isDraft by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    suspend fun loadSummary() {
        loading = true
        try {
            val user = ApiClient.getCachedUser()
            val internshipId = user?.internship?.internshipId?.toString() ?: ""
            if (internshipId.isNotEmpty()) {
                val s = ApiClient.fetchSummary(internshipId)
                summary = s
                if (s != null) {
                    content = s.content
                    startDate = s.startDate.ifEmpty { user?.internship?.startDate ?: "" }
                    endDate = s.endDate.ifEmpty { user?.internship?.endDate ?: "" }
                    companyEval = (s.companyEval.toIntOrNull() ?: 3).coerceAtMost(3)
                    isDraft = s.isDraft
                } else {
                    startDate = user?.internship?.startDate ?: ""
                    endDate = user?.internship?.endDate ?: ""
                }
            }
        } catch (_: Exception) { statusMsg = "获取总结失败" }
        loading = false
    }

    LaunchedEffect(Unit) { loadSummary() }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "实习总结填写",
                navigationIcon = { AppIconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().background(appBackground()).padding(innerPadding).verticalScroll(rememberScrollState()).appOverScrollVertical().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("加载中...", fontSize = 15.sp, color = appOnSurfaceVariant())
                    }
                }

                summary?.let { s ->
                    if (!s.isDraft) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("已正式提交，可修改后重新提交", fontSize = 13.sp, color = Color(0xFF2E7D32), modifier = Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center)
                        }
                    }
                }

                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppTextField(
                            value = content, onValueChange = { content = it },
                            labelText = "请输入实习总结...",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 10
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("开始日期", fontSize = 13.sp, color = appOnSurfaceVariant())
                                Spacer(modifier = Modifier.height(4.dp))
                                AppTextField(
                                    value = startDate, onValueChange = {},
                                    labelText = "开始日期",
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    singleLine = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("结束日期", fontSize = 13.sp, color = appOnSurfaceVariant())
                                Spacer(modifier = Modifier.height(4.dp))
                                AppTextField(
                                    value = endDate, onValueChange = {},
                                    labelText = "结束日期",
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    singleLine = true
                                )
                            }
                        }

                        Column {
                            Text("企业评价", fontSize = 14.sp, color = appOnSurfaceVariant())
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                (1..3).forEach { i ->
                                    IconButton(
                                        onClick = { companyEval = i },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            if (i <= companyEval) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "$i",
                                            tint = if (i <= companyEval) Color(0xFFFFC107) else appOnSurfaceVariant(),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                when (companyEval) { 1 -> "不满意"; 2 -> "一般"; 3 -> "满意"; else -> "" },
                                fontSize = 12.sp, color = appOnSurfaceVariant()
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("草稿", fontSize = 15.sp, color = appOnSurfaceVariant())
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = isDraft, onCheckedChange = { isDraft = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFF9800)))
                        }
                    }
                }

                AppButton(
                    onClick = {
                        scope.launch {
                            if (content.isEmpty()) { statusMsg = "请输入总结内容"; return@launch }
                            submitting = true; statusMsg = ""
                            try {
                                val success = ApiClient.saveSummary(content, startDate, endDate, "$companyEval", if (isDraft) "true" else "false")
                                statusMsg = if (success) "提交成功" else "提交失败"
                                if (success) loadSummary()
                            } catch (_: Exception) { statusMsg = "网络错误" }
                            submitting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submitting
                ) { Text("提交总结", fontSize = 16.sp) }

                if (statusMsg.isNotEmpty()) {
                    Text(statusMsg, fontSize = 15.sp, color = if (statusMsg == "提交成功") Color(0xFF2E7D32) else appError(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
    }
}
