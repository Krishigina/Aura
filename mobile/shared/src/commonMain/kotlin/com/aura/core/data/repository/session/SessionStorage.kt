package com.aura.core.data.repository.session

expect object SessionStorage {
    fun init(context: Any? = null)
    fun getToken(): String?
    fun setToken(token: String)
    fun clearToken()
}
