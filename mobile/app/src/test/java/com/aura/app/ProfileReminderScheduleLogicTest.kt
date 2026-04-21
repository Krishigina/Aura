package com.aura.app.notifications

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileReminderScheduleLogicTest {

    @Test
    fun monthly31_fallsBackToLastDayInFebruary() {
        val febLastDay = LocalDate.parse("2026-02-28")
        assertTrue(ProfileReminderScheduleLogic.isMonthlyDue(febLastDay, 31))
    }

    @Test
    fun weekly_onlyConfiguredWeekdayMatches() {
        assertTrue(ProfileReminderScheduleLogic.isWeeklyDue(configuredWeekday = 2, currentWeekday = 2))
        assertFalse(ProfileReminderScheduleLogic.isWeeklyDue(configuredWeekday = 2, currentWeekday = 3))
    }

    @Test
    fun invalidWeeklyConfiguration_isNotDue() {
        assertFalse(ProfileReminderScheduleLogic.isWeeklyDue(configuredWeekday = -1, currentWeekday = 1))
        assertFalse(ProfileReminderScheduleLogic.isWeeklyDue(configuredWeekday = 8, currentWeekday = 1))
    }

    @Test
    fun invalidMonthlyConfiguration_isNotDue() {
        val anyDate = LocalDate.parse("2026-05-15")
        assertFalse(ProfileReminderScheduleLogic.isMonthlyDue(anyDate, 0))
        assertFalse(ProfileReminderScheduleLogic.isMonthlyDue(anyDate, 32))
    }

    @Test
    fun sentDateKey_isStablePerDomain() {
        assertEquals("routine_last_fire_date", ProfileReminderScheduleLogic.sentDateKey("routine"))
        assertEquals("journal_last_fire_date", ProfileReminderScheduleLogic.sentDateKey("journal"))
        assertEquals("unknown_last_fire_date", ProfileReminderScheduleLogic.sentDateKey("other"))
    }
}
