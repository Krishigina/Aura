package com.aura.feature.profile.domain.repository

interface ProfileParametersRepository {
    suspend fun refreshSkinPassport()
    fun skinPassportAnswers(): Map<String, List<String>>
}
