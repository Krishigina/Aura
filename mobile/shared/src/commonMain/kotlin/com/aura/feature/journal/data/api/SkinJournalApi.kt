package com.aura.feature.journal.data.api

import com.aura.core.domain.model.ProcedureCatalogItem
import com.aura.core.domain.model.SkinJournalProcedureCreate
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReadingCreate

interface SkinJournalApi {
    suspend fun getJournal(token: String): SkinJournalResponse
    suspend fun updateReminder(token: String, reminderId: String, action: SkinJournalReminderAction): SkinJournalResponse
    suspend fun getProcedureCatalog(token: String): List<ProcedureCatalogItem>
    suspend fun createProcedure(token: String, payload: SkinJournalProcedureCreate): SkinJournalResponse
    suspend fun createSensorReading(token: String, payload: SkinJournalSensorReadingCreate): SkinJournalResponse
}
