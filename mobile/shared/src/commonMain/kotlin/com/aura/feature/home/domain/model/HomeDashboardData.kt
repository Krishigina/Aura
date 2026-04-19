package com.aura.feature.home.domain.model

import com.aura.core.data.api.model.HomeInsightItem
import com.aura.core.data.api.model.HomeRitualItem
import com.aura.core.data.api.model.HomeStatusResponse
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.SkinJournalReminder

data class HomeDashboardData(
    val ritualItems: List<HomeRitualItem> = emptyList(),
    val insights: List<HomeInsightItem> = emptyList(),
    val activeJournalReminders: List<SkinJournalReminder> = emptyList(),
    val routineSteps: List<ProfileRoutineStep> = emptyList(),
    val routineLoadFailed: Boolean = false,
    val routineLoaded: Boolean = false,
)

data class HomeStatusData(
    val status: HomeStatusResponse = HomeStatusResponse(),
)
