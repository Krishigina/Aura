package com.aura.feature.home.domain.repository

import com.aura.feature.home.domain.model.HomeDashboardData
import com.aura.feature.home.domain.model.HomeStatusData

interface HomeRepository {
    suspend fun getDashboard(): HomeDashboardData
    suspend fun getStatus(latitude: Double?, longitude: Double?): HomeStatusData
    fun getUserName(): String?
}
