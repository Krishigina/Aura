package com.aura.feature.profile.data.repository

import com.aura.core.domain.repository.SessionRepository
import com.aura.core.domain.repository.SkinPassportRepository
import com.aura.feature.profile.data.api.ProfileApi
import com.aura.feature.profile.domain.repository.ProfileParametersRepository

class ProfileParametersRepositoryImpl(
    private val profileApi: ProfileApi,
    private val sessionRepository: SessionRepository,
    private val skinPassportRepository: SkinPassportRepository,
) : ProfileParametersRepository {
    override suspend fun refreshSkinPassport() {
        val token = sessionRepository.token().takeUnless { it.isNullOrBlank() } ?: return
        runCatching { profileApi.getSkinPassport(token) }
            .getOrNull()
            ?.let { serverPassport ->
                if (serverPassport.answers.isNotEmpty()) {
                    skinPassportRepository.save(serverPassport.answers)
                } else {
                    skinPassportRepository.clear()
                }
            }
    }

    override fun skinPassportAnswers(): Map<String, List<String>> {
        return skinPassportRepository.answers()
    }
}
