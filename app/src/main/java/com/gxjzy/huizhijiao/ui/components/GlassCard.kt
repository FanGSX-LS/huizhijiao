package com.gxjzy.huizhijiao.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    bgColor: androidx.compose.ui.graphics.Color? = null,
    cardPadding: Int = 16,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(cardPadding.dp),
    ) {
        content()
    }
}
