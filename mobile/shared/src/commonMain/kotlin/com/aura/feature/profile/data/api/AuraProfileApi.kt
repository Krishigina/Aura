package com.aura.feature.profile.data.api

import com.aura.core.data.api.client.AuthNetworkClient
import com.aura.core.data.api.client.ProductNetworkClient
import com.aura.core.data.api.client.ProfileNetworkClient
import com.aura.core.data.api.client.RecommendationsNetworkClient
import com.aura.core.data.api.model.RecommendationFavoritesResponse
import com.aura.core.domain.model.BackendUser
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ProfileRoutineResponse
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ProfileRoutineUpdateRequest
import com.aura.core.domain.model.RoutineProductOption
import com.aura.core.domain.model.SkinPassportResponse

internal class AuraProfileApi(
    private val authApi: AuthNetworkClient,
    private val profileApi: ProfileNetworkClient,
    private val productApi: ProductNetworkClient,
    private val recommendationsApi: RecommendationsNetworkClient,
) : ProfileApi {
    override suspend fun updateAccount(token: String, name: String, nickname: String?): BackendUser {
        return authApi.updateProfileAccount(token, name, nickname)
    }

    override suspend fun updatePassword(token: String, currentPassword: String, newPassword: String) {
        authApi.updateProfilePassword(token, currentPassword, newPassword)
    }

    override suspend fun deleteAccount(token: String, currentPassword: String) {
        authApi.deleteProfileAccount(token, currentPassword)
    }

    override suspend fun getRoutine(token: String): ProfileRoutineResponse {
        return profileApi.getProfileRoutine(token)
    }

    override suspend fun searchRoutineProducts(token: String, query: String): List<RoutineProductOption> {
        return productApi.searchProductsForRoutine(token, query)
    }

    override suspend fun saveRoutine(token: String, steps: List<ProfileRoutineStep>): ProfileRoutineResponse {
        return profileApi.saveProfileRoutine(token, ProfileRoutineUpdateRequest(steps))
    }

    override suspend fun getNotificationSettings(token: String): ProfileNotificationSettings {
        return profileApi.getNotificationSettings(token)
    }

    override suspend fun saveNotificationSettings(token: String, settings: ProfileNotificationSettings): ProfileNotificationSettings {
        return profileApi.saveNotificationSettings(token, settings)
    }

    override suspend fun getRecommendationFavorites(token: String): RecommendationFavoritesResponse {
        return recommendationsApi.getFavorites(token)
    }

    override suspend fun getSkinPassport(token: String): SkinPassportResponse? {
        return profileApi.getSkinPassport(token)
    }
}
