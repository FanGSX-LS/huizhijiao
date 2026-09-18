package com.gxjzy.huizhijiao.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled

@Composable
fun BRTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val mode = if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light
    val controller = remember { ThemeController(mode) }
    CompositionLocalProvider(LocalSquircleEnabled provides true) {
        MiuixTheme(
            controller = controller,
            content = {
                MaterialTheme(
                    typography = AppTypography,
                    content = content
                )
            }
        )
    }
}
