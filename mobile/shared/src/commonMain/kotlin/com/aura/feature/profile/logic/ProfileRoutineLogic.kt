package com.aura.feature.profile.logic

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ReminderFrequency

fun validateRoutineStep(step: ProfileRoutineStep): String? {
    if (step.product_label.isBlank()) return "Укажите продукт"
    when (step.frequency) {
        ReminderFrequency.NONE -> return "Выберите частоту"
        ReminderFrequency.WEEKLY -> {
            if (step.weekday == null || step.weekday !in 1..7) return "Укажите день недели"
        }

        ReminderFrequency.MONTHLY -> {
            if (step.month_day == null || step.month_day !in 1..31) return "Укажите день месяца"
        }

        else -> Unit
    }
    return null
}

fun isScheduledToday(
    step: ProfileRoutineStep,
    year: Int,
    month: Int,
    dayOfMonth: Int,
    dayOfWeek: Int,
): Boolean {
    return when (step.frequency) {
        ReminderFrequency.DAILY -> true
        ReminderFrequency.WEEKLY -> step.weekday == dayOfWeek
        ReminderFrequency.MONTHLY -> {
            val selectedDay = step.month_day ?: return false
            val monthLength = daysInMonth(year, month)
            val targetDay = if (selectedDay > monthLength) monthLength else selectedDay
            dayOfMonth == targetDay
        }

        ReminderFrequency.NONE -> false
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 31
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

fun normalizeRoutineStepOrder(steps: List<ProfileRoutineStep>): List<ProfileRoutineStep> {
    return steps.sortedBy { it.order }.mapIndexed { index, step ->
        step.copy(order = index + 1)
    }
}

fun shouldHideHomeRoutineChecklist(steps: List<ProfileRoutineStep>): Boolean = steps.isEmpty()
