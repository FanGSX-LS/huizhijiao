package com.gxjzy.huizhijiao

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BRApp : Application() {

    lateinit var prefs: PreferencesManager
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesManager(this)
        initPredictiveBackGesture()
        appScope.launch { restoreApiClientState() }
    }

    private fun initPredictiveBackGesture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                val method = ApplicationInfo::class.java.getDeclaredMethod(
                    "setEnableOnBackInvokedCallback",
                    Boolean::class.javaPrimitiveType
                )
                method.isAccessible = true
                method.invoke(applicationInfo, true)
            }
        }
    }

    private suspend fun restoreApiClientState() {
        val url = prefs.apiBaseUrl.first()
        if (!url.isNullOrEmpty()) ApiClient.setBaseUrl(url)
        val t = prefs.token.first()
        if (!t.isNullOrEmpty()) ApiClient.setToken(t)
    }

    companion object {
        lateinit var instance: BRApp
            private set
    }
}
