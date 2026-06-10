package com.aura.feature.profile

import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ReminderFrequency
import com.aura.feature.profile.logic.validateNotificationSettingsBeforeSave
import com.aura.feature.profile.logic.validatePasswordChange
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileSettingsLogicTest {

    @Test
    fun passwordChangeValidationRequiresCurrentPassword() {
        val error = validatePasswordChange(
            currentPassword = "",
            newPassword = "newpass1",
            confirmPassword = "newpass1",
        )

        assertEquals("Введите текущий пароль", error)
    }

    @Test
    fun passwordChangeValidationReturnsNullWhenPasswordChangeNotRequested() {
        val error = validatePasswordChange(
            currentPassword = "",
            newPassword = "",
            confirmPassword = "",
        )

        assertEquals(null, error)
    }

    @Test
    fun notificationValidationAllowsRoutineReminderWithoutRoutineSteps() {
        val error = validateNotificationSettingsBeforeSave(
            settings = ProfileNotificationSettings(
                routine = com.aura.core.domain.model.ReminderPreference(
                    frequency = ReminderFrequency.DAILY,
                    reminder_time = "09:00",
                ),
            ),
        )

        assertEquals(null, error)
    }

    @Test
    fun notificationValidationAllowsSaveWhenRoutineReminderDisabled() {
        val error = validateNotificationSettingsBeforeSave(
            settings = ProfileNotificationSettings(
                routine = com.aura.core.domain.model.ReminderPreference(
                    frequency = ReminderFrequency.NONE,
                    reminder_time = null,
                ),
            ),
        )

        assertEquals(null, error)
    }

    @Test
    fun notificationValidationAllowsSaveWhenAllNotificationsDisabled() {
        val error = validateNotificationSettingsBeforeSave(
            settings = ProfileNotificationSettings(
                disable_all = true,
                routine = com.aura.core.domain.model.ReminderPreference(
                    frequency = ReminderFrequency.DAILY,
                    reminder_time = "09:00",
                ),
            ),
        )

        assertEquals(null, error)
    }

    @Test
    fun notificationValidationRequiresWeekdayForWeekly() {
        val error = validateNotificationSettingsBeforeSave(
            settings = ProfileNotificationSettings(
                routine = com.aura.core.domain.model.ReminderPreference(
                    frequency = ReminderFrequency.WEEKLY,
                    weekday = null,
                    reminder_time = "09:00",
                ),
            ),
        )

        assertEquals("Выберите день недели для рутины", error)
    }

    @Test
    fun notificationValidationRequiresTimeFormat() {
        val error = validateNotificationSettingsBeforeSave(
            settings = ProfileNotificationSettings(
                routine = com.aura.core.domain.model.ReminderPreference(
                    frequency = ReminderFrequency.DAILY,
                    reminder_time = "99:77",
                ),
            ),
        )

        assertEquals("Укажите корректное время для рутины", error)
    }
}
