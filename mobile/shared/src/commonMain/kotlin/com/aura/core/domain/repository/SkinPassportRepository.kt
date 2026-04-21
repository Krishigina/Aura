package com.aura.core.domain.repository

interface SkinPassportRepository {
    fun answers(): Map<String, List<String>>
    fun save(answers: Map<String, List<String>>)
    fun clear()
    fun isCompleted(): Boolean
}
