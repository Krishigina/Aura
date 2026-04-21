package com.aura.core.data.api

import com.aura.core.data.api.client.AuthNetworkClient
import com.aura.core.data.api.client.ChatNetworkClient
import com.aura.core.data.api.client.GeneralContentNetworkClient
import com.aura.core.data.api.client.ProductNetworkClient
import com.aura.core.data.api.client.ProfileNetworkClient
import com.aura.core.data.api.client.RecommendationsNetworkClient
import com.aura.core.data.api.client.SkinJournalNetworkClient
import com.aura.core.domain.model.AuthResponse
import com.aura.core.domain.model.BackendUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun homeStatusCoordinateQuery(latitude: Double?, longitude: Double?): String? {
    if (latitude == null || longitude == null) return null
    return "latitude=$latitude&longitude=$longitude"
}

internal class AuraNetworkClients(private val baseUrl: String) {

    private companion object {
        const val CHAT_RAG_TIMEOUT_MILLIS = 120_000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = CHAT_RAG_TIMEOUT_MILLIS
            socketTimeoutMillis = CHAT_RAG_TIMEOUT_MILLIS
            connectTimeoutMillis = 30_000L
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    val authNetworkClient = AuthNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        parseAuthResponse = ::parseAuthResponse,
        extractErrorMessage = ::extractErrorMessage,
    )

    val productNetworkClient = ProductNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        extractErrorMessage = ::extractErrorMessage,
    )

    val profileNetworkClient = ProfileNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        extractErrorMessage = ::extractErrorMessage,
    )

    val skinJournalNetworkClient = SkinJournalNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        extractErrorMessage = ::extractErrorMessage,
    )

    val chatNetworkClient = ChatNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        extractErrorMessage = ::extractErrorMessage,
    )

    val generalContentNetworkClient = GeneralContentNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        extractErrorMessage = ::extractErrorMessage,
    )

    val recommendationsNetworkClient = RecommendationsNetworkClient(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
        extractErrorMessage = ::extractErrorMessage,
    )

    private fun parseAuthResponse(responseText: String): AuthResponse {
        val root = json.decodeFromString<JsonElement>(responseText).jsonObject
        val data = root["data"]?.jsonObject

        fun fromRootOrData(key: String): JsonElement? = root[key] ?: data?.get(key)

        val token = fromRootOrData("access_token")?.jsonPrimitive?.contentOrNull
            ?: fromRootOrData("token")?.jsonPrimitive?.contentOrNull
        val tokenType = fromRootOrData("token_type")?.jsonPrimitive?.contentOrNull ?: "bearer"
        val userElement = fromRootOrData("user")

        if (token.isNullOrBlank() || userElement == null) {
            throw IllegalStateException("Некорректный ответ сервера авторизации")
        }

        val user = json.decodeFromJsonElement<BackendUser>(userElement)
        return AuthResponse(access_token = token, token_type = tokenType, user = user)
    }

    private fun extractErrorMessage(responseText: String, status: HttpStatusCode): String {
        val detail = runCatching {
            val root = json.decodeFromString<JsonElement>(responseText).jsonObject
            root["detail"]?.jsonPrimitive?.contentOrNull
                ?: root["message"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()

        return detail ?: "Ошибка авторизации (${status.value})"
    }
}
