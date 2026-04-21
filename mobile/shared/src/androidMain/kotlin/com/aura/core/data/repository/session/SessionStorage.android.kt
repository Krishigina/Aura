package com.aura.core.data.repository.session

import android.content.Context
import android.content.SharedPreferences

actual object SessionStorage {
    private const val PREFS_NAME = "aura_session"
    private const val TOKEN_KEY = "access_token"

    private var preferences: SharedPreferences? = null

    actual fun init(context: Any?) {
        val androidContext = context as? Context ?: return
        preferences = androidContext.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getToken(): String? {
        return preferences?.getString(TOKEN_KEY, null)?.takeIf { it.isNotBlank() }
    }

    actual fun setToken(token: String) {
        preferences?.edit()?.putString(TOKEN_KEY, token)?.apply()
    }

    actual fun clearToken() {
        preferences?.edit()?.remove(TOKEN_KEY)?.apply()
    }
}
