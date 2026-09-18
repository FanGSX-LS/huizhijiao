package com.gxjzy.huizhijiao.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.data.AppDataCache
import com.gxjzy.huizhijiao.model.TemplateData
import com.gxjzy.huizhijiao.model.TemplateItem
import com.gxjzy.huizhijiao.model.AutoReportLog
import com.gxjzy.huizhijiao.model.AutoReportStatus
import com.gxjzy.huizhijiao.model.WeekItem
import com.gxjzy.huizhijiao.model.MonthItem
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppOutlinedButton
import com.gxjzy.huizhijiao.ui.components.AppSwitch
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.components.TimePickerSheet
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun TemplateScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val prefs = BRApp.instance.prefs
    val advancedMode by prefs.advancedMode.distinctUntilChanged().collectAsState(initial = false)
    var currentSubTab by remember { mutableIntStateOf(0) }
    val maxTab = if (advancedMode) 2 else 1
    if (currentSubTab > maxTab) currentSubTab = 0
    val tplType = if (currentSubTab == 0) "week" else "month"

    var weekTemplates by remember { mutableStateOf<List<TemplateItem>>(emptyList()) }
    var monthTemplates by remember { mutableStateOf<List<TemplateItem>>(emptyList()) }
    var weekPeriods by remember { mutableStateOf<List<WeekItem>>(emptyList()) }
    var monthPeriods by remember { mutableStateOf<List<MonthItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var statusMsg by remember { mutableStateOf("") }
    var publishingId by remember { mutableLongStateOf(0L) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TemplateItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteIndex by remember { mutableIntStateOf(-1) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importPreview by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var importJsonText by remember { mutableStateOf("") }

    val context = LocalContext.current

    suspend fun loadTemplates() {
        loading = true
        try {
            if (AppDataCache.weekTemplates != null && AppDataCache.monthTemplates != null) {
                weekTemplates = AppDataCache.weekTemplates!!
                monthTemplates = AppDataCache.monthTemplates!!
            } else {
                weekTemplates = ApiClient.getTemplates("week")
                monthTemplates = ApiClient.getTemplates("month")
                AppDataCache.weekTemplates = weekTemplates
                AppDataCache.monthTemplates = monthTemplates
            }
            if (AppDataCache.weekItems != null && AppDataCache.monthItems != null) {
                weekPeriods = AppDataCache.weekItems!!
                monthPeriods = AppDataCache.monthItems!!
            } else {
                weekPeriods = ApiClient.fetchWeeks()
                monthPeriods = ApiClient.fetchMonths()
                AppDataCache.weekItems = weekPeriods
                AppDataCache.monthItems = monthPeriods
            }
        } catch (_: Exception) { statusMsg = "加载模板失败" }
        loading = false
    }

    LaunchedEffect(Unit) { loadTemplates() }

    val templates = if (tplType == "week") weekTemplates else monthTemplates
    val unitLabel = if (tplType == "week") "周" else "月"
    val periodKey = if (tplType == "week") "week" else "months"

    val jsonFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val text = inputStream.bufferedReader().readText()
                    inputStream.close()
                    importJsonText = text
                    val arr = com.google.gson.JsonParser.parseString(text).asJsonArray
                    importPreview = arr.map { elem ->
                        val obj = elem.asJsonObject
                        mutableMapOf<String, Any?>(
                            periodKey to (obj.get(periodKey)?.asInt ?: 0),
                            "content" to (obj.get("content")?.asString ?: ""),
                            "siteInstruction" to (obj.get("siteInstruction")?.asString ?: "0"),
                            "contactTimes" to (obj.get("contactTimes")?.asString ?: "0")
                        )
                    }
                    showImportDialog = true
                }
            } catch (_: Exception) {
                importJsonText = ""
                importPreview = emptyList()
                statusMsg = "JSON解析失败，请检查格式"
            }
        }
    }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "模板管理",
                navigationIcon = { AppIconButton(onClick = onBack) { Icon(MiuixIcons.Back, contentDescription = "返回") } },
                actions = {
                    AppIconButton(onClick = {
                        importJsonText = ""
                        importPreview = emptyList()
                        jsonFileLauncher.launch("application/json")
                    }) {
                        Icon(MiuixIcons.UploadCloud, contentDescription = "导入JSON")
                    }
                    AppIconButton(onClick = { showAddDialog = true }) {
                        Icon(MiuixIcons.Add, contentDescription = "添加模板")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(appBackground()).padding(innerPadding)) {
                TabRowWithContour(
                    tabs = if (advancedMode) listOf("周报模板", "月报模板", "自动发布") else listOf("周报模板", "月报模板"),
                    selectedTabIndex = currentSubTab.coerceAtMost(maxTab),
                    onTabSelected = { currentSubTab = it },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                )

                if (currentSubTab == 2 && advancedMode) {
                    AutoReportContent()
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).appOverScrollVertical().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appPrimary(), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("加载中...", fontSize = 15.sp, color = appOnSurfaceVariant())
                            }
                        } else if (templates.isEmpty()) {
                            GlassCard {
                                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("暂无${unitLabel}报模板", fontSize = 16.sp, color = appOnSurfaceVariant())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("点击右上角 + 添加，或导入JSON批量创建", fontSize = 13.sp, color = appOnSurfaceVariant())
                                }
                            }
                        }

                        templates.sortedBy { it.sortKey }.forEachIndexed { idx, template ->
                            GlassCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val periodNum = if (tplType == "week") template.data.week else template.data.months
                                    val displayName = if (periodNum > 0) "第${periodNum}${unitLabel}" else template.data.label.ifEmpty { "未命名模板" }
                                    Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = appPrimary())
                                    if (template.data.content.isNotEmpty()) {
                                        Text(template.data.content.take(50) + if (template.data.content.length > 50) "..." else "", fontSize = 13.sp, color = appOnSurfaceVariant())
                                    }
                                    if (tplType == "week" && (template.data.siteInstruction.isNotEmpty() || template.data.contactTimes.isNotEmpty())) {
                                        Text("指导:${template.data.siteInstruction.ifEmpty { "0" }}  联系:${template.data.contactTimes.ifEmpty { "0" }}", fontSize = 12.sp, color = appOnSurfaceVariant())
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppButton(
                                            onClick = {
                                                scope.launch {
                                                    publishingId = template.id
                                                    try {
                                                        val result = if (tplType == "week") {
                                                            val wItem = weekPeriods.find { it.week == periodNum }
                                                            if (wItem != null) {
                                                                ApiClient.saveWeekly(
                                                                    template.data.content, "$periodNum",
                                                                    wItem.startDate, wItem.endDate, "false",
                                                                    template.data.siteInstruction.ifEmpty { "0" },
                                                                    template.data.contactTimes.ifEmpty { "0" }
                                                                )
                                                            } else {
                                                                statusMsg = "未找到第${periodNum}周的日期信息"
                                                                publishingId = 0L
                                                                return@launch
                                                            }
                                                        } else {
                                                            val mItem = monthPeriods.find { it.month == periodNum }
                                                            if (mItem != null) {
                                                                ApiClient.saveMonthly(
                                                                    template.data.content, "$periodNum",
                                                                    mItem.startDate, mItem.endDate, "false"
                                                                )
                                                            } else {
                                                                statusMsg = "未找到第${periodNum}月的日期信息"
                                                                publishingId = 0L
                                                                return@launch
                                                            }
                                                        }
                                                        if (result.first) {
                                                            android.widget.Toast.makeText(context, "发布成功", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            statusMsg = "发布失败: ${result.second}"
                                                        }
                                                    } catch (e: Exception) {
                                                        statusMsg = "发布失败: ${e.message}"
                                                    }
                                                    publishingId = 0L
                                                }
                                            },
                                            minHeight = 32.dp,
                                            containerColor = appPrimary(),
                                            enabled = publishingId != template.id && periodNum > 0 && template.data.content.isNotEmpty()
                                        ) {
                                            if (publishingId == template.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = appOnPrimary(), strokeWidth = 1.5.dp)
                                            } else {
                                                Text("发布", fontSize = 13.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        AppOutlinedButton(
                                            onClick = { editingTemplate = template; showEditDialog = true },
                                            contentColor = appPrimary(),
                                            minHeight = 32.dp
                                        ) { Text("编辑", fontSize = 13.sp) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(MiuixIcons.Delete, contentDescription = "删除", tint = appError(),
                                            modifier = Modifier.clip(CircleShape).clickable { pendingDeleteIndex = idx; showDeleteDialog = true }.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (statusMsg.isNotEmpty()) {
                            Text(statusMsg, fontSize = 14.sp, color = appError(), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        TemplateEditSheet(
            show = showAddDialog,
            title = "添加${unitLabel}报模板",
            tplType = tplType,
            initial = TemplateItem(id = System.currentTimeMillis(), sortKey = templates.size, tplType = tplType, data = TemplateData()),
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                scope.launch {
                    val updated = templates + newItem
                    val success = ApiClient.saveTemplatesRemote(tplType, updated)
                    if (success) { loadTemplates(); showAddDialog = false }
                    else statusMsg = "保存失败"
                }
            }
        )

        TemplateEditSheet(
            show = showEditDialog && editingTemplate != null,
            title = "编辑${unitLabel}报模板",
            tplType = tplType,
            initial = editingTemplate ?: TemplateItem(id = 0, sortKey = 0, tplType = tplType, data = TemplateData()),
            onDismiss = { showEditDialog = false; editingTemplate = null },
            onConfirm = { updated ->
                scope.launch {
                    val updatedList = templates.map { if (it.id == updated.id) updated else it }
                    val success = ApiClient.saveTemplatesRemote(tplType, updatedList)
                    if (success) { loadTemplates(); showEditDialog = false; editingTemplate = null }
                    else statusMsg = "保存失败"
                }
            }
        )

        OverlayDialog(
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog = false; pendingDeleteIndex = -1 },
            title = "确认删除",
        ) {
            Text("删除后模板将无法恢复，确认删除吗？")
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = false; pendingDeleteIndex = -1 })
                AppButton(onClick = {
                    scope.launch {
                        val sorted = templates.sortedBy { it.sortKey }
                        if (pendingDeleteIndex in sorted.indices) {
                            val toRemove = sorted[pendingDeleteIndex]
                            val updatedList = templates.filter { it !== toRemove }
                            val success = ApiClient.saveTemplatesRemote(tplType, updatedList)
                            if (success) { loadTemplates(); showDeleteDialog = false; pendingDeleteIndex = -1 }
                            else statusMsg = "删除失败"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), containerColor = appError()) { Text("删除") }
            }
        }

        OverlayDialog(
            show = showImportDialog,
            onDismissRequest = { showImportDialog = false; importPreview = emptyList(); importJsonText = "" },
            title = "导入${unitLabel}报模板",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("预览：共 ${importPreview.size} 条模板", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (importPreview.size > 0) {
                    val previewText = importPreview.take(5).joinToString("\n") { item ->
                        val num = item[periodKey]
                        val content = (item["content"] as? String ?: "").take(30)
                        "第${num}${unitLabel}: ${content}${if ((item["content"] as? String ?: "").length > 30) "..." else ""}"
                    }
                    Text(previewText, fontSize = 13.sp, color = appOnSurfaceVariant())
                    if (importPreview.size > 5) {
                        Text("...还有 ${importPreview.size - 5} 条", fontSize = 12.sp, color = appOnSurfaceVariant())
                    }
                }
                Text("注意：导入将替换当前所有${unitLabel}报模板", fontSize = 13.sp, color = appError())
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showImportDialog = false; importPreview = emptyList(); importJsonText = "" })
                AppButton(
                    onClick = {
                        scope.launch {
                            val success = ApiClient.importTemplatesRemote(tplType, importPreview)
                            if (success) {
                                loadTemplates()
                                statusMsg = "导入成功，共 ${importPreview.size} 条"
                                showImportDialog = false
                                importPreview = emptyList()
                                importJsonText = ""
                            } else {
                                statusMsg = "导入失败"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = appPrimary(),
                    enabled = importPreview.isNotEmpty()
                ) { Text("确认导入") }
            }
        }
        }
    }

@Composable
private fun TemplateEditSheet(show: Boolean, title: String, tplType: String, initial: TemplateItem, onDismiss: () -> Unit, onConfirm: (TemplateItem) -> Unit) {
    val unitLabel = if (tplType == "week") "周" else "月"
    val periodKey = if (tplType == "week") "week" else "months"
    var periodNum by remember { mutableIntStateOf(if (periodKey == "week") initial.data.week else initial.data.months) }
    var content by remember { mutableStateOf(initial.data.content) }
    var siteInstruction by remember { mutableStateOf(initial.data.siteInstruction) }
    var contactTimes by remember { mutableStateOf(initial.data.contactTimes) }

    LaunchedEffect(initial) {
        periodNum = if (periodKey == "week") initial.data.week else initial.data.months
        content = initial.data.content
        siteInstruction = initial.data.siteInstruction
        contactTimes = initial.data.contactTimes
    }

    OverlayBottomSheet(
        show = show,
        onDismissRequest = onDismiss,
        title = title,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(0.95f).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTextField(
                value = if (periodNum > 0) periodNum.toString() else "",
                onValueChange = { periodNum = it.toIntOrNull() ?: 0 },
                labelText = "${unitLabel}数",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            AppTextField(
                value = content,
                onValueChange = { content = it },
                labelText = "模板内容",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
            if (tplType == "week") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(value = siteInstruction, onValueChange = { siteInstruction = it }, labelText = "指导次数", modifier = Modifier.weight(1f), singleLine = true)
                    AppTextField(value = contactTimes, onValueChange = { contactTimes = it }, labelText = "联系次数", modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(onClick = onDismiss, modifier = Modifier.weight(1f), containerColor = appSurfaceContainerHigh(), contentColor = appOnSurface()) { Text("取消") }
                AppButton(
                    onClick = {
                        onConfirm(initial.copy(
                            tplType = tplType,
                            data = TemplateData(
                                label = "第${periodNum}${unitLabel}",
                                content = content,
                                siteInstruction = siteInstruction,
                                contactTimes = contactTimes,
                                week = if (tplType == "week") periodNum else 0,
                                months = if (tplType == "month") periodNum else 0
                            )
                        ))
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = appPrimary(),
                    enabled = periodNum > 0 && content.isNotEmpty()
                ) { Text("确定") }
            }
        }
    }
}

@Composable
private fun AutoReportContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var reportType by remember { mutableStateOf("week") }
    var reportStatus by remember { mutableStateOf<AutoReportStatus?>(null) }
    var weekPeriods by remember { mutableStateOf<List<WeekItem>>(emptyList()) }
    var monthPeriods by remember { mutableStateOf<List<MonthItem>>(emptyList()) }
    var logItems by remember { mutableStateOf<List<AutoReportLog>>(emptyList()) }
    var statusMsg by remember { mutableStateOf("") }

    var triggerMode by remember { mutableStateOf("auto") }
    var startHour by remember { mutableIntStateOf(8) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(20) }
    var endMin by remember { mutableIntStateOf(0) }
    var selectedPeriods by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val currentConfig = if (reportType == "week") reportStatus?.week else reportStatus?.month
    val isRunning = currentConfig?.isRunning == true

    suspend fun loadStatus() {
        try {
            val s = ApiClient.getAutoReport()
            reportStatus = s
            val cfg = if (reportType == "week") s?.week else s?.month
            if (cfg != null) {
                triggerMode = cfg.triggerMode
                startHour = cfg.startHour
                startMin = cfg.startMin
                endHour = cfg.endHour
                endMin = cfg.endMin
                selectedPeriods =
                    cfg.selectedPeriods.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun loadPeriods() {
        try {
            if (reportType == "week") weekPeriods = ApiClient.fetchWeeks()
            else monthPeriods = ApiClient.fetchMonths()
        } catch (_: Exception) {
        }
    }

    suspend fun refreshLogs() {
        try {
            logItems = ApiClient.getAutoReportLogs(reportType)
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(Unit) { loadStatus(); loadPeriods(); refreshLogs() }
    LaunchedEffect(reportType) { loadStatus(); loadPeriods(); refreshLogs() }

    val reportTypeIndex = if (reportType == "month") 1 else 0
    val triggerModeIndex = if (triggerMode == "selected") 1 else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(cardPadding = 0) {
            WindowDropdownPreference(
                title = "报告类型",
                items = listOf("周记", "月记"),
                selectedIndex = reportTypeIndex,
                onSelectedIndexChange = { idx -> reportType = if (idx == 1) "month" else "week" },
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("当前状态", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    AppSwitch(
                        checked = isRunning,
                        onCheckedChange = { target ->
                            if (target) {
                                scope.launch {
                                    val periodsStr = selectedPeriods.sorted().joinToString(",")
                                    val err = ApiClient.startAutoReport(reportType, triggerMode, startHour, startMin, endHour, endMin, periodsStr)
                                    if (err == null) { statusMsg = "已启动"; loadStatus(); refreshLogs() }
                                    else { statusMsg = err }
                                }
                            } else {
                                scope.launch {
                                    val success = ApiClient.stopAutoReport(reportType)
                                    if (success) { statusMsg = "已停止"; loadStatus(); refreshLogs() }
                                    else { statusMsg = "停止失败" }
                                }
                            }
                        }
                    )
                }

                currentConfig?.let { cfg ->
                    if (cfg.lastReportStatus.isNotEmpty() && cfg.lastReportTime > 0L) {
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = cfg.lastReportTime * 1000
                        val ft = "${cal.get(java.util.Calendar.MONTH) + 1}-${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))}:${String.format("%02d", cal.get(java.util.Calendar.MINUTE))}"
                        Text("上次执行：$ft - ${cfg.lastReportStatus}", fontSize = 13.sp, color = appOnSurfaceVariant())
                    }
                }
            }
        }

        GlassCard(cardPadding = 0) {
            WindowDropdownPreference(
                title = "触发模式",
                items = listOf("自动检测", "指定周期"),
                selectedIndex = triggerModeIndex,
                onSelectedIndexChange = { idx -> triggerMode = if (idx == 1) "selected" else "auto" },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("开始时间", fontSize = 14.sp, color = appOnSurface())
                    AppButton(
                        onClick = { showStartTimePicker = true },
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f),
                        minHeight = 36.dp,
                        containerColor = appSurfaceContainerHigh(),
                        contentColor = if (isRunning) appOnSurfaceVariant() else appOnSurface()
                    ) {
                        Text(String.format("%02d:%02d", startHour, startMin), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("结束时间", fontSize = 14.sp, color = appOnSurface())
                    AppButton(
                        onClick = { showEndTimePicker = true },
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f),
                        minHeight = 36.dp,
                        containerColor = appSurfaceContainerHigh(),
                        contentColor = if (isRunning) appOnSurfaceVariant() else appOnSurface()
                    ) {
                        Text(String.format("%02d:%02d", endHour, endMin), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                if (triggerMode == "selected") {
                    Text("选择周期", fontSize = 14.sp, color = appOnSurface())
                    val periods = if (reportType == "week") weekPeriods.map { it.week } else monthPeriods.map { it.month }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        periods.forEach { num ->
                            AppOutlinedButton(
                                onClick = {
                                    selectedPeriods = if (num in selectedPeriods) selectedPeriods - num else selectedPeriods + num
                                },
                                contentColor = if (num in selectedPeriods) appPrimary() else appOnSurfaceVariant(),
                                enabled = !isRunning
                            ) { Text(if (reportType == "week") "第${num}周" else "第${num}月", fontSize = 13.sp) }
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
                        AppButton(
                            onClick = { scope.launch { refreshLogs() } },
                            minHeight = 32.dp,
                            containerColor = appPrimary()
                        ) { Text("刷新", fontSize = 12.sp) }
                        AppButton(
                            onClick = {
                                scope.launch {
                                    val success = ApiClient.clearAutoReportLogs(reportType)
                                    if (success) { logItems = emptyList(); statusMsg = "日志已清空" }
                                }
                            },
                            minHeight = 32.dp,
                            containerColor = appError()
                        ) { Text("清空", fontSize = 12.sp) }
                    }
                }
                if (logItems.isEmpty()) {
                    Text("暂无日志", fontSize = 13.sp, color = appOnSurfaceVariant())
                } else {
                    logItems.forEach { logItem ->
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = logItem.time * 1000
                        val ft = "${String.format("%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))}:${String.format("%02d", cal.get(java.util.Calendar.MINUTE))}"
                        Text("[$ft] ${logItem.entry}", fontSize = 12.sp, color = Color(0xFF888888), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                    }
                }
            }
        }

        if (statusMsg.isNotEmpty()) {
            Text(
                statusMsg, fontSize = 15.sp,
                color = if (statusMsg.contains("失败") || statusMsg.contains("错误")) appError() else appPrimary(),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
