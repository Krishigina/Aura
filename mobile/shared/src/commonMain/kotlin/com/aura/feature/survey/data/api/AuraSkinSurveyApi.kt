package com.aura.feature.survey.data.api

import com.aura.core.data.api.client.GeneralContentNetworkClient
import com.aura.core.data.api.client.ProfileNetworkClient
import com.aura.core.data.api.client.SkinJournalNetworkClient
import com.aura.core.data.api.model.SurveySchemaResponse
import com.aura.core.domain.model.SkinJournalSettingsUpdate
import com.aura.core.domain.model.SkinPassportResponse
import kotlinx.datetime.Clock

internal class AuraSkinSurveyApi(
    private val generalApi: GeneralContentNetworkClient,
    private val profileApi: ProfileNetworkClient,
    private val skinJournalApi: SkinJournalNetworkClient,
) : SkinSurveyApi {
    override suspend fun getSurveySchema(token: String): SurveySchemaResponse {
        return generalApi.getSurveySchema(token)
    }

    override suspend fun getSkinPassport(token: String): SkinPassportResponse? {
        return profileApi.getSkinPassport(token)
    }

    override suspend fun saveSkinPassport(token: String, answers: Map<String, List<String>>): SkinPassportResponse {
        return profileApi.saveSkinPassport(token, answers, completedAtEpochMillis = Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun saveSkinJournalSettings(token: String, settings: SkinJournalSettingsUpdate) {
        skinJournalApi.saveSettings(token, settings)
    }
}
