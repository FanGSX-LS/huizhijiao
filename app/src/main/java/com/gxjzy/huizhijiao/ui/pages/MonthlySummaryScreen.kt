package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.MonthItem
import com.gxjzy.huizhijiao.ui.components.AppSwitch
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.theme.WarningOrange
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MonthlySummaryScreen(onBack: () -> Unit) {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()
    val advancedMode by prefs.advancedMode.distinctUntilChanged().collectAsState(initial = false)
    var allMonthData by remember { mutableStateOf<List<MonthItem>>(emptyList()) }
    var filteredMonthData by remember { mutableStateOf<List<MonthItem>>(emptyList()) }
    var selectedIdx by remember { mutableIntStateOf(-1) }
    var content by remember { mutableStateOf("") }
    var isDraft by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    var loadingMonths by remember { mutableStateOf(true) }

    fun isFuture(startDate: String): Boolean {
        if (startDate.isEmpty()) return false
        return try {
            val parts = startDate.split("-")
            if (parts.size < 3) return false
            val cal = java.util.Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0) }
            val today = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
            cal.timeInMillis > today.timeInMillis
        } catch (_: Exception) { false }
    }

    fun rebuildMonthData() {
        filteredMonthData = if (advancedMode) allMonthData else allMonthData.filter { !isFuture(it.startDate) }
        if (selectedIdx < 0 || selectedIdx >= filteredMonthData.size) selectedIdx = 0
    }

    LaunchedEffect(Unit) {
        try { allMonthData = ApiClient.fetchMonths(); rebuildMonthData() }
        catch (_: Exception) { statusMsg = "获取月份失败" }
        loadingMonths = false
    }
    LaunchedEffect(advancedMode) { rebuildMonthData() }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "月记填写",
                navigationIcon = { AppIconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().background(appBackground()).padding(innerPadding).verticalScroll(rememberScrollState()).appOverScrollVertical().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!loadingMonths && filteredMonthData.isNotEmpty()) {
                    GlassCard(cardPadding = 0) {
                        WindowDropdownPreference(
                            title = "选择月份",
                            entry = DropdownEntry(
                                items = filteredMonthData.mapIndexed { index, item ->
                                    DropdownItem(
                                        text = "第${item.month}月 ${item.startDate} ~ ${item.endDate}",
                                        selected = index == selectedIdx,
                                        onClick = { selectedIdx = index }
                                    )
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (loadingMonths) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
                        } else if (filteredMonthData.isEmpty()) {
                            Text("暂无月份数据", fontSize = 15.sp, color = appOnSurfaceVariant())
                        }
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("草稿", fontSize = 15.sp, color = appOnSurfaceVariant())
                            Spacer(modifier = Modifier.width(8.dp))
                            AppSwitch(checked = isDraft, onCheckedChange = { isDraft = it })
                        }
                        AppTextField(value = content, onValueChange = { content = it }, labelText = "请输入月记内容...", modifier = Modifier.fillMaxWidth(), maxLines = 10)
                    }
                }
                AppButton(
                    onClick = {
                        scope.launch {
                            if (selectedIdx < 0 || selectedIdx >= filteredMonthData.size) { statusMsg = "请选择月份"; return@launch }
                            if (content.isEmpty()) { statusMsg = "请输入月记内容"; return@launch }
                            submitting = true; statusMsg = ""
                            val m = filteredMonthData[selectedIdx]
                            try {
                                val success = ApiClient.saveMonthly(content, "${m.month}", m.startDate, m.endDate, if (isDraft) "true" else "false")
                                statusMsg = if (success) "提交成功" else "提交失败"
                                if (success) content = ""
                            } catch (_: Exception) { statusMsg = "网络错误" }
                            submitting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submitting
                ) { Text("提交月记", fontSize = 16.sp) }
                if (statusMsg.isNotEmpty()) {
                    Text(statusMsg, fontSize = 15.sp, color = if (statusMsg == "提交成功") androidx.compose.ui.graphics.Color(0xFF2E7D32) else appError())
                }
            }
    }
}
