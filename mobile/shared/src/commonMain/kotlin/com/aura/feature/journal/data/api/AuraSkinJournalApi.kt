package com.aura.feature.journal.data.api

import com.aura.core.data.api.client.SkinJournalNetworkClient
import com.aura.core.domain.model.ProcedureCatalogItem
import com.aura.core.domain.model.SkinJournalProcedureCreate
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReadingCreate

internal class AuraSkinJournalApi(
    private val apiClient: SkinJournalNetworkClient,
) : SkinJournalApi {
    override suspend fun getJournal(token: String): SkinJournalResponse {
        return apiClient.getSkinJournal(token)
    }

    override suspend fun updateReminder(token: String, reminderId: String, action: SkinJournalReminderAction): SkinJournalResponse {
        return apiClient.updateReminder(token, reminderId, action)
    }

    override suspend fun getProcedureCatalog(token: String): List<ProcedureCatalogItem> {
        return apiClient.getProcedureCatalog(token)
    }

    override suspend fun createProcedure(token: String, payload: SkinJournalProcedureCreate): SkinJournalResponse {
        return apiClient.createProcedure(token, payload)
    }

    override suspend fun createSensorReading(token: String, payload: SkinJournalSensorReadingCreate): SkinJournalResponse {
        return apiClient.createSensorReading(token, payload)
    }
}
