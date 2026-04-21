package com.aura.core.data.api.client

import com.aura.core.data.api.model.SkinPassportRequest
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ProfileRoutineResponse
import com.aura.core.domain.model.ProfileRoutineUpdateRequest
import com.aura.core.domain.model.SkinPassportResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

internal class ProfileNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val extractErrorMessage: (String, HttpStatusCode) -> String,
) {
    suspend fun getSkinPassport(token: String): SkinPassportResponse? {
        val response = httpClient.get("$baseUrl/api/profile/skin-passport") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status == HttpStatusCode.NotFound) return null

        val responseText = response.bodyTextOrThrow(extractErrorMessage)
        return json.decodeOrDefault(responseText) { null }
    }

    suspend fun saveSkinPassport(token: String, answers: Map<String, List<String>>, completedAtEpochMillis: Long): SkinPassportResponse {
        val response = httpClient.put("$baseUrl/api/profile/skin-passport") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SkinPassportRequest(answers = answers, completed_at_epoch_millis = completedAtEpochMillis))
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun getProfileRoutine(token: String): ProfileRoutineResponse {
        val response = httpClient.get("$baseUrl/api/profile/routine") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ProfileRoutineResponse)
    }

    suspend fun saveProfileRoutine(token: String, payload: ProfileRoutineUpdateRequest): ProfileRoutineResponse {
        val response = httpClient.put("$baseUrl/api/profile/routine") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ProfileRoutineResponse)
    }

    suspend fun getNotificationSettings(token: String): ProfileNotificationSettings {
        val response = httpClient.get("$baseUrl/api/profile/notifications") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ProfileNotificationSettings)
    }

    suspend fun saveNotificationSettings(token: String, payload: ProfileNotificationSettings): ProfileNotificationSettings {
        val response = httpClient.put("$baseUrl/api/profile/notifications") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ProfileNotificationSettings)
    }
}
