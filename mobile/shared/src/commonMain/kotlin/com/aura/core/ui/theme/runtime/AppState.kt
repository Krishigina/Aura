package com.aura.core.ui.theme.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppState {
    var isDarkMode by mutableStateOf(false)
    var isNavigationReady by mutableStateOf(false)
}
