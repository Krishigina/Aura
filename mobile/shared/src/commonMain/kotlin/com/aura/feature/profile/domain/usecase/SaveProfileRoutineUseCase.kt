package com.aura.feature.profile.domain.usecase

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.feature.profile.domain.repository.ProfileRoutineRepository

class SaveProfileRoutineUseCase(private val repository: ProfileRoutineRepository) {
    suspend operator fun invoke(steps: List<ProfileRoutineStep>) = repository.saveRoutine(steps)
}
