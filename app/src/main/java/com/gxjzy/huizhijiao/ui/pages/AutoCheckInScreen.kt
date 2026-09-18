package com.gxjzy.huizhijiao.ui.pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.AutoCheckinConfig
import com.gxjzy.huizhijiao.model.AutoCheckinLog
import com.gxjzy.huizhijiao.model.PresetItem
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppCheckbox
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.AppOutlinedButton
import com.gxjzy.huizhijiao.ui.components.AppSwitch
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.components.TimePickerSheet
import com.gxjzy.huizhijiao.ui.theme.WarningOrange
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AutoCheckInScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var presets by remember { mutableStateOf<List<PresetItem>>(emptyList()) }
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var startHour by remember { mutableIntStateOf(8) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(12) }
    var endMin by remember { mutableIntStateOf(0) }
    var config by remember { mutableStateOf<AutoCheckinConfig?>(null) }
    var logItems by remember { mutableStateOf<List<AutoCheckinLog>>(emptyList()) }
    var skipDays by remember { mutableStateOf(BooleanArray(7) { it >= 5 }) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    suspend fun loadPresets() {
        try { presets = ApiClient.getPresets("checkin") } catch (_: Exception) {}
    }

    suspend fun loadStatus() {
        try {
            val d = ApiClient.getAutoCheckin()
            if (d != null) {
                config = d
                running = d.isRunning
                startHour = d.startHour
                startMin = d.startMin
                endHour = d.endHour
                endMin = d.endMin
                if (d.presetId > 0) {
                    val idx = presets.indexOfFirst { it.id == d.presetId }
                    if (idx >= 0) selectedPresetIndex = idx
                }
                val parts = d.skipDays.split(",").filter { it.isNotEmpty() }
                val arr = BooleanArray(7) { false }
                for (p in parts) {
                    val day = p.trim().toIntOrNull()
                    if (day != null && day in 0..6) arr[day] = true
                }
                skipDays = arr
            }
        } catch (_: Exception) {}
    }

    suspend fun refreshLogs() {
        try { logItems = ApiClient.getAutoCheckinLogs() } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        loadPresets()
        loadStatus()
        refreshLogs()
    }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "自动签到",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackground())
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()).appOverScrollVertical()
                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("当前状态", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            AppSwitch(
                                checked = running,
                                onCheckedChange = { target ->
                                    if (target) {
                                        scope.launch {
                                            if (presets.isEmpty() || selectedPresetIndex !in presets.indices) {
                                                Toast.makeText(context, "请先选择预设", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            val skipStr = skipDays.mapIndexed { idx, checked ->
                                                if (checked) idx.toString() else null
                                            }.filterNotNull().joinToString(",")
                                            try {
                                                val err = ApiClient.startAutoCheckin(presets[selectedPresetIndex].id, startHour, startMin, endHour, endMin, skipStr)
                                                if (err == null) {
                                                    running = true
                                                    Toast.makeText(context, "已启动", Toast.LENGTH_SHORT).show()
                                                    loadStatus()
                                                    refreshLogs()
                                                } else {
                                                    running = false
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                running = false
                                                Toast.makeText(context, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            try {
                                                val success = ApiClient.stopAutoCheckin()
                                                if (success) {
                                                    running = false
                                                    Toast.makeText(context, "已停止", Toast.LENGTH_SHORT).show()
                                                    refreshLogs()
                                                } else {
                                                    running = true
                                                    Toast.makeText(context, "停止失败", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (_: Exception) {
                                                running = true
                                                Toast.makeText(context, "停止失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        config?.let { c ->
                            if (c.lastCheckinStatus != null && c.lastCheckinTime > 0L) {
                                val cal2 = java.util.Calendar.getInstance()
                                cal2.timeInMillis = c.lastCheckinTime * 1000
                                val ft = "${cal2.get(java.util.Calendar.MONTH) + 1}-${cal2.get(java.util.Calendar.DAY_OF_MONTH)} ${String.format("%02d", cal2.get(java.util.Calendar.HOUR_OF_DAY))}:${String.format("%02d", cal2.get(java.util.Calendar.MINUTE))}"
                                Text("上次签到：$ft - ${c.lastCheckinStatus}", fontSize = 13.sp, color = appOnSurfaceVariant())
                            }
                        }
                    }
                }

                if (presets.isNotEmpty()) {
                    GlassCard(cardPadding = 0) {
                        WindowDropdownPreference(
                            title = "选择预设",
                            entry = DropdownEntry(
                                items = presets.mapIndexed { index, item ->
                                    DropdownItem(
                                        text = item.name,
                                        selected = index == selectedPresetIndex,
                                        onClick = { selectedPresetIndex = index }
                                    )
                                }
                            ),
                            enabled = !running,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    GlassCard {
                        Text("暂无预设，请先在签到页保存", fontSize = 14.sp, color = appError())
                    }
                }

                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppButton(
                                onClick = { showStartTimePicker = true },
                                enabled = !running,
                                modifier = Modifier.weight(1f),
                                minHeight = 36.dp,
                                containerColor = appSurfaceContainerHigh(),
                                contentColor = if (running) appOnSurfaceVariant() else appOnSurface()
                            ) {
                                Text("开始  ${String.format("%02d:%02d", startHour, startMin)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            AppButton(
                                onClick = { showEndTimePicker = true },
                                enabled = !running,
                                modifier = Modifier.weight(1f),
                                minHeight = 36.dp,
                                containerColor = appSurfaceContainerHigh(),
                                contentColor = if (running) appOnSurfaceVariant() else appOnSurface()
                            ) {
                                Text("结束  ${String.format("%02d:%02d", endHour, endMin)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                            dayLabels.forEachIndexed { idx, name ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(name, fontSize = 11.sp, color = appOnSurfaceVariant())
                                    AppCheckbox(
                                        checked = skipDays[idx],
                                        onCheckedChange = if (!running) { checked ->
                                            skipDays = skipDays.copyOf().also { it[idx] = checked }
                                        } else null,
                                        modifier = Modifier.size(26.dp),
                                        checkedColor = appError()
                                    )
                                }
                            }
                        }


                    }
                }

                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("日志", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton(onClick = { scope.launch { refreshLogs() } }, minHeight = 32.dp, containerColor = appPrimary()) { Text("刷新", fontSize = 12.sp) }
                                AppButton(onClick = {
                                    scope.launch {
                                        try {
                                            val success = ApiClient.clearAutoCheckinLogs()
                                            if (success) { logItems = emptyList(); Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show() }
                                        } catch (_: Exception) {}
                                    }
                                }, minHeight = 32.dp, containerColor = appError()) { Text("清空", fontSize = 12.sp) }
                            }
                        }
                        if (logItems.isEmpty()) {
                            Text("暂无日志", fontSize = 13.sp, color = appOnSurfaceVariant())
                        } else {
                            logItems.forEachIndexed { index, logItem ->
                                val cal3 = java.util.Calendar.getInstance()
                                cal3.timeInMillis = logItem.time * 1000
                                val ft2 = "${String.format("%02d", cal3.get(java.util.Calendar.HOUR_OF_DAY))}:${String.format("%02d", cal3.get(java.util.Calendar.MINUTE))}"
                                Text(
                                    "[$ft2] ${logItem.entry}",
                                    fontSize = 12.sp, color = Color(0xFF888888),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
    }

    TimePickerSheet(
        show = showStartTimePicker,
        onDismiss = { showStartTimePicker = false },
        onConfirm = { h, m -> startHour = h; startMin = m; showStartTimePicker = false },
        initialHour = startHour,
        initialMinute = startMin
    )

    TimePickerSheet(
        show = showEndTimePicker,
        onDismiss = { showEndTimePicker = false },
        onConfirm = { h, m -> endHour = h; endMin = m; showEndTimePicker = false },
        initialHour = endHour,
        initialMinute = endMin
    )
}
