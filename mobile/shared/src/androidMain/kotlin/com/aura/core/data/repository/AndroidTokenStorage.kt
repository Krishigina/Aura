package com.aura.core.data.repository

import android.content.Context

class AndroidTokenStorage(context: Context) : TokenManager.TokenStorage {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun setToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    private companion object {
        const val PREFS_NAME = "aura_auth_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}
