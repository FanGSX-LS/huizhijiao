package com.gxjzy.huizhijiao.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    checkedColor: androidx.compose.ui.graphics.Color = com.gxjzy.huizhijiao.ui.theme.appPrimary()
) {
    val state = if (checked) ToggleableState.On else ToggleableState.Off
    Checkbox(
        state = state,
        onClick = { onCheckedChange?.invoke(!checked) },
        modifier = modifier
    )
}
