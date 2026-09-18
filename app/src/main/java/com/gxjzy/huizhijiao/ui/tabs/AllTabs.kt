package com.gxjzy.huizhijiao.ui.tabs

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.AutoCheckinConfig
import com.gxjzy.huizhijiao.model.AutoCheckinLog
import com.gxjzy.huizhijiao.model.PresetData
import com.gxjzy.huizhijiao.model.PresetItem
import com.gxjzy.huizhijiao.model.CheckinResult
import com.gxjzy.huizhijiao.model.UserInfo
import com.gxjzy.huizhijiao.map.LocationHelper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import com.gxjzy.huizhijiao.ui.components.AppSwitch
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppOutlinedButton
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.ui.components.AppAlertDialog
import com.gxjzy.huizhijiao.ui.components.AppSurface
import com.gxjzy.huizhijiao.ui.components.AppCheckbox
import com.gxjzy.huizhijiao.ui.components.AppCircularProgressIndicator
import com.gxjzy.huizhijiao.ui.components.AppDivider
import com.gxjzy.huizhijiao.ui.components.TimePickerSheet
import com.gxjzy.huizhijiao.ui.theme.WarningOrange
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun CheckInTab() {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()
    val advancedMode by prefs.advancedMode.distinctUntilChanged().collectAsState(initial = false)
    val savedCheckInPage = rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = savedCheckInPage.intValue, pageCount = { 2 })

    LaunchedEffect(pagerState.settledPage) {
        savedCheckInPage.intValue = pagerState.settledPage
    }

    Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Text(
            "签到",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appOnSurface(),
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
        )

        if (advancedMode) {
            TabRowWithContour(tabs = listOf("实习签到", "自动签到"), selectedTabIndex = pagerState.currentPage, onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } }, modifier = Modifier.padding(horizontal = 16.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = advancedMode
        ) { page ->
            when (page) {
                0 -> CheckInContent()
                1 -> if (advancedMode) AutoCheckInContent() else CheckInContent()
            }
        }
    }
}

