package com.aura.feature.survey.data.api

import com.aura.core.data.api.model.SurveySchemaResponse
import com.aura.core.domain.model.SkinJournalSettingsUpdate
import com.aura.core.domain.model.SkinPassportResponse

interface SkinSurveyApi {
    suspend fun getSurveySchema(token: String): SurveySchemaResponse
    suspend fun getSkinPassport(token: String): SkinPassportResponse?
    suspend fun saveSkinPassport(token: String, answers: Map<String, List<String>>): SkinPassportResponse
    suspend fun saveSkinJournalSettings(token: String, settings: SkinJournalSettingsUpdate)
}
