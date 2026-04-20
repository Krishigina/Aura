package com.aura.feature.profile.presentation.routine

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.RoutineProductOption

data class ProfileRoutineUiState(
    val routineSteps: List<ProfileRoutineStep> = emptyList(),
    val searchResults: Map<String, List<RoutineProductOption>> = emptyMap(),
    val searchQueries: Map<String, String> = emptyMap(),
    val pendingRemoveStepId: String? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val success: String? = null,
)
