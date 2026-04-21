package com.aura.core.data.api.client

import com.aura.core.data.api.model.RecommendationFavoriteRequest
import com.aura.core.data.api.model.RecommendationFavoriteResponse
import com.aura.core.data.api.model.RecommendationFavoritesResponse
import com.aura.core.data.api.model.RecommendationResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject

internal class RecommendationsNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val extractErrorMessage: (String, HttpStatusCode) -> String,
) {
    suspend fun generateRecommendation(token: String): RecommendationResponse {
        val response = httpClient.post("$baseUrl/api/recommendations/generate") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun saveFavorite(token: String, recommendation: RecommendationResponse): RecommendationFavoriteResponse {
        val response = httpClient.post("$baseUrl/api/recommendations/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(RecommendationFavoriteRequest(recommendation))
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun getFavorites(token: String): RecommendationFavoritesResponse {
        val response = httpClient.get("$baseUrl/api/recommendations/favorites") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }
}
