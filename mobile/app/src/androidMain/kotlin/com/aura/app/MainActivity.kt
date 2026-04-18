package com.aura.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aura.app.notifications.configureAndroidNotifications
import com.aura.app.systemui.configureEdgeToEdgeWindow
import com.aura.app.systemui.hideSystemBars
import com.aura.app.ui.AuraAppContent
import com.aura.core.ui.theme.runtime.AppState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppState.isNavigationReady = false
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureAndroidNotifications()
        configureEdgeToEdgeWindow()
        hideSystemBars()
        setContent {
            AuraAppContent()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }
}
