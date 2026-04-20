package com.aura.feature.profile.domain.usecase

import com.aura.feature.profile.domain.repository.ProfileParametersRepository

class RefreshSkinPassportUseCase(private val repository: ProfileParametersRepository) {
    suspend operator fun invoke() = repository.refreshSkinPassport()
}
