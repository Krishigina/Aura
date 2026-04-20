package com.aura.feature.profile.data.api

import com.aura.core.data.api.model.RecommendationFavoritesResponse
import com.aura.core.domain.model.BackendUser
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ProfileRoutineResponse
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.RoutineProductOption
import com.aura.core.domain.model.SkinPassportResponse

interface ProfileApi {
    suspend fun updateAccount(token: String, name: String, nickname: String?): BackendUser
    suspend fun updatePassword(token: String, currentPassword: String, newPassword: String)
    suspend fun deleteAccount(token: String, currentPassword: String)
    suspend fun getRoutine(token: String): ProfileRoutineResponse
    suspend fun searchRoutineProducts(token: String, query: String): List<RoutineProductOption>
    suspend fun saveRoutine(token: String, steps: List<ProfileRoutineStep>): ProfileRoutineResponse
    suspend fun getNotificationSettings(token: String): ProfileNotificationSettings
    suspend fun saveNotificationSettings(token: String, settings: ProfileNotificationSettings): ProfileNotificationSettings
    suspend fun getRecommendationFavorites(token: String): RecommendationFavoritesResponse
    suspend fun getSkinPassport(token: String): SkinPassportResponse?
}
