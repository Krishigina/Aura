package com.aura.core.data.repository.session

import com.aura.core.domain.model.BackendUser
import com.aura.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.StateFlow

class TokenSessionRepository : SessionRepository {
    override fun loadSession(): Boolean {
        TokenManager.loadToken()
        return isLoggedIn() && !token().isNullOrBlank()
    }

    override fun token(): String? = TokenManager.getToken()

    override fun requireToken(message: String): String = token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException(message)

    override fun user(): BackendUser? = TokenManager.getUser()

    override fun authState(): StateFlow<Boolean> = TokenManager.authState()

    override fun setToken(token: String) = TokenManager.setToken(token)

    override fun setUser(user: BackendUser) = TokenManager.setUser(user)

    override fun clear() = TokenManager.clearToken()

    override fun isLoggedIn(): Boolean = TokenManager.isLoggedIn()
}
