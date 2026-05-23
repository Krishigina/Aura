package com.aura.app

import java.time.LocalDate

internal object ProfileReminderScheduleLogic {
    fun isWeeklyDue(configuredWeekday: Int, currentWeekday: Int): Boolean {
        return configuredWeekday in 1..7 && configuredWeekday == currentWeekday
    }

    fun isMonthlyDue(today: LocalDate, configuredDay: Int): Boolean {
        if (configuredDay !in 1..31) return false
        val effectiveDay = configuredDay.coerceAtMost(today.lengthOfMonth())
        return today.dayOfMonth == effectiveDay
    }

    fun sentDateKey(domain: String): String = when (domain) {
        "routine" -> "routine_last_fire_date"
        "journal" -> "journal_last_fire_date"
        else -> "unknown_last_fire_date"
    }
}