@Composable
private fun CheckInContent() {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()
    val advancedMode by prefs.advancedMode.distinctUntilChanged().collectAsState(initial = false)

    var locationX by remember { mutableStateOf("") }
    var locationY by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var mapScale by remember { mutableStateOf("") }
    var mapType by remember { mutableStateOf("") }
    var isAbnormal by remember { mutableStateOf(false) }
    var isEvection by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var presets by remember { mutableStateOf<List<PresetItem>>(emptyList()) }
    var selectedPresetIndex by remember { mutableIntStateOf(-1) }
    var presetName by remember { mutableStateOf("我的预设") }
    var showResultDialog by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var resultSuccess by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showDeletePresetDialog by remember { mutableStateOf(false) }
    var showEditPresetDialog by remember { mutableStateOf(false) }
    var editPresetName by remember { mutableStateOf("") }
    var editLocationX by remember { mutableStateOf("") }
    var editLocationY by remember { mutableStateOf("") }
    var editLabel by remember { mutableStateOf("") }
    var editMapScale by remember { mutableStateOf("") }
    var editMapType by remember { mutableStateOf("") }
    var editIsAbnormal by remember { mutableStateOf(false) }
    var editIsEvection by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locating = true
            scope.launch {
                try {
                    val helper = com.gxjzy.huizhijiao.utils.LocationHelper(BRApp.instance)
                    val result = helper.fetchCurrentLocation()
                    locationX = result.longitude
                    locationY = result.latitude
                    label = result.address
                    mapScale = "16"
                    mapType = "Baidu"
                    Toast.makeText(context, "定位成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "定位失败", Toast.LENGTH_SHORT).show()
                }
                locating = false
            }
        } else {
            Toast.makeText(context, "位置权限未授予", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        try { presets = ApiClient.getPresets("checkin") } catch (_: Exception) {}
    }

    LaunchedEffect(presets) {
        if (advancedMode && presets.isNotEmpty() && selectedPresetIndex < 0) {
            selectedPresetIndex = 0
            val d = presets[0].data
            locationX = d.locationX.toString()
            locationY = d.locationY.toString()
            label = d.label
            mapScale = d.scale.toString()
            mapType = d.mapType
            isAbnormal = d.isAbnormal
            isEvection = d.isEvection
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (advancedMode && presets.isNotEmpty()) {
            GlassCard(cardPadding = 0) {
                Column {
                    WindowDropdownPreference(
                        title = "预设方案",
                        entry = DropdownEntry(
                            items = presets.mapIndexed { index, item ->
                                DropdownItem(
                                    text = item.name,
                                    selected = index == selectedPresetIndex,
                                    onClick = {
                                        selectedPresetIndex = index
                                        val d = presets[index].data
                                        locationX = d.locationX.toString()
                                        locationY = d.locationY.toString()
                                        label = d.label
                                        mapScale = d.scale.toString()
                                        mapType = d.mapType
                                        isAbnormal = d.isAbnormal
                                        isEvection = d.isEvection
                                        Toast.makeText(context, "已填入: ${presets[index].name}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppOutlinedButton(
                            onClick = {
                                if (presets.isNotEmpty() && selectedPresetIndex in presets.indices) {
                                    val d = presets[selectedPresetIndex].data
                                    editPresetName = presets[selectedPresetIndex].name
                                    editLocationX = d.locationX.toString()
                                    editLocationY = d.locationY.toString()
                                    editLabel = d.label
                                    editMapScale = d.scale.toString()
                                    editMapType = d.mapType
                                    editIsAbnormal = d.isAbnormal
                                    editIsEvection = d.isEvection
                                    showEditPresetDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("编辑", fontSize = 14.sp) }
                        AppOutlinedButton(
                            onClick = {
                                if (presets.isNotEmpty() && selectedPresetIndex in presets.indices) {
                                    showDeletePresetDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentColor = appError()
                        ) { Text("删除", fontSize = 14.sp) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                AppButton(
                    onClick = {
                        locationLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = appPrimary(),
                    enabled = !locating
                ) { Text(if (locating) "定位中..." else "获取当前位置", fontSize = 15.sp) }

                AppTextField(
                    value = label,
                    onValueChange = { label = it },
                    labelText = "地址描述",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (advancedMode) {
                AppButton(
                    onClick = { showSavePresetDialog = true },
                    modifier = Modifier.weight(1f),
                    containerColor = appPrimary()
                ) { Text("保存为预设", fontSize = 14.sp) }
            }
            AppButton(
                onClick = {
                    scope.launch {
                        submitting = true
                        try {
                            val presetData = PresetData(
                                locationX = locationX.toDoubleOrNull() ?: 0.0,
                                locationY = locationY.toDoubleOrNull() ?: 0.0,
                                label = label,
                                scale = mapScale.toDoubleOrNull() ?: 0.0,
                                mapType = mapType,
                                isAbnormal = isAbnormal,
                                isEvection = isEvection
                            )
                            val result = ApiClient.doCheckin(presetData, "CHECKIN")
                            if (result.success) { resultTitle = "操作成功"; resultMessage = "签到成功"; resultSuccess = true }
                            else { resultTitle = "操作失败"; resultMessage = result.msg; resultSuccess = false }
                            showResultDialog = true
                        } catch (e: Exception) {
                            resultTitle = "操作失败"; resultMessage = "网络错误: ${e.message}"; resultSuccess = false; showResultDialog = true
                        }
                        submitting = false
                    }
                },
                modifier = if (advancedMode) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                enabled = !submitting
            ) { Text("签到", fontSize = 16.sp) }
        }
    }

    AppAlertDialog(
        show = showResultDialog,
        onDismissRequest = { showResultDialog = false },
        title = resultTitle,
        text = { Text(resultMessage) },
        confirmButton = {
            AppButton(
                onClick = { showResultDialog = false },
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (resultSuccess) appPrimary() else appError()
            ) { Text("确定") }
        }
    )

    OverlayBottomSheet(
        show = showSavePresetDialog,
        onDismissRequest = { showSavePresetDialog = false },
        title = "保存预设",
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTextField(
                value = presetName,
                onValueChange = { presetName = it },
                labelText = "预设名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            AppTextField(
                value = locationX,
                onValueChange = { locationX = it },
                labelText = "经度",
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )
            AppTextField(
                value = locationY,
                onValueChange = { locationY = it },
                labelText = "纬度",
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )
            AppTextField(
                value = label,
                onValueChange = { label = it },
                labelText = "地址描述",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = mapScale,
                    onValueChange = { mapScale = it },
                    labelText = "缩放",
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    singleLine = true
                )
                AppTextField(
                    value = mapType,
                    onValueChange = { mapType = it },
                    labelText = "地图类型",
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    singleLine = true
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("异常", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    AppSwitch(checked = isAbnormal, onCheckedChange = { isAbnormal = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("出差", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    AppSwitch(checked = isEvection, onCheckedChange = { isEvection = it })
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppOutlinedButton(
                    onClick = { showSavePresetDialog = false },
                    modifier = Modifier.weight(1f)
                ) { Text("取消") }
                AppButton(
                    onClick = {
                        scope.launch {
                            try {
                                val data = mapOf<String, Any>(
                                    "locationX" to (locationX.toDoubleOrNull() ?: 0.0),
                                    "locationY" to (locationY.toDoubleOrNull() ?: 0.0),
                                    "label" to label,
                                    "scale" to (mapScale.toDoubleOrNull() ?: 0.0),
                                    "mapType" to mapType,
                                    "isAbnormal" to if (isAbnormal) 1 else 0,
                                    "isEvection" to if (isEvection) 1 else 0
                                )
                                val json = com.gxjzy.huizhijiao.api.ApiClient.gson.toJson(mapOf("name" to presetName, "data" to data))
                                val body = json.toRequestBody("application/json".toMediaType())
                                val resp = ApiClient.service.savePreset("checkin", body)
                                if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) {
                                    Toast.makeText(context, "预设已保存", Toast.LENGTH_SHORT).show()
                                    presets = ApiClient.getPresets("checkin")
                                } else { Toast.makeText(context, "保存预设失败", Toast.LENGTH_SHORT).show() }
                            } catch (_: Exception) { Toast.makeText(context, "保存预设失败", Toast.LENGTH_SHORT).show() }
                            showSavePresetDialog = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
            }
        }
    }

    AppAlertDialog(
        show = showDeletePresetDialog,
        onDismissRequest = { showDeletePresetDialog = false },
        title = "删除预设",
        text = {
            if (presets.isNotEmpty() && selectedPresetIndex in presets.indices) {
                Text("确认删除预设「${presets[selectedPresetIndex].name}」吗？")
            } else {
                Text("请先选择预设")
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    scope.launch {
                        if (presets.isNotEmpty() && selectedPresetIndex in presets.indices) {
                            try {
                                val id = presets[selectedPresetIndex].id
                                val resp = ApiClient.service.deletePreset(id)
                                if (resp.isSuccessful) {
                                    Toast.makeText(context, "预设已删除", Toast.LENGTH_SHORT).show()
                                    presets = ApiClient.getPresets("checkin")
                                    selectedPresetIndex = -1
                                } else { Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show() }
                            } catch (_: Exception) { Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show() }
                        }
                        showDeletePresetDialog = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = appError()
            ) { Text("删除") }
        },
        dismissButton = { AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeletePresetDialog = false }) }
    )

    AppAlertDialog(
        show = showEditPresetDialog,
        onDismissRequest = { showEditPresetDialog = false },
        title = "编辑预设",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = editPresetName,
                    onValueChange = { editPresetName = it },
                    labelText = "预设名称",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                AppTextField(
                    value = editLocationX,
                    onValueChange = { editLocationX = it },
                    labelText = "经度",
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    singleLine = true
                )
                AppTextField(
                    value = editLocationY,
                    onValueChange = { editLocationY = it },
                    labelText = "纬度",
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    singleLine = true
                )
                AppTextField(
                    value = editLabel,
                    onValueChange = { editLabel = it },
                    labelText = "地址描述",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(
                        value = editMapScale,
                        onValueChange = { editMapScale = it },
                        labelText = "缩放",
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        singleLine = true
                    )
                    AppTextField(
                        value = editMapType,
                        onValueChange = { editMapType = it },
                        labelText = "地图类型",
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        singleLine = true
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("异常", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        AppSwitch(checked = editIsAbnormal, onCheckedChange = { editIsAbnormal = it }, checkedTrackColor = appError())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("出差", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        AppSwitch(checked = editIsEvection, onCheckedChange = { editIsEvection = it }, checkedTrackColor = appPrimary())
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    if (presets.isNotEmpty() && selectedPresetIndex in presets.indices) {
                        scope.launch {
                            try {
                                val data = mapOf<String, Any>(
                                    "name" to editPresetName,
                                    "data" to mapOf<String, Any>(
                                        "locationX" to (editLocationX.toDoubleOrNull() ?: 0.0),
                                        "locationY" to (editLocationY.toDoubleOrNull() ?: 0.0),
                                        "label" to editLabel,
                                        "scale" to (editMapScale.toDoubleOrNull() ?: 0.0),
                                        "mapType" to editMapType,
                                        "isAbnormal" to editIsAbnormal,
                                        "isEvection" to editIsEvection
                                    )
                                )
                                val json = com.gxjzy.huizhijiao.api.ApiClient.gson.toJson(data)
                                val body = json.toRequestBody("application/json".toMediaType())
                                val resp = ApiClient.service.updatePreset(presets[selectedPresetIndex].id, body)
                                if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) {
                                    Toast.makeText(context, "预设已更新", Toast.LENGTH_SHORT).show()
                                    presets = ApiClient.getPresets("checkin")
                                } else { Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show() }
                            } catch (_: Exception) { Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show() }
                        }
                    }
                     showEditPresetDialog = false
                },
                 modifier = Modifier.fillMaxWidth()
             ) { Text("保存") }
         },
         dismissButton = { AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showEditPresetDialog = false }) }
     )
}

@Composable
private fun AutoCheckInContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var presets by remember { mutableStateOf<List<PresetItem>>(emptyList()) }
    var selectedPresetIndex by remember { mutableIntStateOf(-1) }
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

    suspend fun loadPresets() { try { presets = ApiClient.getPresets("checkin") } catch (_: Exception) {} }
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
                if (d.presetId > 0) { val idx = presets.indexOfFirst { it.id == d.presetId }; if (idx >= 0) selectedPresetIndex = idx }
                val parts = d.skipDays.split(",").filter { it.isNotEmpty() }
                val arr = BooleanArray(7) { false }
                for (p in parts) { val day = p.trim().toIntOrNull(); if (day != null && day in 0..6) arr[day] = true }
                skipDays = arr
            }
        } catch (_: Exception) {}
    }
    suspend fun refreshLogs() { try { logItems = ApiClient.getAutoCheckinLogs() } catch (_: Exception) {} }

    LaunchedEffect(Unit) { loadPresets(); loadStatus(); refreshLogs() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                                    val skipStr = skipDays.mapIndexed { idx, checked -> if (checked) idx.toString() else null }.filterNotNull().joinToString(",")
                                    try {
                                        val err = ApiClient.startAutoCheckin(presets[selectedPresetIndex].id, startHour, startMin, endHour, endMin, skipStr)
                                        if (err == null) { running = true; Toast.makeText(context, "已启动", Toast.LENGTH_SHORT).show(); loadStatus(); refreshLogs() }
                                        else { running = false; Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    } catch (e: Exception) { running = false; Toast.makeText(context, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show() }
                                }
                            } else {
                                scope.launch {
                                    try {
                                        val success = ApiClient.stopAutoCheckin()
                                        if (success) { running = false; Toast.makeText(context, "已停止", Toast.LENGTH_SHORT).show(); refreshLogs() }
                                        else { running = true; Toast.makeText(context, "停止失败", Toast.LENGTH_SHORT).show() }
                                    } catch (_: Exception) { running = true; Toast.makeText(context, "停止失败", Toast.LENGTH_SHORT).show() }
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        onClick = { showStartTimePicker = true },
                        enabled = !running, modifier = Modifier.weight(1f),
                        minHeight = 36.dp,
                        containerColor = appSurfaceContainerHigh(),
                        contentColor = if (running) appOnSurfaceVariant() else appOnSurface()
                    ) { Text("开始  ${String.format("%02d:%02d", startHour, startMin)}", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                    AppButton(
                        onClick = { showEndTimePicker = true },
                        enabled = !running, modifier = Modifier.weight(1f),
                        minHeight = 36.dp,
                        containerColor = appSurfaceContainerHigh(),
                        contentColor = if (running) appOnSurfaceVariant() else appOnSurface()
                    ) { Text("结束  ${String.format("%02d:%02d", endHour, endMin)}", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                    dayLabels.forEachIndexed { idx, name ->
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, fontSize = 11.sp, color = appOnSurfaceVariant())
                            AppCheckbox(checked = skipDays[idx], onCheckedChange = if (!running) { checked -> skipDays = skipDays.copyOf().also { it[idx] = checked } } else null, checkedColor = appError(), modifier = Modifier.size(26.dp))
                        }
                    }
                }


            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("日志", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(onClick = { scope.launch { refreshLogs() } }, minHeight = 32.dp, containerColor = appPrimary()) { Text("刷新", fontSize = 12.sp) }
                        AppButton(onClick = { scope.launch { try { val success = ApiClient.clearAutoCheckinLogs(); if (success) { logItems = emptyList(); Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show() } } catch (_: Exception) {} } }, minHeight = 32.dp, containerColor = appError()) { Text("清空", fontSize = 12.sp) }
                    }
                }
                if (logItems.isEmpty()) {
                    Text("暂无日志", fontSize = 13.sp, color = appOnSurfaceVariant())
                } else {
                    logItems.forEachIndexed { _, logItem ->
                        val cal3 = java.util.Calendar.getInstance()
                        cal3.timeInMillis = logItem.time * 1000
                        val ft2 = "${String.format("%02d", cal3.get(java.util.Calendar.HOUR_OF_DAY))}:${String.format("%02d", cal3.get(java.util.Calendar.MINUTE))}"
                        Text("[$ft2] ${logItem.entry}", fontSize = 12.sp, color = Color(0xFF888888), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
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

@Composable
fun MyTab(
    onNavigateToSettings: () -> Unit,
    onNavigateToTemplate: () -> Unit,
    onLogout: () -> Unit
) {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(UserInfo()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cached = ApiClient.getCachedUser()
        if (cached != null) user = cached
        try {
            val fetched = ApiClient.fetchMe()
            if (fetched != null) { user = fetched; ApiClient.setCachedUser(fetched) }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()).appOverScrollVertical()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "我的",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appOnSurface(),
            modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 12.dp, bottom = 0.dp)
        )

        ProfileCard(user)

        InternshipInfoCard(user)

        AppSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = appSurfaceContainerLow()
        ) {
            Column {
                MenuItemRow("设置") { onNavigateToSettings() }
                AppDivider(color = appOutlineVariant(), thickness = 0.5.dp)
                MenuItemRow("模板管理") { onNavigateToTemplate() }
                AppDivider(color = appOutlineVariant(), thickness = 0.5.dp)
                MenuItemRow("退出登录", textColor = appError()) { showLogoutDialog = true }
            }
        }
    }

    AppAlertDialog(
        show = showLogoutDialog,
        onDismissRequest = { showLogoutDialog = false },
        title = "退出登录",
        text = { Text("确认要退出登录吗？") },
        confirmButton = {
            AppTextButton(text = "确认", modifier = Modifier.fillMaxWidth(), onClick = {
                showLogoutDialog = false
                scope.launch {
                    prefs.clearCredentials()
                    ApiClient.setToken(null)
                    ApiClient.setCachedUser(null)
                    onLogout()
                }
            })
        },
        dismissButton = {
            AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showLogoutDialog = false })
        }
    )
}

@Composable
private fun ProfileCard(user: UserInfo) {
    val shape = RoundedCornerShape(24.dp)
    val colorsList = listOf(appPrimary(), appPrimary().copy(alpha = 0.7f))
    Box(modifier = Modifier.fillMaxWidth().clip(shape).background(Brush.linearGradient(colorsList), shape).padding(24.dp)) {
        Column {
            Text(
                user.name.ifEmpty { "未登录" },
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = appOnPrimary()
            )
            val studentNumber = user.number.ifEmpty { user.studentId }
            if (studentNumber.isNotEmpty()) {
                Text("学号：$studentNumber", fontSize = 15.sp, color = appOnPrimary().copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp))
            }
            if (user.clazzName.isNotEmpty()) {
                Text(user.clazzName, fontSize = 14.sp, color = appOnPrimary().copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun InternshipInfoCard(user: UserInfo) {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = appSurfaceContainerLow()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("实习信息", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = appOnSurface(), modifier = Modifier.padding(bottom = 16.dp))
            val intern = user.internship
            if (intern != null) {
                if (intern.internshipName.isNotEmpty()) InfoRow("实习项目", intern.internshipName)
                if (intern.companyName.isNotEmpty()) InfoRow("实习企业", intern.companyName)
                if (intern.internType.isNotEmpty()) InfoRow("实习类型", intern.internType)
                if (intern.teacherName.isNotEmpty()) InfoRow("指导老师", intern.teacherName)
                if (intern.startDate.isNotEmpty()) InfoRow("实习时间", "${intern.startDate} ~ ${intern.endDate}")
                if (intern.classHour > 0) InfoRow("学时", intern.classHour.toString())
                if (intern.credit > 0.0) InfoRow("学分", intern.credit.toString())
                if (intern.signedDays > 0 || intern.leastSignIn > 0) {
                    InfoRow("签到天数", "${intern.signedDays} / ${intern.leastSignIn} 天")
                }
            } else {
                Text("暂无实习信息", fontSize = 14.sp, color = appOnSurfaceVariant())
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(label, fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.width(72.dp))
        Text(value, fontSize = 15.sp, color = appOnSurface(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MenuItemRow(label: String, textColor: Color = Color.Unspecified, onClick: () -> Unit) {
    val resolvedTextColor = if (textColor != Color.Unspecified) textColor else appOnSurface()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 17.sp, color = resolvedTextColor, modifier = Modifier.weight(1f))
        Text(">", fontSize = 17.sp, color = appOutline())
    }
}
