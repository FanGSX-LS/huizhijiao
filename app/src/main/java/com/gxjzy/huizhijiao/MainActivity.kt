package com.gxjzy.huizhijiao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gxjzy.huizhijiao.navigation.AppNavHost
import com.gxjzy.huizhijiao.ui.theme.BRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = (application as BRApp).prefs
        setContent {
            BRTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
