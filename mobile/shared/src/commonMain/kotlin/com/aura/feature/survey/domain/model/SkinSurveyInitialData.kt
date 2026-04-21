package com.aura.feature.survey.domain.model

import com.aura.core.data.api.model.SurveySectionSchema

data class SkinSurveyInitialData(
    val sections: List<SurveySectionSchema>,
    val answers: Map<String, List<String>>,
)
