package com.aura.feature.profile.data.repository

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.RoutineProductOption
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.profile.data.api.ProfileApi
import com.aura.feature.profile.domain.repository.ProfileRoutineRepository

class ProfileRoutineRepositoryImpl(
    private val profileApi: ProfileApi,
    private val sessionRepository: SessionRepository,
) : ProfileRoutineRepository {
    override suspend fun loadRoutine(): List<ProfileRoutineStep> {
        return profileApi.getRoutine(requireToken()).steps
    }

    override suspend fun searchProducts(query: String): List<RoutineProductOption> {
        return profileApi.searchRoutineProducts(requireToken(), query.trim())
    }

    override suspend fun saveRoutine(steps: List<ProfileRoutineStep>): List<ProfileRoutineStep> {
        return profileApi.saveRoutine(requireToken(), steps).steps
    }

    private fun requireToken(): String = sessionRepository.token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException("Сессия истекла, войдите снова")
}
