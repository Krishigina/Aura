package com.aura.core.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class SurveyQuestionSchema(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val multi_choice: Boolean = false,
    val options: List<String> = emptyList(),
)

@Serializable
data class SurveySectionSchema(
    val title: String,
    val questions: List<SurveyQuestionSchema> = emptyList(),
)

@Serializable
data class SurveySchemaResponse(
    val sections: List<SurveySectionSchema> = emptyList(),
)
