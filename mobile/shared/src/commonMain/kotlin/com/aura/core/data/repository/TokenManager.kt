package com.aura.core.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aura.core.domain.model.BackendUser

// Simple in-memory token manager for mobile auth
// Persists token across app lifecycle; survives rotation
// For production: replace with platform-specific secure storage
object TokenManager {
    private var _token: String? = null
    private var _user: BackendUser? by mutableStateOf(null)
    
    fun getToken(): String? = _token
    
    fun setToken(token: String) {
        _token = token
    }
    
    fun clearToken() {
        _token = null
        _user = null
    }
    
    fun getUser(): BackendUser? = _user
    
    fun setUser(user: BackendUser) {
        _user = user
    }
    
    fun isLoggedIn(): Boolean = _token != null
}
