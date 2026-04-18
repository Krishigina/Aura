package com.aura.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aura.app.MainActivity
import com.aura.app.bridge.buildAttachmentChooserIntent
import com.aura.app.bridge.deliverPickedAttachment
import com.aura.app.bridge.deliverWeatherLocationPermissionResult
import com.aura.app.bridge.requestWeatherLocation
import com.aura.core.navigation.AuraNavigation
import com.aura.core.ui.theme.AuraTheme
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.chat.ChatAttachmentPickerBridge
import com.aura.feature.home.HomeLocationBridge
import com.aura.feature.splash.AuraSplashScreen

@Composable
internal fun MainActivity.AuraAppContent() {
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        deliverPickedAttachment(result)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        deliverWeatherLocationPermissionResult(permissions)
    }

    DisposableEffect(documentPicker) {
        val openHandler = {
            documentPicker.launch(buildAttachmentChooserIntent())
        }
        ChatAttachmentPickerBridge.openPicker = openHandler
        onDispose {
            if (ChatAttachmentPickerBridge.openPicker === openHandler) {
                ChatAttachmentPickerBridge.openPicker = null
            }
        }
    }

    DisposableEffect(locationPermissionLauncher) {
        val requestHandler = {
            requestWeatherLocation(locationPermissionLauncher)
        }
        HomeLocationBridge.requestLocation = requestHandler
        onDispose {
            if (HomeLocationBridge.requestLocation === requestHandler) {
                HomeLocationBridge.requestLocation = null
            }
        }
    }

    AuraTheme {
        var showSplash by remember { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.aura.splash.baseColor),
        ) {
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
