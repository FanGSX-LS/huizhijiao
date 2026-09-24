package com.gxjzy.huizhijiao.ui.components

import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class AmapBridge(
    private val onMapReady: (() -> Unit)? = null,
    private val onLocationPicked: ((lng: Double, lat: Double, address: String) -> Unit)? = null
) {
    @JavascriptInterface
    fun onMapReady() {
        Log.d("AmapView", "map ready")
        onMapReady?.invoke()
    }

    @JavascriptInterface
    fun onLocationPicked(lng: String, lat: String, address: String) {
        onLocationPicked?.invoke(lng.toDoubleOrNull() ?: 0.0, lat.toDoubleOrNull() ?: 0.0, address)
    }

    @JavascriptInterface
    fun onLog(msg: String) {
        Log.d("AmapView", "JS: $msg")
    }
}

private fun amapStyleFor(isDark: Boolean): String =
    if (isDark) "amap://styles/dark" else "amap://styles/normal"

private fun bgColorFor(isDark: Boolean): String =
    if (isDark) "#1a1a2e" else "#f2f3f7"

class AmapWebHolder(
    context: Context,
    private val bridge: AmapBridge,
    private val html: String,
    bgColor: String
) : FrameLayout(context) {

    val webView: WebView = WebView(context).apply {
        WebView.setWebContentsDebuggingEnabled(true)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("AmapView", "console: ${it.message()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }
        addJavascriptInterface(bridge, "Android")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.postDelayed({
                    view.evaluateJavascript("typeof initMap === 'function' && !map && initMap()", null)
                }, 300)
            }
        }
    }

    private var loaded = false

    init {
        setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        addView(
            webView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!loaded && w > 0 && h > 0) {
            loaded = true
            webView.loadDataWithBaseURL(
                "https://webapi.amap.com/",
                html,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    fun applyMapStyle(isDark: Boolean) {
        webView.evaluateJavascript("typeof setMapStyle === 'function' && setMapStyle('${amapStyleFor(isDark)}')", null)
    }
}

@Composable
fun AmapView(
    longitude: Double,
    latitude: Double,
    label: String = "",
    mode: String = "show",
    onLocationPicked: ((lng: Double, lat: Double, address: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var mapReady by remember { mutableStateOf(false) }
    var lastLng by remember { mutableStateOf(0.0) }
    var lastLat by remember { mutableStateOf(0.0) }
    var lastLabel by remember { mutableStateOf("") }
    var lastDark by remember { mutableStateOf(isDark) }
    val bridge = remember(onLocationPicked) {
        AmapBridge(
            onMapReady = { mapReady = true },
            onLocationPicked = onLocationPicked
        )
    }

    AndroidView(
        factory = { ctx ->
            val bg = bgColorFor(isDark)
            val pageHtml = ctx.assets.open("amap_map.html")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .replace("{{PICK_MODE}}", (mode == "pick").toString())
                .replace("{{MAP_STYLE}}", amapStyleFor(isDark))
                .replace("{{BG_COLOR}}", bg)
            AmapWebHolder(ctx, bridge, pageHtml, bg)
        },
        update = { holder ->
            if (isDark != lastDark) {
                lastDark = isDark
                holder.applyMapStyle(isDark)
            }
            if (mapReady && (longitude != 0.0 || latitude != 0.0)) {
                if (longitude != lastLng || latitude != lastLat || label != lastLabel) {
                    lastLng = longitude
                    lastLat = latitude
                    lastLabel = label
                    val escapedLabel = label.replace("'", "\\'").replace("\n", " ").replace("\r", "")
                    holder.webView.evaluateJavascript("setMarker($longitude,$latitude,'$escapedLabel')", null)
                }
            }
        },
        modifier = modifier
    )
}
