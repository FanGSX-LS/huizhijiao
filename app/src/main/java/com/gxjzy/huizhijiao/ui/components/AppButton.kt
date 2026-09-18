package com.gxjzy.huizhijiao.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import com.gxjzy.huizhijiao.ui.theme.appPrimary
import com.gxjzy.huizhijiao.ui.theme.appOnPrimary

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = appPrimary(),
    contentColor: Color = appOnPrimary(),
    minHeight: Dp = 44.dp,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minWidth = 1.dp,
        minHeight = minHeight,
        colors = ButtonDefaults.buttonColors(
            color = containerColor,
            contentColor = contentColor,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = appPrimary()
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(textColor = contentColor),
    )
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = appPrimary(),
    minHeight: Dp = 44.dp,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minWidth = 1.dp,
        minHeight = minHeight,
        colors = ButtonDefaults.buttonColors(
            color = containerColor,
            contentColor = contentColor,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}
