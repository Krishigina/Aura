package com.aura.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRoutineStep(
    val id: String = "",
    val product_label: String = "",
    val order: Int = 0,
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val weekday: Int? = null,
    val month_day: Int? = null,
    val reminder_time: String? = null,
)

@Serializable
data class ProfileRoutineResponse(
    val steps: List<ProfileRoutineStep> = emptyList(),
)

@Serializable
data class ProfileRoutineUpdateRequest(
    val steps: List<ProfileRoutineStep> = emptyList(),
)

@Serializable
data class ReminderPreference(
    val frequency: ReminderFrequency = ReminderFrequency.NONE,
    val weekday: Int? = null,
    val month_day: Int? = null,
    val reminder_time: String? = null,
)

@Serializable
data class ProfileNotificationSettings(
    val disable_all: Boolean = false,
    val routine: ReminderPreference = ReminderPreference(),
    val journal: ReminderPreference = ReminderPreference(),
)
