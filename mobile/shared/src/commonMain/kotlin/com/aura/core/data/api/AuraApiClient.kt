package com.aura.core.data.api

import com.aura.core.domain.model.AuthResponse
import com.aura.core.domain.model.BackendUser
import com.aura.core.domain.model.LoginRequest
import com.aura.core.domain.model.Product
import com.aura.core.domain.model.RegisterRequest
import com.aura.core.domain.model.SkinPassportRequest
import com.aura.core.domain.model.SkinPassportResponse
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuraApiClient(private val baseUrl: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // Manual JSON parsing to handle corrupted data gracefully
    suspend fun getProducts(): List<Product> {
        return try {
            val response = httpClient.get("$baseUrl/api/products")
            val responseText = response.bodyAsText()
            
            // Try to parse as Product list, if fails use manual parser
            try {
                json.decodeFromString<List<Product>>(responseText)
            } catch (e: Exception) {
                parseProductsManually(responseText)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseProductsManually(text: String): List<Product> {
        return try {
            val element = json.parseToJsonElement(text)
            if (element is JsonArray) {
                element.mapNotNull { item ->
                    try {
                        if (item is JsonObject) {
                            Product(
                                id = item["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                                name = item["name"]?.jsonPrimitive?.content,
                                brand = item["brand"]?.jsonPrimitive?.content,
                                what_is_it = item["what_is_it"]?.jsonPrimitive?.content,
                                product_type = item["product_type"]?.jsonPrimitive?.content,
                                for_whom = item["for_whom"]?.jsonPrimitive?.content,
                                purpose = item["purpose"]?.let { 
                                    if (it is JsonArray) {
                                        it.mapNotNull { p -> p.jsonPrimitive?.content }
                                    } else null
                                },
                                skin_type = item["skin_type"]?.let {
                                    if (it is JsonArray) {
                                        it.mapNotNull { p -> p.jsonPrimitive?.content }
                                    } else null
                                },
                                application_time = item["application_time"]?.jsonPrimitive?.content,
                                area = item["area"]?.jsonPrimitive?.content,
                                active_ingredient = item["active_ingredient"]?.jsonPrimitive?.content,
                                volume = item["volume"]?.jsonPrimitive?.content,
                                segment = item["segment"]?.jsonPrimitive?.content,
                                composition = item["composition"]?.jsonPrimitive?.content,
                                application_info = item["application_info"]?.jsonPrimitive?.content,
                                country = item["country"]?.jsonPrimitive?.content,
                                country_origin = item["country_origin"]?.jsonPrimitive?.content,
                                manufacturer = item["manufacturer"]?.jsonPrimitive?.content,
                                description = item["description"]?.jsonPrimitive?.content,
                                imageUrl = item["imageUrl"]?.jsonPrimitive?.content,
                                price = item["price"]?.jsonPrimitive?.content,
                                currency = item["currency"]?.jsonPrimitive?.content,
                                category = item["category"]?.jsonPrimitive?.content,
                                desc = item["desc"]?.jsonPrimitive?.content
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(extractErrorMessage(responseText, response.status))
        }

        return parseAuthResponse(responseText)
    }

    suspend fun register(name: String, email: String, password: String, nickname: String = "", phone: String? = null): AuthResponse {
        val response = httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(name, email, password, nickname = nickname, phone = phone))
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(extractErrorMessage(responseText, response.status))
        }

        return parseAuthResponse(responseText)
    }
    
    suspend fun getMe(token: String): BackendUser {
        return httpClient.get("$baseUrl/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body()
    }

    suspend fun getSkinPassport(token: String): SkinPassportResponse? {
        val response = httpClient.get("$baseUrl/api/profile/skin-passport") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status == HttpStatusCode.NotFound) return null

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(extractErrorMessage(responseText, response.status))
        }

        if (responseText.isBlank()) return null
        return json.decodeFromString<SkinPassportResponse>(responseText)
    }

    suspend fun saveSkinPassport(
        token: String,
        answers: Map<String, List<String>>,
        completedAtEpochMillis: Long = System.currentTimeMillis()
    ): SkinPassportResponse {
        val response = httpClient.put("$baseUrl/api/profile/skin-passport") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                SkinPassportRequest(
                    answers = answers,
                    completed_at_epoch_millis = completedAtEpochMillis
                )
            )
        }

        val responseText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException(extractErrorMessage(responseText, response.status))
        }

        return json.decodeFromString<SkinPassportResponse>(responseText)
    }

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