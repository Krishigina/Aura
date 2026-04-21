package com.aura.feature.survey.presentation

import com.aura.core.data.api.model.SurveySectionSchema

data class SkinSurveyUiState(
    val sections: List<SurveySectionSchema> = emptyList(),
    val initialAnswers: Map<String, List<String>> = emptyMap(),
    val selectedAnswers: Map<String, Set<String>> = emptyMap(),
    val sectionIndex: Int = 0,
    val isSchemaLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val showSensorPrompt: Boolean = false,
    val sensorOwnershipSaved: Boolean = false,
)
