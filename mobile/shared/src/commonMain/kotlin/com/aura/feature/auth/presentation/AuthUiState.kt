package com.aura.feature.auth.presentation

data class AuthUiState(
    val isLogin: Boolean = true,
    val name: String = "",
    val nickname: String = "@",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isNewUser: Boolean = false,
)
