package com.gxjzy.huizhijiao.ui.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.ui.components.AppAlertDialog
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.AppSwitch
import com.gxjzy.huizhijiao.ui.components.AppTextButton
import com.gxjzy.huizhijiao.ui.components.AppTopAppBar
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.ui.theme.WarningOrange
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit = {}) {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()

    val advancedMode by prefs.advancedMode.distinctUntilChanged().collectAsState(initial = false)
    val advancedRevealed by prefs.advancedModeRevealed.collectAsState(initial = false)
    val bgImage by prefs.backgroundImage.collectAsState(initial = null)


    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAdvancedDialog by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        scope.launch {
            if (uri != null) {
                try {
                    val inputStream = BRApp.instance.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val destFile = java.io.File(BRApp.instance.filesDir, "bg_wallpaper")
                        destFile.outputStream().use { out -> inputStream.copyTo(out) }
                        inputStream.close()
                        prefs.saveBackgroundImage(destFile.absolutePath)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    AppScaffold(
        topBar = {
            AppTopAppBar(
                title = "设置",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().background(appBackground()).padding(innerPadding).verticalScroll(rememberScrollState()).appOverScrollVertical().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("背景图", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (bgImage != null) {
                                AppTextButton(text = "清除", onClick = { scope.launch { prefs.saveBackgroundImage(null) } })
                            }
                        }
                        if (bgImage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("已设置背景图", fontSize = 14.sp, color = Color(0xFF2E7D32))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AppButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("选择背景图") }
                    }
                }

                if (advancedRevealed || advancedMode) {
                    GlassCard {
                        Row(
                    modifier = Modifier.fillMaxWidth().clickable(indication = null, interactionSource = null) {
                                if (!advancedMode) showAdvancedDialog = true
                                else scope.launch { prefs.saveAdvancedMode(false) }
                            }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("高级模式", fontSize = 17.sp, color = appOnSurface())
                                Text("开启后显示自动签到、自动填充、异常/出差标记等实验性功能", fontSize = 14.sp, color = appOnSurfaceVariant())
                            }
                            AppSwitch(checked = advancedMode, onCheckedChange = {
                                if (it) showAdvancedDialog = true
                                else scope.launch { prefs.saveAdvancedMode(false) }
                            }, checkedTrackColor = WarningOrange)
                        }
                    }
                }

                AppButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = appError()
                ) { Text("退出登录", fontSize = 17.sp) }

                Text(
                    "2.2",
                    fontSize = 14.sp, color = Color(0xFFCCCCCC),
                    modifier = Modifier.fillMaxWidth().clickable(indication = null, interactionSource = null) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 500) tapCount++ else tapCount = 1
                        lastTapTime = now
                        if (tapCount >= 10) {
                            scope.launch { prefs.saveAdvancedModeRevealed(true) }
                            tapCount = 0
                        }
                    }.wrapContentSize(Alignment.Center).padding(top = 24.dp, bottom = 16.dp)
                )
             }

        AppAlertDialog(
            show = showLogoutDialog,
            onDismissRequest = { showLogoutDialog = false },
            title = "退出登录",
            text = { Text("退出后将清除登录凭证及所有账号相关缓存，下次需重新登录。确认退出？") },
            confirmButton = {
                AppButton(onClick = {
                    scope.launch {
                        prefs.clearCredentials()
                        com.gxjzy.huizhijiao.api.ApiClient.setToken(null)
                        com.gxjzy.huizhijiao.api.ApiClient.setCachedUser(null)
                        com.gxjzy.huizhijiao.data.AppDataCache.clear()
                        com.gxjzy.huizhijiao.ui.tabs.ManageDataCache.clear()
                        showLogoutDialog = false
                        onLogout()
                    }
                }, modifier = Modifier.fillMaxWidth(), containerColor = appError()) { Text("确认退出") }
            },
            dismissButton = { AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showLogoutDialog = false }) }
        )

        AppAlertDialog(
            show = showAdvancedDialog,
            onDismissRequest = { showAdvancedDialog = false },
            title = "开启高级模式",
            text = {
                val user = ApiClient.getCachedUser()
                if (user != null && user.advancedModeExpired) {
                    Text("您的高级模式已于 ${user.advancedModeEnd.ifEmpty { "未知日期" }} 到期，请联系管理员续期")
                } else {
                    Text("高级模式包含自动签到、自动填充、异常/出差标记等实验性功能，可能存在风险。确认开启？")
                }
            },
            confirmButton = {
                val user = ApiClient.getCachedUser()
                if (user != null && user.advancedModeExpired) {
                    AppButton(onClick = { showAdvancedDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("确定") }
                } else {
                    AppButton(onClick = {
                        scope.launch { prefs.saveAdvancedMode(true) }
                        showAdvancedDialog = false
                    }, modifier = Modifier.fillMaxWidth(), containerColor = WarningOrange, contentColor = Color.White) { Text("确认开启") }
                }
            },
            dismissButton = { AppTextButton(text = "取消", modifier = Modifier.fillMaxWidth(), onClick = { showAdvancedDialog = false }) }
        )
    }
}
