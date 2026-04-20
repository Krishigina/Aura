package com.aura.feature.profile.presentation.notifications

import com.aura.core.domain.model.ProfileNotificationSettings

data class ProfileNotificationsUiState(
    val routineStepsCount: Int = 0,
    val settings: ProfileNotificationSettings = ProfileNotificationSettings(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val success: String? = null,
)
