package com.aura.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.aura.core.di.appModules
import com.aura.core.ui.theme.AuraTheme
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.splash.AuraSplashScreen
import org.koin.compose.KoinApplication
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    AppState.isNavigationReady = false
    return ComposeUIViewController {
        KoinApplication(application = { modules(appModules) }) {
            AuraTheme {
                var showSplash by remember { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AuraNavigation()
                    }
                    if (showSplash) {
                        AuraSplashScreen(
                            isAppReady = AppState.isNavigationReady,
                            isWarmStart = false,
                            onSplashFinished = { showSplash = false },
                        )
                    }
                }
            }
        }
    }
}
