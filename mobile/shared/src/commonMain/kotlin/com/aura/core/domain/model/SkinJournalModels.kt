package com.aura.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkinJournalSettings(
    val has_sensor: Boolean? = null,
    val push_enabled: Boolean = false,
    val sensor_reminder_schedule: String? = null,
)

@Serializable
data class SkinJournalReminder(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val due_at: String = "",
    val status: String = "planned",
    val related_id: String? = null,
)

@Serializable
data class SkinJournalZoneAmount(val zone: String, val amount: String)

@Serializable
data class SkinJournalProcedureEntry(
    val id: String = "",
    val catalog_procedure_id: Int = 0,
    val procedure_name: String = "",
    val performed_at: String = "",
    val zones: List<String> = emptyList(),
    val zone_amounts: List<SkinJournalZoneAmount> = emptyList(),
    val preparation_name: String? = null,
    val clinic_or_doctor: String? = null,
    val note: String? = null,
    val repeat_due_at: String? = null,
    val post_care_tasks: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
)

@Serializable
data class SkinJournalSensorReading(
    val id: String = "",
    val measured_at: String = "",
    val zone: String = "",
    val percent_value: Int = 0,
    val hydration: Int = 0,
    val oiliness: Int = 0,
    val softness: Int = 0,
)

@Serializable
data class SkinJournalResponse(
    val settings: SkinJournalSettings = SkinJournalSettings(),
    val procedures: List<SkinJournalProcedureEntry> = emptyList(),
    val sensor_readings: List<SkinJournalSensorReading> = emptyList(),
    val reminders: List<SkinJournalReminder> = emptyList(),
)

@Serializable
data class SkinJournalSettingsUpdate(
    val has_sensor: Boolean? = null,
    val push_enabled: Boolean? = null,
    val sensor_reminder_schedule: String? = null,
)

@Serializable
data class SkinJournalProcedureCreate(
    val catalog_procedure_id: Int,
    val procedure_name: String,
    val performed_at: String,
    val zones: List<String>,
    val zone_amounts: List<SkinJournalZoneAmount> = emptyList(),
    val preparation_name: String? = null,
    val clinic_or_doctor: String? = null,
    val note: String? = null,
    val repeat_due_at: String? = null,
    val post_care_tasks: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
)

@Serializable
data class SkinJournalSensorReadingCreate(
    val measured_at: String,
    val zone: String,
    val percent_value: Int,
    val hydration: Int,
    val oiliness: Int,
    val softness: Int,
)

@Serializable
data class SkinJournalReminderAction(
    val action: String,
    val rescheduled_due_at: String? = null,
)

@Serializable
enum class ReminderFrequency {
    @SerialName("daily")
    DAILY,

    @SerialName("weekly")
    WEEKLY,

    @SerialName("monthly")
    MONTHLY,

    @SerialName("none")
    NONE,
}

@Serializable
data class ProcedureCatalogItem(
    val id: Int = 0,
    val name: String = "",
    val recommended_course: String? = null,
    val rehabilitation: String? = null,
)
