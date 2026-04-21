package com.aura.core.data.api.client

import com.aura.core.data.repository.session.TokenManager
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal suspend fun HttpResponse.bodyTextOrThrow(
    extractErrorMessage: (String, HttpStatusCode) -> String,
): String {
    val responseText = bodyAsText()
    if (!status.isSuccess()) {
        if (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) {
            TokenManager.clearToken()
        }
        throw IllegalStateException(extractErrorMessage(responseText, status))
    }
    return responseText
}

internal inline fun <reified T> Json.decodeOrDefault(responseText: String, defaultValue: () -> T): T {
    if (responseText.isBlank()) return defaultValue()
    return decodeFromString(responseText)
}
