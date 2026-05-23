package com.aura.feature.profile

import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ReminderFrequency

fun validatePasswordChange(currentPassword: String, newPassword: String, confirmPassword: String): String? {
    if (newPassword.isBlank() && confirmPassword.isBlank() && currentPassword.isBlank()) return null
    if (currentPassword.isBlank()) return "Введите текущий пароль"
    if (newPassword.length < 6) return "Новый пароль минимум 6 символов"
    if (newPassword != confirmPassword) return "Новые пароли не совпадают"
    return null
}

fun validateNotificationSettingsBeforeSave(settings: ProfileNotificationSettings, routineStepsCount: Int): String? {
    if (settings.routine.frequency != ReminderFrequency.NONE && routineStepsCount == 0) {
        return "Создайте шаги рутины, чтобы получать напоминания"
    }
    return null
}
