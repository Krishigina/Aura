package com.aura.feature.survey.domain.repository

import com.aura.feature.survey.domain.model.SkinSurveyInitialData

interface SkinSurveyRepository {
    suspend fun loadInitialData(): SkinSurveyInitialData
    suspend fun savePassport(answers: Map<String, List<String>>): Map<String, List<String>>
    suspend fun saveSensorOwnership(hasSensor: Boolean)
}
