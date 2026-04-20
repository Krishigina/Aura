package com.aura.feature.profile.domain.repository

import com.aura.core.domain.model.BackendUser

interface ProfileSettingsRepository {
    fun currentUser(): BackendUser?
    suspend fun updateAccount(name: String, nickname: String?): BackendUser
    suspend fun updatePassword(currentPassword: String, newPassword: String)
    suspend fun deleteAccount(currentPassword: String)
}
