package com.aura.feature.auth.mvi

private val EMAIL_REGEX = Regex("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)
private val NICKNAME_REGEX = Regex("^[a-zA-Z0-9_]+$")

private fun normalizeNicknameInput(input: String): String {
    val clean = input.removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }.take(32)
    return "@$clean"
}

fun reduce(state: AuthUiState, intent: AuthIntent): AuthUiState {
    return when (intent) {
        is AuthIntent.SetName -> state.copy(name = intent.value)
        is AuthIntent.SetNickname -> state.copy(nickname = normalizeNicknameInput(intent.value))
        is AuthIntent.SetEmail -> state.copy(email = intent.value)
        is AuthIntent.SetPassword -> state.copy(password = intent.value)
        is AuthIntent.SetConfirmPassword -> state.copy(confirmPassword = intent.value)
        AuthIntent.ToggleMode -> state.copy(
            isLogin = !state.isLogin,
            errorMessage = null,
            nickname = if (state.isLogin && state.nickname.isBlank()) "@" else state.nickname
        )
        AuthIntent.ClearError -> state.copy(errorMessage = null)
        AuthIntent.Submit -> state
    }
}

fun validate(state: AuthUiState): String? {
    if (state.isLogin) {
        if (state.email.isBlank()) return "Введите email"
        if (!EMAIL_REGEX.matches(state.email)) return "Некорректный email"
        if (state.password.length < 6) return "Пароль минимум 6 символов"
        return null
    }

    if (state.name.isBlank()) return "Введите имя"
    if (state.name.length < 2) return "Имя минимум 2 символа"
    if (state.nickname.isBlank() || state.nickname == "@") return "Введите логин"
    val cleanNickname = state.nickname.removePrefix("@")
    if (!NICKNAME_REGEX.matches(cleanNickname)) return "Логин: только латиница, цифры, _"
    if (state.email.isBlank()) return "Введите email"
    if (!EMAIL_REGEX.matches(state.email)) return "Некорректный email"
    if (state.password.length < 6) return "Пароль минимум 6 символов"
    if (state.password != state.confirmPassword) return "Пароли не совпадают"

    return null
}
