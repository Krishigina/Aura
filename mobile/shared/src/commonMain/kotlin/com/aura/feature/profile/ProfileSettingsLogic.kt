package com.aura.feature.profile

fun validatePasswordChange(currentPassword: String, newPassword: String, confirmPassword: String): String? {
    if (newPassword.isBlank() && confirmPassword.isBlank() && currentPassword.isBlank()) return null
    if (currentPassword.isBlank()) return "Введите текущий пароль"
    if (newPassword.length < 6) return "Новый пароль минимум 6 символов"
    if (newPassword != confirmPassword) return "Новые пароли не совпадают"
    return null
}
