package com.aura.feature.profile.domain.usecase

import com.aura.feature.profile.domain.repository.ProfileNotificationsRepository

class LoadProfileNotificationsUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke() = repository.load()
}
