package com.aura.feature.profile.domain.model

import com.aura.core.domain.model.ProfileNotificationSettings

data class ProfileNotificationsData(
    val routineStepsCount: Int,
    val settings: ProfileNotificationSettings,
)
