package com.gxjzy.huizhijiao.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun AppAlertDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        content = {
            if (text != null) {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    text()
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dismissButton != null) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        dismissButton()
                    }
                }
                Box(modifier = if (dismissButton != null) Modifier.weight(1f) else Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    confirmButton()
                }
            }
        }
    )
}
