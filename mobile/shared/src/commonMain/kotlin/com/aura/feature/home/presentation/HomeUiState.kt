package com.aura.feature.home.presentation

import com.aura.core.data.api.model.HomeInsightItem
import com.aura.core.data.api.model.HomeRitualItem
import com.aura.core.data.api.model.HomeStatusResponse
import com.aura.core.data.api.model.WeatherCoordinates
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.SkinJournalReminder

data class HomeUiState(
    val userName: String? = null,
    val ritualItems: List<HomeRitualItem> = emptyList(),
    val ritualCheckedStates: Map<String, Boolean> = emptyMap(),
    val insights: List<HomeInsightItem> = emptyList(),
    val feedLoading: Boolean = false,
    val homeStatus: HomeStatusResponse = HomeStatusResponse(),
    val statusLoading: Boolean = false,
    val weatherCoordinates: WeatherCoordinates? = null,
    val locationRequested: Boolean = false,
    val activeJournalReminders: List<SkinJournalReminder> = emptyList(),
    val routineSteps: List<ProfileRoutineStep> = emptyList(),
    val routineLoading: Boolean = true,
    val routineLoadFailed: Boolean = false,
    val routineLoaded: Boolean = false,
)
