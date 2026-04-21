package com.aura.feature.auth.mvi

import com.aura.core.data.api.AuraApiClient
import com.aura.core.data.repository.TokenManager
import com.aura.core.presentation.mvi.MviStore

class AuthStore(private val apiClient: AuraApiClient) : MviStore<AuthUiState, AuthIntent>(AuthUiState()) {
    override fun reduce(state: AuthUiState, intent: AuthIntent): AuthUiState = com.aura.feature.auth.mvi.reduce(state, intent)

    suspend fun submit(): AuthEffect {
        val validationError = validate(state)
        if (validationError != null) {
            setState(state.copy(errorMessage = validationError))
            return AuthEffect.ShowError(validationError)
        }

        setState(state.copy(isLoading = true, errorMessage = null))

        return runCatching {
            val response = if (state.isLogin) {
                apiClient.login(state.email, state.password)
            } else {
                val cleanNickname = state.nickname.removePrefix("@").trim()
                apiClient.register(
                    name = state.name.trim(),
                    email = state.email.trim(),
                    password = state.password,
                    nickname = cleanNickname
                )
            }

            TokenManager.setToken(response.access_token)
            TokenManager.setUser(response.user)
            val isNewUser = !state.isLogin
            setState(state.copy(isLoading = false))
            AuthEffect.AuthSucceeded(isNewUser)
        }.getOrElse { e ->
            val safeMessage = toUserSafeMessage(e)
            println(
                "AUTH_ERROR submit failed: isLogin=${state.isLogin}, email=${state.email}, " +
                    "errorClass=${e::class.simpleName}, errorMessage=${e.message}"
            )
            setState(state.copy(isLoading = false, errorMessage = safeMessage))
            AuthEffect.ShowError(safeMessage)
        }
    }

    private fun toUserSafeMessage(error: Throwable): String {
        val raw = (error.message ?: "").trim()
        val low = raw.lowercase()

        if (low.isBlank()) {
            return "Не удалось выполнить операцию. Попробуйте позже."
        }

        val technicalMarkers = listOf("url=", "connect timeout", "unknown ms", "http://", "https://", "io.ktor")
        if (technicalMarkers.any { low.contains(it) }) {
            return "Не удалось подключиться к серверу. Проверьте интернет и повторите попытку."
        }

        if (low.contains("timeout")) {
            return "Сервер отвечает слишком долго. Попробуйте ещё раз."
        }

        return raw
    }
}
