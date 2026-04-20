package com.aura.feature.profile.presentation.settings

import com.aura.core.domain.model.BackendUser

enum class ProfileSettingsTab { ROOT, NAME, LOGIN, PASSWORD, DELETE }

data class ProfileSettingsUiState(
    val user: BackendUser? = null,
    val tab: ProfileSettingsTab = ProfileSettingsTab.ROOT,
    val name: String = user?.name.orEmpty(),
    val nickname: String = user?.nickname.orEmpty(),
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val deletePassword: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val accountDeleted: Boolean = false,
)
