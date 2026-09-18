package com.gxjzy.huizhijiao.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gxjzy.huizhijiao.ui.components.BackgroundImageState

@Composable
fun appPrimary(): Color = mc.primary
@Composable
fun appOnPrimary(): Color = mc.onPrimary
@Composable
fun appPrimaryContainer(): Color = mc.primaryContainer
@Composable
fun appOnPrimaryContainer(): Color = mc.onPrimaryContainer
@Composable
fun appSecondary(): Color = mc.secondary
@Composable
fun appOnSecondary(): Color = mc.onSecondary
@Composable
fun appSecondaryContainer(): Color = mc.secondaryContainer
@Composable
fun appBackground(): Color = if (BackgroundImageState.hasCustomImage) Color.Transparent else PageBackground
@Composable
fun appOnBackground(): Color = mc.onBackground
@Composable
fun appSurface(): Color = Color.White
@Composable
fun appOnSurface(): Color = mc.onSurface
@Composable
fun appSurfaceVariant(): Color = mc.surfaceVariant
@Composable
fun appOnSurfaceVariant(): Color = mc.onBackgroundVariant
@Composable
fun appSurfaceContainerLow(): Color = mc.surfaceContainer
@Composable
fun appSurfaceContainerHigh(): Color = mc.surfaceContainerHigh
@Composable
fun appOutline(): Color = mc.outline
@Composable
fun appOutlineVariant(): Color = mc.dividerLine
@Composable
fun appError(): Color = mc.error

private val mc: top.yukonga.miuix.kmp.theme.Colors
    @Composable get() = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
