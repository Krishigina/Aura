package com.aura.feature.journal

import com.aura.core.domain.model.SkinJournalProcedureEntry
import com.aura.core.domain.model.SkinJournalReminder
import com.aura.core.domain.model.SkinJournalSensorReading
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun activeTodayReminders(
    reminders: List<SkinJournalReminder>,
    now: Instant = Clock.System.now(),
): List<SkinJournalReminder> {
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return reminders
        .filter { it.status == "planned" || it.status == "rescheduled" }
        .filter { reminder ->
            runCatching {
                Instant.parse(reminder.due_at)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date <= today
            }.getOrDefault(false)
        }
}

fun activeSkinJournalReminders(reminders: List<SkinJournalReminder>): List<SkinJournalReminder> =
    activeTodayReminders(reminders)

fun skinJournalTodayStatus(activeReminders: List<SkinJournalReminder>): String = when (activeReminders.size) {
    0 -> "Сегодня все спокойно"
    1 -> activeReminders.first().title
    else -> "Активных напоминаний: ${activeReminders.size}"
}

fun tomorrowJournalTimestamp(): String =
    Clock.System.now().plus(1, DateTimeUnit.DAY, TimeZone.UTC).toString()

fun sixMonthsJournalTimestamp(): String =
    Clock.System.now().plus(182, DateTimeUnit.DAY, TimeZone.UTC).toString()

data class SkinJournalDayEvents(
    val procedureCount: Int = 0,
    val sensorCount: Int = 0,
)

data class SkinJournalMonthEvents(
    val procedureCount: Int = 0,
    val sensorCount: Int = 0,
)

data class SkinJournalDateEntries(
    val procedures: List<SkinJournalProcedureEntry> = emptyList(),
    val sensorReadings: List<SkinJournalSensorReading> = emptyList(),
)

fun latestSkinJournalProcedures(
    procedures: List<SkinJournalProcedureEntry>,
    limit: Int = 5,
): List<SkinJournalProcedureEntry> = procedures
    .sortedByDescending { parseJournalInstantOrNull(it.performed_at) }
    .take(limit)

fun journalDateKey(timestamp: String): String? = runCatching {
    Instant.parse(timestamp).toString().substring(0, 10)
}.getOrNull()

fun skinJournalEventDays(
    procedures: List<SkinJournalProcedureEntry>,
    readings: List<SkinJournalSensorReading>,
): Map<String, SkinJournalDayEvents> {
    val result = mutableMapOf<String, SkinJournalDayEvents>()
    procedures.forEach { procedure ->
        val key = journalDateKey(procedure.performed_at) ?: return@forEach
        val current = result[key] ?: SkinJournalDayEvents()
        result[key] = current.copy(procedureCount = current.procedureCount + 1)
    }
    readings.forEach { reading ->
        val key = journalDateKey(reading.measured_at) ?: return@forEach
        val current = result[key] ?: SkinJournalDayEvents()
        result[key] = current.copy(sensorCount = current.sensorCount + 1)
    }
    return result
}

fun skinJournalEventMonths(
    procedures: List<SkinJournalProcedureEntry>,
    readings: List<SkinJournalSensorReading>,
): Map<String, SkinJournalMonthEvents> {
    val result = mutableMapOf<String, SkinJournalMonthEvents>()
    skinJournalEventDays(procedures, readings).forEach { (dateKey, events) ->
        val monthKey = dateKey.take(7)
        val current = result[monthKey] ?: SkinJournalMonthEvents()
        result[monthKey] = current.copy(
            procedureCount = current.procedureCount + events.procedureCount,
            sensorCount = current.sensorCount + events.sensorCount,
        )
    }
    return result
}

fun firstJournalEventDateInMonth(
    year: Int,
    month: Int,
    eventDays: Map<String, SkinJournalDayEvents>,
): String? {
    val monthKey = "$year-${month.pad2()}"
    return eventDays.keys
        .filter { it.startsWith(monthKey) }
        .sorted()
        .firstOrNull()
}

fun selectedJournalMonthDate(
    year: Int,
    month: Int,
    eventDays: Map<String, SkinJournalDayEvents>,
): String = firstJournalEventDateInMonth(year, month, eventDays) ?: "$year-${month.pad2()}-01"

fun entriesForJournalDate(
    dateKey: String,
    procedures: List<SkinJournalProcedureEntry>,
    readings: List<SkinJournalSensorReading>,
): SkinJournalDateEntries = SkinJournalDateEntries(
    procedures = procedures.filter { journalDateKey(it.performed_at) == dateKey },
    sensorReadings = readings.filter { journalDateKey(it.measured_at) == dateKey },
)

fun displayJournalDate(timestamp: String): String =
    (journalDateKey(timestamp) ?: timestamp.take(10)).isoDateToDisplayDate()

fun String.toJournalDateTimestampOrNull(): String? {
    val trimmed = trim()
    if (!Regex("\\d{2}\\.\\d{2}\\.\\d{4}").matches(trimmed)) return null
    val day = trimmed.substring(0, 2).toIntOrNull() ?: return null
    val month = trimmed.substring(3, 5).toIntOrNull() ?: return null
    val year = trimmed.substring(6, 10).toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..journalDaysInMonth(year, month)) return null
    return "$year-${month.pad2()}-${day.pad2()}T12:00:00Z"
}

fun String.isoDateToDisplayDate(): String {
    val year = take(4)
    val month = substringOrNull(5, 7) ?: return this
    val day = substringOrNull(8, 10) ?: return this
    return "$day.$month.$year"
}

fun shiftJournalMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val zeroBased = year * 12 + (month - 1) + delta
    return (zeroBased / 12) to (zeroBased % 12 + 1)
}

fun moisturePercentInterpretation(percent: Int): String = when {
    percent <= 33 -> "Очень сухая"
    percent <= 37 -> "Сухая"
    percent <= 42 -> "Нормальная"
    percent <= 46 -> "Влажная"
    else -> "Очень влажная"
}

fun oilinessScaleInterpretation(value: Int): String = when (value.coerceIn(1, 5)) {
    1 -> "Очень сухая"
    2 -> "Сухая"
    3 -> "Нормальная"
    4 -> "Жирная"
    else -> "Очень жирная"
}

fun hydrationScaleInterpretation(value: Int): String = when (value.coerceIn(1, 5)) {
    1 -> "Очень сухая"
    2 -> "Сухая"
    3 -> "Нормальная"
    4 -> "Влажная"
    else -> "Очень влажная"
}

fun softnessScaleInterpretation(value: Int): String = when (value.coerceIn(1, 5)) {
    1 -> "Очень шершавая"
    2 -> "Шершавая"
    3 -> "Нормальная"
    4 -> "Мягкая"
    else -> "Очень мягкая"
}

fun Int.procedureCountText(): String = "$this ${russianPlural(this, "процедура", "процедуры", "процедур")}"

fun Int.sensorCountText(): String = "$this ${russianPlural(this, "замер", "замера", "замеров")}"

fun journalDaysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isJournalLeapYear(year)) 29 else 28
    else -> 30
}

private fun russianPlural(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    if (mod100 in 11..14) return many
    return when (count % 10) {
        1 -> one
        in 2..4 -> few
        else -> many
    }
}

private fun isJournalLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

fun String.substringOrNull(startIndex: Int, endIndex: Int): String? =
    if (length >= endIndex) substring(startIndex, endIndex) else null

fun Int.pad2(): String = toString().padStart(2, '0')

private fun parseJournalInstantOrNull(timestamp: String): Instant? = runCatching {
    Instant.parse(timestamp)
}.getOrNull()
