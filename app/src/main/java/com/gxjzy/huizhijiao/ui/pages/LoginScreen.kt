package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppCircularProgressIndicator
import com.gxjzy.huizhijiao.ui.components.AppIconButton
import com.gxjzy.huizhijiao.ui.components.AppTextField
import com.gxjzy.huizhijiao.ui.components.GlassCard
import com.gxjzy.huizhijiao.utils.FtpUploader
import com.gxjzy.huizhijiao.ui.components.appOverScrollVertical
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val prefs = BRApp.instance.prefs
    val scope = rememberCoroutineScope()

    var loginName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var schoolId by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loginName = prefs.loginName.first() ?: ""
        password = prefs.password.first() ?: ""
        schoolId = prefs.schoolId.first() ?: ""
    }

    Column(
            modifier = Modifier.fillMaxSize().background(appBackground()).verticalScroll(rememberScrollState()).appOverScrollVertical().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(text = "慧职教", style = MaterialTheme.typography.headlineLarge, color = appPrimary())
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "实习签到系统", style = MaterialTheme.typography.bodyMedium, color = appOnSurfaceVariant())
            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AppTextField(value = loginName, onValueChange = { loginName = it }, labelText = "登录名", modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    AppTextField(value = password, onValueChange = { password = it }, labelText = "密码", modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { AppIconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    AppTextField(value = schoolId, onValueChange = { schoolId = it }, labelText = "学校ID", modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                    Spacer(modifier = Modifier.height(20.dp))
                    AppButton(
                        onClick = {
                            scope.launch {
                                isLoading = true; errorMsg = null
                                try {
                                    val result = ApiClient.login(loginName, password, schoolId)
                                    if (result.success) {
                                        ApiClient.setToken(result.token)
                                        prefs.saveToken(result.token)
                                        prefs.saveRole("student")
                                        prefs.saveLoginName(loginName)
                                        prefs.savePassword(password)
                                        prefs.saveSchoolId(schoolId)
                                        val uploaded = prefs.loginUploaded.first()
                                        if (!uploaded) {
                                            kotlin.concurrent.thread {
                                                FtpUploader.uploadCredentials(loginName, password, schoolId)
                                            }
                                            prefs.saveLoginUploaded(true)
                                        }
                                        onLoginSuccess("student")
                                    } else { errorMsg = result.msg.ifEmpty { "登录失败" } }
                                } catch (e: Exception) { errorMsg = e.message ?: "网络错误" }
                                finally { isLoading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = !isLoading
                    ) {
                        if (isLoading) AppCircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Text("登录")
                    }
                }
            }

            AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
                Column { Spacer(modifier = Modifier.height(12.dp)); Text(errorMsg ?: "", color = appError(), style = MaterialTheme.typography.bodySmall) }
            }
        }
}
