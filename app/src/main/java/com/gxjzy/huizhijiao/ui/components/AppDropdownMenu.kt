package com.gxjzy.huizhijiao.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.RadioButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

typealias AppDropdownMenuScope = AppDropdownMenuScopeImpl

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AppDropdownMenuScope.() -> Unit
) {
    val scope = AppDropdownMenuScopeImpl()
    scope.content()

    var visible by remember { mutableStateOf(false) }
    val fractionProgress = remember { Animatable(0f) }
    val alphaProgress = remember { Animatable(0f) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded) {
        if (expanded) {
            visible = true
            fractionProgress.snapTo(0f)
            alphaProgress.snapTo(0f)
            launch { fractionProgress.animateTo(1f, ListPopupDefaults.FractionAnimationSpec) }
            alphaProgress.animateTo(1f, ListPopupDefaults.AlphaEnterAnimationSpec)
        } else if (visible) {
            launch { fractionProgress.animateTo(0f, ListPopupDefaults.FractionAnimationSpec) }
            alphaProgress.animateTo(0f, ListPopupDefaults.AlphaExitAnimationSpec)
            visible = false
        }
    }

    Box(modifier = modifier.onSizeChanged { anchorHeightPx = it.height }) {
        if (visible) {
            Popup(
                offset = androidx.compose.ui.unit.IntOffset(0, anchorHeightPx),
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true, clippingEnabled = false)
            ) {
                Box(modifier = Modifier.widthIn(min = ListPopupDefaults.MinWidth)) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val fraction = fractionProgress.value
                                val scale = 0.15f + 0.85f * fraction
                                scaleX = scale
                                scaleY = scale
                                alpha = alphaProgress.value
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                    ) {
                        ListPopupColumn {
                            scope.items.forEachIndexed { index, item ->
                                DropdownRadioRow(
                                    text = item.text,
                                    selected = item.selected,
                                    onClick = {
                                        item.onClick()
                                        onDismissRequest()
                                    }
                                )
                                if (index < scope.items.size - 1) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            text = text,
            fontSize = MiuixTheme.textStyles.body1.fontSize,
            fontWeight = FontWeight.Medium,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceContainer,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.radioButtonColors()
        )
    }
}

class AppDropdownMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val textContent: @Composable () -> Unit,
    val iconContent: (@Composable () -> Unit)? = null,
    val selected: Boolean = false,
)

class AppDropdownMenuScopeImpl {
    internal val items = mutableListOf<AppDropdownMenuItem>()

    fun item(
        text: String,
        onClick: () -> Unit,
        textContent: @Composable () -> Unit,
        iconContent: (@Composable () -> Unit)? = null,
        selected: Boolean = false,
    ) {
        items.add(AppDropdownMenuItem(text, onClick, textContent, iconContent, selected))
    }
}
