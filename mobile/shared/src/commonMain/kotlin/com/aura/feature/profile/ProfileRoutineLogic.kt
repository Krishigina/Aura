package com.aura.feature.profile

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ReminderFrequency

fun validateRoutineStep(step: ProfileRoutineStep): String? {
    if (step.product_label.isBlank()) return "Укажите продукт"
    if (step.frequency != ReminderFrequency.NONE && step.reminder_time.isNullOrBlank()) {
        return "Укажите время напоминания"
    }
    return null
}

fun normalizeRoutineStepOrder(steps: List<ProfileRoutineStep>): List<ProfileRoutineStep> {
    return steps.sortedBy { it.order }.mapIndexed { index, step ->
        step.copy(order = index + 1)
    }
}

fun shouldHideHomeRoutineChecklist(steps: List<ProfileRoutineStep>): Boolean = steps.isEmpty()
