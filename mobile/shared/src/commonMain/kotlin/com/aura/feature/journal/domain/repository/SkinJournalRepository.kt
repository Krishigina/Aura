package com.aura.feature.journal.domain.repository

import com.aura.core.domain.model.ProcedureCatalogItem
import com.aura.core.domain.model.SkinJournalProcedureCreate
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReadingCreate

interface SkinJournalRepository {
    suspend fun loadJournal(): SkinJournalResponse
    suspend fun updateReminder(reminderId: String, action: SkinJournalReminderAction): SkinJournalResponse
    suspend fun loadProcedureCatalog(): List<ProcedureCatalogItem>
    suspend fun createProcedure(payload: SkinJournalProcedureCreate): SkinJournalResponse
    suspend fun createSensorReading(payload: SkinJournalSensorReadingCreate): SkinJournalResponse
}
