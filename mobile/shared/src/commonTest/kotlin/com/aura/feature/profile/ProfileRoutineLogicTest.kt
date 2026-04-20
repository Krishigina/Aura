package com.aura.feature.profile

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ReminderFrequency
import com.aura.feature.profile.logic.isScheduledToday
import com.aura.feature.profile.logic.normalizeRoutineStepOrder
import com.aura.feature.profile.logic.shouldHideHomeRoutineChecklist
import com.aura.feature.profile.logic.validateRoutineStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileRoutineLogicTest {

    @Test
    fun requiresProductLabelWhenBlank() {
        val error = validateRoutineStep(
            ProfileRoutineStep(
                product_label = "   ",
                frequency = ReminderFrequency.NONE,
                reminder_time = "",
            ),
        )

        assertEquals("Укажите продукт", error)
    }

    @Test
    fun dailyDoesNotRequireSelector() {
        val error = validateRoutineStep(
            ProfileRoutineStep(
                product_label = "Cleanser",
                frequency = ReminderFrequency.DAILY,
                weekday = null,
            ),
        )

        assertEquals(null, error)
    }

    @Test
    fun noneFrequencyIsNotAllowedForRoutine() {
        val error = validateRoutineStep(
            ProfileRoutineStep(
                product_label = "Cleanser",
                frequency = ReminderFrequency.NONE,
            ),
        )

        assertEquals("Выберите частоту", error)
    }

    @Test
    fun weeklyWithoutWeekdayReturnsValidationError() {
        val error = validateRoutineStep(
            ProfileRoutineStep(
                product_label = "Serum",
                frequency = ReminderFrequency.WEEKLY,
                weekday = null,
            ),
        )

        assertEquals("Укажите день недели", error)
    }

    @Test
    fun monthlyWithoutMonthDayReturnsValidationError() {
        val error = validateRoutineStep(
            ProfileRoutineStep(
                product_label = "Toner",
                frequency = ReminderFrequency.MONTHLY,
                month_day = null,
            ),
        )

        assertEquals("Укажите день месяца", error)
    }

    @Test
    fun monthlyFallbackSchedulesOnLastDayWhenMonthTooShort() {
        val scheduled = isScheduledToday(
            step = ProfileRoutineStep(product_label = "Mask", frequency = ReminderFrequency.MONTHLY, month_day = 31),
            year = 2026,
            month = 4,
            dayOfMonth = 30,
            dayOfWeek = 4,
        )

        assertTrue(scheduled)
    }

    @Test
    fun scheduleSanityChecksForDailyWeeklyMonthly() {
        val daily = isScheduledToday(
            step = ProfileRoutineStep(product_label = "Daily", frequency = ReminderFrequency.DAILY),
            year = 2026,
            month = 5,
            dayOfMonth = 22,
            dayOfWeek = 5,
        )
        val weeklyTrue = isScheduledToday(
            step = ProfileRoutineStep(product_label = "Weekly", frequency = ReminderFrequency.WEEKLY, weekday = 5),
            year = 2026,
            month = 5,
            dayOfMonth = 22,
            dayOfWeek = 5,
        )
        val weeklyFalse = isScheduledToday(
            step = ProfileRoutineStep(product_label = "Weekly", frequency = ReminderFrequency.WEEKLY, weekday = 1),
            year = 2026,
            month = 5,
            dayOfMonth = 22,
            dayOfWeek = 5,
        )
        val monthlyFalse = isScheduledToday(
            step = ProfileRoutineStep(product_label = "Monthly", frequency = ReminderFrequency.MONTHLY, month_day = 10),
            year = 2026,
            month = 5,
            dayOfMonth = 22,
            dayOfWeek = 5,
        )

        assertTrue(daily)
        assertTrue(weeklyTrue)
        assertFalse(weeklyFalse)
        assertFalse(monthlyFalse)
    }

    @Test
    fun hidesChecklistWhenNoSteps() {
        assertTrue(shouldHideHomeRoutineChecklist(emptyList()))
        assertFalse(shouldHideHomeRoutineChecklist(listOf(ProfileRoutineStep(product_label = "Cream"))))
    }

    @Test
    fun normalizesNonContiguousOrderToOneToN() {
        val normalized = normalizeRoutineStepOrder(
            listOf(
                ProfileRoutineStep(id = "c", product_label = "C", order = 9),
                ProfileRoutineStep(id = "a", product_label = "A", order = 4),
                ProfileRoutineStep(id = "b", product_label = "B", order = 6),
            ),
        )

        assertEquals(listOf("a", "b", "c"), normalized.map { it.id })
        assertEquals(listOf(1, 2, 3), normalized.map { it.order })
    }
}
