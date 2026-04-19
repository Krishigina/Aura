package com.aura.feature.home.data.repository

import com.aura.core.data.api.model.HomeStatusResponse
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.home.data.api.HomeApi
import com.aura.feature.home.domain.model.HomeDashboardData
import com.aura.feature.home.domain.model.HomeStatusData
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.journal.activeTodayReminders

class HomeRepositoryImpl(
    private val homeApi: HomeApi,
    private val sessionRepository: SessionRepository,
) : HomeRepository {
    override suspend fun getDashboard(): HomeDashboardData {
        val token = requireToken()
        val feed = runCatching { homeApi.getHomeFeed(token) }.getOrNull()
        val reminders = runCatching { homeApi.getSkinJournal(token) }
            .getOrNull()
            ?.reminders
            ?.let { activeTodayReminders(it) }
            .orEmpty()
        val routineResult = runCatching { homeApi.getProfileRoutine(token) }

        return HomeDashboardData(
            ritualItems = feed?.ritual_items.orEmpty(),
            insights = feed?.insights.orEmpty(),
            activeJournalReminders = reminders,
            routineSteps = routineResult.getOrNull()?.steps.orEmpty(),
            routineLoadFailed = routineResult.isFailure,
            routineLoaded = routineResult.isSuccess,
        )
    }

    override suspend fun getStatus(latitude: Double?, longitude: Double?): HomeStatusData {
        val status = runCatching {
            homeApi.getHomeStatus(
                token = requireToken(),
                latitude = latitude,
                longitude = longitude,
            )
        }.getOrDefault(HomeStatusResponse())
        return HomeStatusData(status)
    }

    override fun getUserName(): String? {
        return sessionRepository.user()?.name?.takeIf { it.isNotBlank() }
    }

    private fun requireToken(): String {
        return sessionRepository.token().takeUnless { it.isNullOrBlank() }
            ?: throw IllegalStateException("Нужна авторизация")
    }
}
