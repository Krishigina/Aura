package com.aura.feature.profile.data.repository

import com.aura.core.domain.model.BackendUser
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.profile.data.api.ProfileApi
import com.aura.feature.profile.domain.repository.ProfileSettingsRepository

class ProfileSettingsRepositoryImpl(
    private val profileApi: ProfileApi,
    private val sessionRepository: SessionRepository,
) : ProfileSettingsRepository {
    override fun currentUser(): BackendUser? = sessionRepository.user()

    override suspend fun updateAccount(name: String, nickname: String?): BackendUser {
        val updated = profileApi.updateAccount(requireToken(), name, nickname)
        sessionRepository.setUser(updated)
        return updated
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String) {
        profileApi.updatePassword(requireToken(), currentPassword, newPassword)
    }

    override suspend fun deleteAccount(currentPassword: String) {
        profileApi.deleteAccount(requireToken(), currentPassword)
        sessionRepository.clear()
    }

    private fun requireToken(): String = sessionRepository.token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException("Сессия истекла, войдите снова")
}
