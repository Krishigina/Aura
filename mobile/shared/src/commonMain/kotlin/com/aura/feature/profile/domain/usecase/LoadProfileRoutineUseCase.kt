package com.aura.feature.profile.domain.usecase

import com.aura.feature.profile.domain.repository.ProfileRoutineRepository

class LoadProfileRoutineUseCase(private val repository: ProfileRoutineRepository) {
    suspend operator fun invoke() = repository.loadRoutine()
}
