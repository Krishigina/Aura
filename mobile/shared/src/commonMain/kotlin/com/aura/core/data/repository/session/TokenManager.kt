package com.aura.core.data.repository.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.core.domain.model.BackendUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Simple in-memory token manager for mobile auth
// Persists token across app lifecycle; survives rotation
// For production: replace with platform-specific secure storage
object TokenManager {
    private var _token: String? = null
    private var _user: BackendUser? by mutableStateOf(null)
    private val _authState = MutableStateFlow(false)

    fun loadToken() {
        _token = SessionStorage.getToken()
        _authState.value = _token != null
    }

    fun getToken(): String? = _token

    fun authState(): StateFlow<Boolean> = _authState

    fun setToken(token: String) {
        _token = token
        SessionStorage.setToken(token)
        _authState.value = true
    }

    fun clearToken() {
        _token = null
        _user = null
        SessionStorage.clearToken()
        _authState.value = false
    }

    fun getUser(): BackendUser? = _user

    fun setUser(user: BackendUser) {
        _user = user
    }

    fun isLoggedIn(): Boolean = _authState.value
}
