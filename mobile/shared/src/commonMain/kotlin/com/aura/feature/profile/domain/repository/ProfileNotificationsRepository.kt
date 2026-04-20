package com.aura.feature.profile.domain.repository

import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.feature.profile.domain.model.ProfileNotificationsData

interface ProfileNotificationsRepository {
    suspend fun load(): ProfileNotificationsData
    suspend fun save(settings: ProfileNotificationSettings): ProfileNotificationSettings
}
