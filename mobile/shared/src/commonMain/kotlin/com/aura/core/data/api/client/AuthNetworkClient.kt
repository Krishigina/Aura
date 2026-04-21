package com.aura.core.data.api.client

import com.aura.core.data.api.model.LoginRequest
import com.aura.core.data.api.model.ProfileAccountUpdateRequest
import com.aura.core.data.api.model.ProfileDeleteRequest
import com.aura.core.data.api.model.ProfilePasswordUpdateRequest
import com.aura.core.data.api.model.RegisterRequest
import com.aura.core.domain.model.AuthResponse
import com.aura.core.domain.model.BackendUser
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

internal class AuthNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val parseAuthResponse: (String) -> AuthResponse,
    private val extractErrorMessage: (String, io.ktor.http.HttpStatusCode) -> String,
) {
    suspend fun login(email: String, password: String): AuthResponse {
        val response = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        return parseAuthResponse(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun register(name: String, email: String, password: String, nickname: String = ""): AuthResponse {
        val response = httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(name, email, password, nickname = nickname))
        }

        return parseAuthResponse(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun getMe(token: String): BackendUser {
        val response = httpClient.get("$baseUrl/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun updateProfileAccount(token: String, name: String, nickname: String?): BackendUser {
        val response = httpClient.patch("$baseUrl/api/profile/account") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ProfileAccountUpdateRequest(name = name, nickname = nickname))
        }
        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun updateProfilePassword(token: String, currentPassword: String, newPassword: String) {
        val response = httpClient.patch("$baseUrl/api/profile/password") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                ProfilePasswordUpdateRequest(
                    current_password = currentPassword,
                    new_password = newPassword,
                ),
            )
        }
        response.bodyTextOrThrow(extractErrorMessage)
    }

    suspend fun deleteProfileAccount(token: String, currentPassword: String) {
        val response = httpClient.delete("$baseUrl/api/profile/account") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ProfileDeleteRequest(current_password = currentPassword))
        }
        response.bodyTextOrThrow(extractErrorMessage)
    }
}
