package com.aura.feature.home.data.api

import com.aura.core.data.api.client.GeneralContentNetworkClient
import com.aura.core.data.api.client.ProfileNetworkClient
import com.aura.core.data.api.client.SkinJournalNetworkClient
import com.aura.core.data.api.model.HomeFeedResponse
import com.aura.core.data.api.model.HomeStatusResponse
import com.aura.core.domain.model.ProfileRoutineResponse
import com.aura.core.domain.model.SkinJournalResponse

internal class AuraHomeApi(
    private val generalApi: GeneralContentNetworkClient,
    private val skinJournalApi: SkinJournalNetworkClient,
    private val profileApi: ProfileNetworkClient,
) : HomeApi {
    override suspend fun getHomeFeed(token: String): HomeFeedResponse {
        return generalApi.getHomeFeed(token)
    }

    override suspend fun getSkinJournal(token: String): SkinJournalResponse {
        return skinJournalApi.getSkinJournal(token)
    }

    override suspend fun getProfileRoutine(token: String): ProfileRoutineResponse {
        return profileApi.getProfileRoutine(token)
    }

    override suspend fun getHomeStatus(token: String, latitude: Double?, longitude: Double?): HomeStatusResponse {
        return generalApi.getHomeStatus(token, latitude, longitude)
    }
}
