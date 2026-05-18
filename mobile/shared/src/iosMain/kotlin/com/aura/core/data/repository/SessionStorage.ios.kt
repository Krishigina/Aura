package com.aura.core.data.repository

actual object SessionStorage {
    private var token: String? = null

    actual fun init(context: Any?) {
    }

    actual fun getToken(): String? = token?.takeIf { it.isNotBlank() }

    actual fun setToken(token: String) {
        this.token = token
    }

    actual fun clearToken() {
        token = null
    }
}
