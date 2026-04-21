package com.aura.core.data.api.client

import com.aura.core.domain.model.ProcedureCatalogItem
import com.aura.core.domain.model.SkinJournalProcedureCreate
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReadingCreate
import com.aura.core.domain.model.SkinJournalSettingsUpdate
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class SkinJournalNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val extractErrorMessage: (String, HttpStatusCode) -> String,
) {
    suspend fun getSkinJournal(token: String): SkinJournalResponse {
        val response = httpClient.get("$baseUrl/api/profile/skin-journal") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::SkinJournalResponse)
    }

    suspend fun saveSettings(token: String, payload: SkinJournalSettingsUpdate): SkinJournalResponse {
        val response = httpClient.put("$baseUrl/api/profile/skin-journal/settings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun createProcedure(token: String, payload: SkinJournalProcedureCreate): SkinJournalResponse {
        val response = httpClient.post("$baseUrl/api/profile/skin-journal/procedures") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun createSensorReading(token: String, payload: SkinJournalSensorReadingCreate): SkinJournalResponse {
        val response = httpClient.post("$baseUrl/api/profile/skin-journal/sensor-readings") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun updateReminder(token: String, reminderId: String, payload: SkinJournalReminderAction): SkinJournalResponse {
        val response = httpClient.patch("$baseUrl/api/profile/skin-journal/reminders/$reminderId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun getProcedureCatalog(token: String): List<ProcedureCatalogItem> {
        val response = httpClient.get("$baseUrl/api/procedures") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage)) { emptyList() }
    }
}
