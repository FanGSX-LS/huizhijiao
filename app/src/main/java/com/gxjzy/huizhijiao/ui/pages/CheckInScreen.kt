package com.gxjzy.huizhijiao.ui.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.CheckinResult
import com.gxjzy.huizhijiao.model.PresetData
import com.gxjzy.huizhijiao.model.PresetItem
import com.gxjzy.huizhijiao.map.LocationHelper
import com.gxjzy.huizhijiao.map.CoordTransform
import com.gxjzy.huizhijiao.ui.components.AmapView
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppOutlinedButton
import com.gxjzy.huizhijiao.ui.components.AppSwitch
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.theme.WarningOrange
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.theme.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import top.yukonga.miuix.kmp.basic.Icon

@Composable
fun CheckInScreen(onBack: () -> Unit) {
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
    var mapGcjLng by remember { mutableStateOf(0.0) }
    var mapGcjLat by remember { mutableStateOf(0.0) }

    val context = LocalContext.current

    fun doLocate() {
        locating = true
        scope.launch {
            val result = LocationHelper.locate(BRApp.instance)
            if (result.success) {
                locationX = result.longitude.toString()
                locationY = result.latitude.toString()
                label = result.address
                mapScale = "16"
                mapType = "baidu"
                val gcj = CoordTransform.bd09ToGcj02(result.longitude, result.latitude)
                mapGcjLng = gcj[0]
                mapGcjLat = gcj[1]
                Toast.makeText(context, "定位成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, result.errorMsg, Toast.LENGTH_SHORT).show()
            }
            locating = false
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            doLocate()
        } else {
            Toast.makeText(context, "位置权限未授予", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        try {
            presets = ApiClient.getPresets("checkin")
            if (selectedPresetIndex < 0 && presets.isNotEmpty()) {
                selectedPresetIndex = 0
                val d = presets[0].data
                locationX = d.locationX.toString()
                locationY = d.locationY.toString()
                label = d.label
                mapScale = if (d.scale == d.scale.toInt().toDouble()) d.scale.toInt().toString() else d.scale.toString()
                mapType = d.mapType
                isAbnormal = d.isAbnormal
                isEvection = d.isEvection
                val gcj = CoordTransform.bd09ToGcj02(d.locationX, d.locationY)
                mapGcjLng = gcj[0]
                mapGcjLat = gcj[1]
            }
        } catch (_: Exception) {}
    }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "实习签到",
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
                .imePadding()
                .padding(16.dp),
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
                                                mapScale = if (d.scale == d.scale.toInt().toDouble()) d.scale.toInt().toString() else d.scale.toString()
                                                mapType = d.mapType
                                                isAbnormal = d.isAbnormal
                                                isEvection = d.isEvection
                                                val gcj = CoordTransform.bd09ToGcj02(d.locationX, d.locationY)
                                                mapGcjLng = gcj[0]
                                                mapGcjLat = gcj[1]
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
                                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                    doLocate()
                                } else {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !locating
                        ) {
                            Text(if (locating) "定位中..." else "获取当前位置", fontSize = 15.sp)
                        }

                        if (mapGcjLng != 0.0 || mapGcjLat != 0.0) {
                            AmapView(
                                longitude = mapGcjLng,
                                latitude = mapGcjLat,
                                label = label,
                                mode = "pick",
                                onLocationPicked = { gcjLng, gcjLat, address ->
                                    val bd = CoordTransform.gcj02ToBd09(gcjLng, gcjLat)
                                    locationX = bd[0].toString()
                                    locationY = bd[1].toString()
                                    if (address.isNotEmpty()) label = address
                                    mapScale = "16"
                                    mapType = "baidu"
                                    mapGcjLng = gcjLng
                                    mapGcjLat = gcjLat
                                },
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }

                        AppTextField(
                            value = label,
                            onValueChange = { label = it },
                            labelText = "地址描述",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                            if (result.success) {
                                                resultTitle = "操作成功"
                                                resultMessage = "签到成功"
                                                resultSuccess = true
                                            } else {
                                                resultTitle = "操作失败"
                                                resultMessage = result.msg
                                                resultSuccess = false
                                            }
                                            showResultDialog = true
                                        } catch (e: Exception) {
                                            resultTitle = "操作失败"
                                            resultMessage = "网络错误: ${e.message}"
                                            resultSuccess = false
                                            showResultDialog = true
                                        }
                                        submitting = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFF2E7D32),
                                enabled = !submitting
                            ) { Text("签到", fontSize = 15.sp) }
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
                                            val result = ApiClient.doCheckin(presetData, "CHECKOUT")
                                            if (result.success) {
                                                resultTitle = "操作成功"
                                                resultMessage = "签退成功"
                                                resultSuccess = true
                                            } else {
                                                resultTitle = "操作失败"
                                                resultMessage = result.msg
                                                resultSuccess = false
                                            }
                                            showResultDialog = true
                                        } catch (e: Exception) {
                                            resultTitle = "操作失败"
                                            resultMessage = "网络错误: ${e.message}"
                                            resultSuccess = false
                                            showResultDialog = true
                                        }
                                        submitting = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !submitting
                            ) { Text("签退", fontSize = 15.sp) }
                        }
                    }
                }

                OverlayDialog(
                    show = showResultDialog,
                    onDismissRequest = { showResultDialog = false },
                    title = resultTitle,
                ) {
                    Text(resultMessage)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppButton(onClick = { showResultDialog = false }, modifier = Modifier.weight(1f), containerColor = if (resultSuccess) appPrimary() else appError()) { Text("确定") }
                    }
                }

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
                            singleLine = true
                        )
                        AppTextField(
                            value = locationY,
                            onValueChange = { locationY = it },
                            labelText = "纬度",
                            modifier = Modifier.fillMaxWidth(),
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
                                singleLine = true
                            )
                            AppTextField(
                                value = mapType,
                                onValueChange = { mapType = it },
                                labelText = "地图类型",
                                modifier = Modifier.weight(1f),
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
                                            val reqBody = json.toRequestBody("application/json".toMediaType())
                                            val resp = ApiClient.service.savePreset("checkin", reqBody)
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

                OverlayDialog(
                    show = showDeletePresetDialog,
                    onDismissRequest = { showDeletePresetDialog = false },
                    title = "删除预设",
                ) {
                    if (presets.isNotEmpty() && selectedPresetIndex in presets.indices) {
                        Text("确认删除预设「${presets[selectedPresetIndex].name}」吗？")
                    } else {
                        Text("请先选择预设")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextButton(text = "取消", modifier = Modifier.weight(1f), onClick = { showDeletePresetDialog = false })
                        AppButton(onClick = {
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
                        }, modifier = Modifier.weight(1f), containerColor = appError()) { Text("删除") }
                    }
                }

                OverlayBottomSheet(
                    show = showEditPresetDialog,
                    onDismissRequest = { showEditPresetDialog = false },
                    title = "编辑预设",
                ) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                            singleLine = true
                        )
                        AppTextField(
                            value = editLocationY,
                            onValueChange = { editLocationY = it },
                            labelText = "纬度",
                            modifier = Modifier.fillMaxWidth(),
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
                                singleLine = true
                            )
                            AppTextField(
                                value = editMapType,
                                onValueChange = { editMapType = it },
                                labelText = "地图类型",
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("异常", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                AppSwitch(checked = editIsAbnormal, onCheckedChange = { editIsAbnormal = it })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("出差", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                AppSwitch(checked = editIsEvection, onCheckedChange = { editIsEvection = it })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppOutlinedButton(
                                onClick = { showEditPresetDialog = false },
                                modifier = Modifier.weight(1f)
                            ) { Text("取消") }
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
                                                        "isAbnormal" to if (editIsAbnormal) 1 else 0,
                                                        "isEvection" to if (editIsEvection) 1 else 0
                                                    )
                                                )
                                                val json = com.gxjzy.huizhijiao.api.ApiClient.gson.toJson(data)
                                                val reqBody = json.toRequestBody("application/json".toMediaType())
                                                val resp = ApiClient.service.updatePreset(presets[selectedPresetIndex].id, reqBody)
                                                if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) {
                                                    Toast.makeText(context, "预设已更新", Toast.LENGTH_SHORT).show()
                                                    presets = ApiClient.getPresets("checkin")
                                                } else { Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show() }
                                            } catch (_: Exception) { Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show() }
                                        }
                                    }
                                    showEditPresetDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("保存") }
                        }
                    }
                }
            }
    }
}
