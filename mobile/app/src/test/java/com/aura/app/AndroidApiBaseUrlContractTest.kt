package com.aura.app

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidApiBaseUrlContractTest {

    @Test
    fun androidDefaultApiBaseUrlTargetsHostBackendFromEmulator() {
        val source = readText(
            Path.of("..", "shared", "src", "androidMain", "kotlin", "com", "aura", "core", "platform", "ApiBaseUrl.android.kt")
        )

        assertTrue(source.contains("http://10.0.2.2:3002"))
    }

    @Test
    fun appModuleUsesSharedDefaultApiBaseUrl() {
        val source = readText(
            Path.of("..", "shared", "src", "commonMain", "kotlin", "com", "aura", "core", "di", "AppModule.kt")
        )

        assertTrue(source.contains("defaultApiBaseUrl()"))
        assertTrue(!source.contains("AuraNetworkClients(\"http://10.0.2.2:3002\")"))
    }

    @Test
    fun chatScreenUsesSharedChatComponentsAndLoadingState() {
        val source = readText(
            Path.of("..", "shared", "src", "commonMain", "kotlin", "com", "aura", "feature", "chat", "ChatScreen.kt")
        )

        assertTrue(source.contains("ChatArea("))
        assertTrue(source.contains("ChatInputDock("))
        assertTrue(source.contains("ChatInput("))
        assertTrue(source.contains("isSending = uiState.isLoading"))
    }

    @Test
    fun navigationDoesNotShowCustomComposeSplash() {
        val source = readText(
            Path.of("..", "shared", "src", "commonMain", "kotlin", "com", "aura", "core", "navigation", "Navigation.kt")
        )

        assertTrue(!source.contains("AuraSplashScreen"))
        assertTrue(!source.contains("showSplash"))
    }

    private fun readText(path: Path): String = String(Files.readAllBytes(path))
}
