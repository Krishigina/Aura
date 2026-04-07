package com.aura.core.data.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class SkinPassport(
    val completedAtEpochMillis: Long,
    val answers: Map<String, List<String>>
)

object SkinPassportManager {
    var passport by mutableStateOf<SkinPassport?>(null)
        private set

    fun save(answers: Map<String, List<String>>) {
        passport = SkinPassport(
            completedAtEpochMillis = System.currentTimeMillis(),
            answers = answers
        )
    }

    fun clear() {
        passport = null
    }

    fun isCompleted(): Boolean = passport != null
}
