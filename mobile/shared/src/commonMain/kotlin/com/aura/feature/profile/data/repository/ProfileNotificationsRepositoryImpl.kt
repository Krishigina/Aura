package com.aura.feature.profile.data.repository

import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.profile.data.api.ProfileApi
import com.aura.feature.profile.domain.model.ProfileNotificationsData
import com.aura.feature.profile.domain.repository.ProfileNotificationsRepository

class ProfileNotificationsRepositoryImpl(
    private val profileApi: ProfileApi,
    private val sessionRepository: SessionRepository,
) : ProfileNotificationsRepository {
    override suspend fun load(): ProfileNotificationsData {
        val token = requireToken()
        val routineStepsCount = runCatching { profileApi.getRoutine(token).steps.size }.getOrDefault(0)
        val settings = profileApi.getNotificationSettings(token)
        return ProfileNotificationsData(routineStepsCount, settings)
    }

    override suspend fun save(settings: ProfileNotificationSettings): ProfileNotificationSettings {
        return profileApi.saveNotificationSettings(requireToken(), settings)
    }

    private fun requireToken(): String = sessionRepository.token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException("Сессия истекла, войдите снова")
}
