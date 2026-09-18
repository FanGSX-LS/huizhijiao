package com.gxjzy.huizhijiao.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.HorizontalDivider

@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    thickness: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Hairline,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    HorizontalDivider(modifier = modifier)
}
