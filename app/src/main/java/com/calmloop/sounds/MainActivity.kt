package com.calmloop.sounds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calmloop.sounds.ads.AdsManager
import com.calmloop.sounds.ui.MainScreen
import com.calmloop.sounds.ui.theme.CalmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        AdsManager.gatherConsent(this)
        setContent {
            CalmTheme {
                MainScreen(activity = this)
            }
        }
    }
}
