package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Unlock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.model.AdminAutoCheckin
import com.gxjzy.huizhijiao.model.AdminUser
import com.gxjzy.huizhijiao.model.CheckinLog
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.AppOutlinedButton
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.theme.CardRed
import com.gxjzy.huizhijiao.ui.theme.CardGreen
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.gxjzy.huizhijiao.ui.theme.WarningOrange
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun AdminScreen(onLogout: () -> Unit) {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "管理仪表盘",
                actions = {
                    AppIconButton(onClick = { showLogoutDialog = true }) {
                        Icon(MiuixIcons.Unlock, contentDescription = "退出")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("仪表盘", "用户管理", "签到管理").forEachIndexed { idx, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (selectedTab == idx) appSurface() else Color(0xFFF0F0F0))
                                .clickable { selectedTab = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label, fontSize = 15.sp,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == idx) appPrimary() else appOnSurfaceVariant()
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> AdminDashboardContent()
                    1 -> AdminUsersContent()
                    2 -> AdminAutoCheckinsContent()
                }
            }
    }

    OverlayDialog(
        show = showLogoutDialog,
        onDismissRequest = { showLogoutDialog = false },
        title = "退出登录",
    ) {
        Text("确定要退出登录吗？")
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showLogoutDialog = false })
            AppButton(onClick = {
                showLogoutDialog = false
                scope.launch {
                    prefs.clearCredentials()
                    ApiClient.setToken(null)
                    onLogout()
                }
            }, modifier = Modifier.fillMaxWidth(), containerColor = appError()) { Text("确定") }
        }
    }
}

@Composable
private fun AdminDashboardContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var autoCheckins by remember { mutableStateOf<List<AdminAutoCheckin>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun loadData() {
        loading = true
        try {
            users = ApiClient.getAdminUsers()
            autoCheckins = ApiClient.getAdminAutoCheckins()
        } catch (_: Exception) { Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show() }
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    val activeCount = users.count { it.autoCheckin?.isRunning == true }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).appOverScrollVertical().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(appSurface()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${users.size}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = appPrimary())
                Text("注册用户", fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 6.dp))
            }
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(appSurface()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$activeCount", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("自动签到", fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 6.dp))
            }
        }

        Text("活跃自动签到任务", fontSize = 17.sp, fontWeight = FontWeight.Medium)

        if (autoCheckins.isEmpty()) {
            Text("暂无活跃任务", fontSize = 15.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(28.dp))
        }

        autoCheckins.forEach { item ->
            GlassCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.studentName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = appOnSurface(), modifier = Modifier.weight(1f))
                        AppButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val resp = ApiClient.service.stopAdminCheckin(item.userId)
                                        if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) { Toast.makeText(context, "已停止", Toast.LENGTH_SHORT).show(); loadData() }
                                        else Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) { Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            containerColor = appError()
                        ) { Text("停止", fontSize = 13.sp) }
                    }
                    Text("学号：${item.loginName}  预设：${item.presetName}", fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 6.dp))
                    if (!item.lastStatus.isNullOrEmpty()) {
                        Text("上次状态：${item.lastStatus}", fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

    }
}

