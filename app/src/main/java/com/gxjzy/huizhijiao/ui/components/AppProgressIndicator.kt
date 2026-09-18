package com.gxjzy.huizhijiao.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gxjzy.huizhijiao.ui.theme.appPrimary
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = appPrimary(),
    strokeWidth: Dp = 2.dp
) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
fun AppLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = appPrimary(),
    trackColor: Color = Color(0xFFE0E0E0)
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
    )
}
