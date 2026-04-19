package com.aura.feature.home.data.api

import com.aura.core.data.api.model.HomeFeedResponse
import com.aura.core.data.api.model.HomeStatusResponse
import com.aura.core.domain.model.ProfileRoutineResponse
import com.aura.core.domain.model.SkinJournalResponse

interface HomeApi {
    suspend fun getHomeFeed(token: String): HomeFeedResponse
    suspend fun getSkinJournal(token: String): SkinJournalResponse
    suspend fun getProfileRoutine(token: String): ProfileRoutineResponse
    suspend fun getHomeStatus(token: String, latitude: Double?, longitude: Double?): HomeStatusResponse
}