@Composable
private fun AdminUsersContent() {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var logsData by remember { mutableStateOf<List<CheckinLog>>(emptyList()) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteUserId by remember { mutableLongStateOf(-1L) }
    var showAdvancedModeDialog by remember { mutableStateOf(false) }
    var advancedModeUserId by remember { mutableLongStateOf(-1L) }
    var advancedModeStartDate by remember { mutableStateOf("") }
    var advancedModeEndDate by remember { mutableStateOf("") }
    var advancedModePreset by remember { mutableStateOf("") }
    val context = LocalContext.current

    suspend fun loadData() {
        loading = true
        try {
            users = ApiClient.getAdminUsers()
        } catch (_: Exception) { Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show() }
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).appOverScrollVertical().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!loading && users.isEmpty()) {
            Text("暂无用户", fontSize = 14.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
        }

        users.forEach { user ->
            GlassCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.studentName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appOnSurface(), modifier = Modifier.weight(1f))
                        when {
                            !user.isActive -> Text("已禁用", fontSize = 12.sp, color = appError(), modifier = Modifier.background(CardRed, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                            user.autoCheckin?.isRunning == true -> Text("运行中", fontSize = 12.sp, color = Color(0xFF2E7D32), modifier = Modifier.background(CardGreen, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                            else -> Text("正常", fontSize = 12.sp, color = Color(0xFF2E7D32), modifier = Modifier.background(CardGreen, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Text("学号: ${user.loginName}  班级: ${user.clazzName}", fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 4.dp))
                    Text("实习: ${user.internshipName.ifEmpty { "无" }}", fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 2.dp))
                    if (user.advancedModeStart.isNotEmpty() || user.advancedModeEnd.isNotEmpty()) {
                        val expired = user.advancedModeEnd.isNotEmpty() && java.time.LocalDate.now().isAfter(java.time.LocalDate.parse(user.advancedModeEnd))
                        Text(
                            "高级模式: ${user.advancedModeStart} ~ ${user.advancedModeEnd}${if (expired) " (已到期)" else ""}",
                            fontSize = 12.sp, color = if (expired) appError() else WarningOrange,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "日志", fontSize = 12.sp, color = WarningOrange,
                            modifier = Modifier.border(1.dp, WarningOrange, RoundedCornerShape(14.dp)).clickable {
                                scope.launch {
                                    try {
                                        val resp = ApiClient.service.getAdminUserLogs(user.id)
                                        if (resp.isSuccessful) {
                                            val arr = resp.body()?.getAsJsonArray("data")
                                            if (arr != null) {
                                                logsData = arr.mapNotNull { elem ->
                                                    val obj = elem.asJsonObject
                                                    CheckinLog(entry = obj.get("entry")?.asString ?: "", time = obj.get("time")?.asString ?: "")
                                                }
                                                showLogsDialog = true
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                        if (user.isActive) {
                            Text(
                                "禁用", fontSize = 12.sp, color = appError(),
                                modifier = Modifier.border(1.dp, appError(), RoundedCornerShape(14.dp)).clickable {
                                    scope.launch {
                                        try {
                                            val resp = ApiClient.service.toggleUserActive(user.id, mapOf("isActive" to 0))
                                            if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) { Toast.makeText(context, "已禁用", Toast.LENGTH_SHORT).show(); loadData() }
                                        } catch (_: Exception) {}
                                    }
                                }.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        } else {
                            Text(
                                "启用", fontSize = 12.sp, color = Color(0xFF2E7D32),
                                modifier = Modifier.border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(14.dp)).clickable {
                                    scope.launch {
                                        try {
                                            val resp = ApiClient.service.toggleUserActive(user.id, mapOf("isActive" to 1))
                                            if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) { Toast.makeText(context, "已启用", Toast.LENGTH_SHORT).show(); loadData() }
                                        } catch (_: Exception) {}
                                    }
                                }.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Text(
                            "高级模式", fontSize = 12.sp, color = appPrimary(),
                            modifier = Modifier.border(1.dp, appPrimary(), RoundedCornerShape(14.dp)).clickable {
                                advancedModeUserId = user.id
                                advancedModeStartDate = user.advancedModeStart
                                advancedModeEndDate = user.advancedModeEnd
                                advancedModePreset = ""
                                showAdvancedModeDialog = true
                            }.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                        Text(
                            "删除", fontSize = 12.sp, color = appError(),
                            modifier = Modifier.border(1.dp, appError(), RoundedCornerShape(14.dp)).clickable {
                                pendingDeleteUserId = user.id
                                showDeleteDialog = true
                            }.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }

    OverlayDialog(
        show = showLogsDialog,
        onDismissRequest = { showLogsDialog = false; logsData = emptyList() },
        title = "签到日志",
    ) {
        if (logsData.isEmpty()) Text("暂无日志", fontSize = 15.sp, color = appOnSurfaceVariant())
        else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            logsData.forEach { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(appBackground(), RoundedCornerShape(10.dp)).padding(10.dp)
                ) {
                    Text(log.entry, fontSize = 14.sp, color = appOnSurface(), modifier = Modifier.weight(1f))
                    Text(log.time, fontSize = 13.sp, color = appOnSurfaceVariant())
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppButton(onClick = { showLogsDialog = false; logsData = emptyList() }, modifier = Modifier.fillMaxWidth(), containerColor = Color(0xFFE0E0E0)) { Text("关闭") }
        }
    }

    OverlayDialog(
        show = showDeleteDialog,
        onDismissRequest = { showDeleteDialog = false; pendingDeleteUserId = -1L },
        title = "确认删除",
    ) {
        Text("删除后所有相关数据将被清除，确认删除吗？")
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showDeleteDialog = false; pendingDeleteUserId = -1L })
            AppButton(onClick = {
                scope.launch {
                    try {
                        val resp = ApiClient.service.deleteAdminUser(pendingDeleteUserId)
                        if (resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true) { Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show(); loadData() }
                        else Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) { Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show() }
                    showDeleteDialog = false; pendingDeleteUserId = -1L
                }
            }, modifier = Modifier.fillMaxWidth(), containerColor = appError()) { Text("删除") }
        }
    }

    OverlayDialog(
        show = showAdvancedModeDialog,
        onDismissRequest = { showAdvancedModeDialog = false },
        title = "设置高级模式",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(
                value = advancedModeStartDate,
                onValueChange = { advancedModeStartDate = it },
                labelText = "开始日期 (yyyy-MM-dd)",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("week" to "周", "month" to "月", "year" to "年", "permanent" to "永久").forEach { (key, label) ->
                    AppOutlinedButton(
                        onClick = { advancedModePreset = key },
                        contentColor = if (advancedModePreset == key) appPrimary() else appOnSurfaceVariant()
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
            if (advancedModeEndDate.isNotEmpty()) {
                Text("到期日: $advancedModeEndDate", fontSize = 14.sp, color = appOnSurfaceVariant())
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showAdvancedModeDialog = false })
            if (advancedModeStartDate.isNotEmpty() || advancedModeEndDate.isNotEmpty()) {
                AppTextButton(text = "清除", modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        val err = ApiClient.adminClearAdvancedMode(advancedModeUserId)
                        if (err != null) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        else { Toast.makeText(context, "已清除", Toast.LENGTH_SHORT).show(); loadData() }
                        showAdvancedModeDialog = false
                    }
                })
            }
            AppButton(onClick = {
                scope.launch {
                    val err = ApiClient.adminSetAdvancedMode(advancedModeUserId, advancedModeStartDate, advancedModeEndDate, advancedModePreset)
                    if (err != null) Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    else { Toast.makeText(context, "已设置", Toast.LENGTH_SHORT).show(); loadData() }
                    showAdvancedModeDialog = false
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        }
    }
}

@Composable
private fun AdminAutoCheckinsContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var autoCheckins by remember { mutableStateOf<List<AdminAutoCheckin>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var logsData by remember { mutableStateOf<List<CheckinLog>>(emptyList()) }
    var showLogsDialog by remember { mutableStateOf(false) }

    suspend fun loadData() {
        loading = true
        try {
            autoCheckins = ApiClient.getAdminAutoCheckins()
        } catch (_: Exception) { Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show() }
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).appOverScrollVertical().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!loading && autoCheckins.isEmpty()) {
            Text("暂无自动签到任务", fontSize = 15.sp, color = appOnSurfaceVariant(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
        }

        autoCheckins.forEach { item ->
            GlassCard {
                Column {
                    Text(item.studentName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = appOnSurface())
                    Text("学号：${item.loginName}  预设：${item.presetName}", fontSize = 14.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 6.dp))
                    if (!item.lastStatus.isNullOrEmpty()) {
                        Text("上次状态：${item.lastStatus}", fontSize = 13.sp, color = appOnSurfaceVariant(), modifier = Modifier.padding(top = 4.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        AppButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val resp = ApiClient.service.getAdminUserLogs(item.userId)
                                        if (resp.isSuccessful) {
                                            val arr = resp.body()?.getAsJsonArray("data")
                                            if (arr != null) {
                                                logsData = arr.mapNotNull { elem ->
                                                    val obj = elem.asJsonObject
                                                    CheckinLog(entry = obj.get("entry")?.asString ?: "", time = obj.get("time")?.asString ?: "")
                                                }
                                                showLogsDialog = true
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            },
                            containerColor = WarningOrange
                        ) { Text("日志", fontSize = 13.sp) }
                        AppButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val resp = ApiClient.service.stopAdminCheckin(item.userId)
                                        val ok = resp.isSuccessful && resp.body()?.get("success")?.asBoolean == true
                                        Toast.makeText(context, if (ok) "已停止" else "操作失败", Toast.LENGTH_SHORT).show()
                                        if (ok) loadData()
                                    } catch (_: Exception) { Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            containerColor = appError()
                        ) { Text("停止", fontSize = 13.sp) }
                    }
                }
            }
        }
    }

    OverlayDialog(
        show = showLogsDialog,
        onDismissRequest = { showLogsDialog = false; logsData = emptyList() },
        title = "签到日志",
    ) {
        if (logsData.isEmpty()) Text("暂无日志", fontSize = 15.sp, color = appOnSurfaceVariant())
        else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            logsData.forEach { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(appBackground(), RoundedCornerShape(10.dp)).padding(10.dp)
                ) {
                    Text(log.entry, fontSize = 14.sp, color = appOnSurface(), modifier = Modifier.weight(1f))
                    Text(log.time, fontSize = 13.sp, color = appOnSurfaceVariant())
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppButton(onClick = { showLogsDialog = false; logsData = emptyList() }, modifier = Modifier.fillMaxWidth(), containerColor = Color(0xFFE0E0E0)) { Text("关闭") }
        }
    }
}
