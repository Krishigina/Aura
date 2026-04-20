package com.aura.feature.journal.data.repository

import com.aura.core.domain.model.ProcedureCatalogItem
import com.aura.core.domain.model.SkinJournalProcedureCreate
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReadingCreate
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.journal.data.api.SkinJournalApi
import com.aura.feature.journal.domain.repository.SkinJournalRepository

class SkinJournalRepositoryImpl(
    private val skinJournalApi: SkinJournalApi,
    private val sessionRepository: SessionRepository,
) : SkinJournalRepository {
    override suspend fun loadJournal(): SkinJournalResponse = skinJournalApi.getJournal(requireToken())

    override suspend fun updateReminder(reminderId: String, action: SkinJournalReminderAction): SkinJournalResponse {
        return skinJournalApi.updateReminder(requireToken(), reminderId, action)
    }

    override suspend fun loadProcedureCatalog(): List<ProcedureCatalogItem> {
        return skinJournalApi.getProcedureCatalog(requireToken())
    }

    override suspend fun createProcedure(payload: SkinJournalProcedureCreate): SkinJournalResponse {
        return skinJournalApi.createProcedure(requireToken(), payload)
    }

    override suspend fun createSensorReading(payload: SkinJournalSensorReadingCreate): SkinJournalResponse {
        return skinJournalApi.createSensorReading(requireToken(), payload)
    }

    private fun requireToken(): String = sessionRepository.token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException("Войдите в аккаунт, чтобы продолжить")
}
