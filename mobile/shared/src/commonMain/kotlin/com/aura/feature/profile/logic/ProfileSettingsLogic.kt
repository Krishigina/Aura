package com.aura.feature.profile.logic

import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ReminderFrequency

fun validatePasswordChange(currentPassword: String, newPassword: String, confirmPassword: String): String? {
    if (newPassword.isBlank() && confirmPassword.isBlank() && currentPassword.isBlank()) return null
    if (currentPassword.isBlank()) return "Введите текущий пароль"
    if (newPassword.length < 6) return "Новый пароль минимум 6 символов"
    if (newPassword != confirmPassword) return "Новые пароли не совпадают"
    return null
}

fun validateNotificationSettingsBeforeSave(settings: ProfileNotificationSettings): String? {
    if (settings.disable_all) {
        return null
    }

    fun validateDomain(name: String, frequency: ReminderFrequency, weekday: Int?, monthDay: Int?, time: String?): String? {
        when (frequency) {
            ReminderFrequency.NONE -> return null
            ReminderFrequency.DAILY -> Unit
            ReminderFrequency.WEEKLY -> if (weekday == null || weekday !in 1..7) return "Выберите день недели для $name"
            ReminderFrequency.MONTHLY -> if (monthDay == null || monthDay !in 1..31) return "Выберите день месяца для $name"
        }

        if (time.isNullOrBlank()) return "Укажите время для $name"
        val parts = time.split(':')
        if (parts.size != 2) return "Введите время в формате ЧЧ:ММ для $name"
        val hh = parts[0].toIntOrNull() ?: return "Введите время в формате ЧЧ:ММ для $name"
        val mm = parts[1].toIntOrNull() ?: return "Введите время в формате ЧЧ:ММ для $name"
        if (hh !in 0..23 || mm !in 0..59) return "Укажите корректное время для $name"
        return null
    }

    validateDomain("рутины", settings.routine.frequency, settings.routine.weekday, settings.routine.month_day, settings.routine.reminder_time)?.let { return it }
    validateDomain("журнала", settings.journal.frequency, settings.journal.weekday, settings.journal.month_day, settings.journal.reminder_time)?.let { return it }

    return null
}
