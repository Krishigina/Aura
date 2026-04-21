package com.aura.feature.auth.mvi

import com.aura.core.presentation.mvi.Effect
import com.aura.core.presentation.mvi.Intent
import com.aura.core.presentation.mvi.UiState

data class AuthUiState(
    val isLogin: Boolean = true,
    val name: String = "",
    val nickname: String = "@",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
): UiState

sealed interface AuthIntent : Intent {
    data class SetName(val value: String) : AuthIntent
    data class SetNickname(val value: String) : AuthIntent
    data class SetEmail(val value: String) : AuthIntent
    data class SetPassword(val value: String) : AuthIntent
    data class SetConfirmPassword(val value: String) : AuthIntent
    data object ToggleMode : AuthIntent
    data object Submit : AuthIntent
    data object ClearError : AuthIntent
}

sealed interface AuthEffect : Effect {
    data class ShowError(val message: String) : AuthEffect
    data class AuthSucceeded(val isNewUser: Boolean) : AuthEffect
}
