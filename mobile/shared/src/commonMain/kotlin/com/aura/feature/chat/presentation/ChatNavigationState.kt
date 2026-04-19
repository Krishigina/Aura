package com.aura.feature.chat.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonObject

class ChatNavigationState {
    var activeSessionId: Int? by mutableStateOf(null)
        private set

    private var pendingProductContext: JsonObject? by mutableStateOf(null)
    var productContextRequestKey: Int by mutableStateOf(0)
        private set

    fun setActiveSession(sessionId: Int?) {
        activeSessionId = sessionId
    }

    fun clearActiveSession() {
        activeSessionId = null
    }

    fun setProductContext(value: JsonObject?) {
        pendingProductContext = value
    }

    fun startProductChat(value: JsonObject?) {
        activeSessionId = null
        pendingProductContext = value
        productContextRequestKey++
    }

    fun consumeProductContext(): JsonObject? {
        val value = pendingProductContext
        pendingProductContext = null
        return value
    }
}
