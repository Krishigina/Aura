package com.aura.core.data.api.client

import com.aura.core.data.api.homeStatusCoordinateQuery
import com.aura.core.data.api.model.DiagnosticsSummaryResponse
import com.aura.core.data.api.model.HomeFeedResponse
import com.aura.core.data.api.model.HomeStatusResponse
import com.aura.core.data.api.model.SurveySchemaResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

internal class GeneralContentNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val extractErrorMessage: (String, HttpStatusCode) -> String,
) {
    suspend fun getHomeFeed(token: String): HomeFeedResponse {
        val response = httpClient.get("$baseUrl/api/home/feed") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::HomeFeedResponse)
    }

    suspend fun getDiagnosticsSummary(token: String): DiagnosticsSummaryResponse {
        val response = httpClient.get("$baseUrl/api/diagnostics/summary") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::DiagnosticsSummaryResponse)
    }

    suspend fun getHomeStatus(token: String, latitude: Double? = null, longitude: Double? = null): HomeStatusResponse {
        val coordinateQuery = homeStatusCoordinateQuery(latitude, longitude)
        val url = if (coordinateQuery == null) {
            "$baseUrl/api/home/status"
        } else {
            "$baseUrl/api/home/status?$coordinateQuery"
        }
        val response = httpClient.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::HomeStatusResponse)
    }

    suspend fun getSurveySchema(token: String): SurveySchemaResponse {
        val response = httpClient.get("$baseUrl/api/survey/schema") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::SurveySchemaResponse)
    }
}
