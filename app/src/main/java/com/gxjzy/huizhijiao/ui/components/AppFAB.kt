package com.gxjzy.huizhijiao.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.FloatingActionButton

@Composable
fun AppSmallFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = com.gxjzy.huizhijiao.ui.theme.appPrimary(),
    contentColor: androidx.compose.ui.graphics.Color = com.gxjzy.huizhijiao.ui.theme.appOnPrimary(),
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
    ) { content() }
}
