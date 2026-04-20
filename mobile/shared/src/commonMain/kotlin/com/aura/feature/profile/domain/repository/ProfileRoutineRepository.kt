package com.aura.feature.profile.domain.repository

import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.RoutineProductOption

interface ProfileRoutineRepository {
    suspend fun loadRoutine(): List<ProfileRoutineStep>
    suspend fun searchProducts(query: String): List<RoutineProductOption>
    suspend fun saveRoutine(steps: List<ProfileRoutineStep>): List<ProfileRoutineStep>
}
