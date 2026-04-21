package com.aura.core.domain.repository

import com.aura.core.domain.model.BackendUser
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    fun loadSession(): Boolean
    fun token(): String?
    fun requireToken(message: String = "Нужна авторизация"): String
    fun user(): BackendUser?
    fun authState(): StateFlow<Boolean>
    fun setToken(token: String)
    fun setUser(user: BackendUser)
    fun clear()
    fun isLoggedIn(): Boolean
}
