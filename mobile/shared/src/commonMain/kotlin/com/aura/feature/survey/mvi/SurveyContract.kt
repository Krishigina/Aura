package com.aura.feature.survey.mvi

import com.aura.core.presentation.mvi.Effect
import com.aura.core.presentation.mvi.Intent
import com.aura.core.presentation.mvi.UiState

data class SurveyUiState(
    val sectionIndex: Int = 0,
    val answers: Map<String, Set<String>> = emptyMap(),
    val isSaving: Boolean = false
): UiState

sealed interface SurveyIntent : Intent {
    data class SetAnswers(val answers: Map<String, Set<String>>) : SurveyIntent
    data class SelectOption(
        val questionId: String,
        val option: String,
        val multiChoice: Boolean
    ) : SurveyIntent
    data object PreviousSection : SurveyIntent
    data object NextSection : SurveyIntent
    data class SetSaving(val value: Boolean) : SurveyIntent
}

sealed interface SurveyEffect : Effect {
    data object MovedNext : SurveyEffect
    data object Completed : SurveyEffect
    data class SaveFailed(val message: String) : SurveyEffect
}
