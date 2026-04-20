package com.aura.feature.profile.domain.usecase

import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.feature.profile.domain.repository.ProfileNotificationsRepository

class SaveProfileNotificationsUseCase(private val repository: ProfileNotificationsRepository) {
    suspend operator fun invoke(settings: ProfileNotificationSettings) = repository.save(settings)
}
