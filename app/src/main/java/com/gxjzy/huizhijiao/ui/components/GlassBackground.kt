package com.gxjzy.huizhijiao.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import android.net.Uri

object BackgroundImageState {
    var hasCustomImage: Boolean by mutableStateOf(false)
}

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val prefs = BRApp.instance.prefs
    val bgImage by prefs.backgroundImage.distinctUntilChanged().collectAsState(initial = null)
    BackgroundImageState.hasCustomImage = bgImage != null
    Box(modifier = modifier.fillMaxSize()) {
        if (bgImage != null) {
            Image(
                painter = rememberAsyncImagePainter(model = Uri.fromFile(File(bgImage!!))),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.background)
            )
        }
        content()
    }
}
