package com.aura.core.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object I18n {
    var language by mutableStateOf(AppLanguage.RU)

    private val ruMap: Map<String, String> by lazy { load("i18n/ru.properties") }
    private val enMap: Map<String, String> by lazy { load("i18n/en.properties") }

    fun toggleLanguage() {
        language = if (language == AppLanguage.RU) AppLanguage.EN else AppLanguage.RU
    }

    fun t(key: String, fallback: String): String {
        val active = when (language) {
            AppLanguage.RU -> ruMap
            AppLanguage.EN -> enMap
        }
        return active[key] ?: fallback
    }

    private fun load(path: String): Map<String, String> {
        val content = readResourceText(path) ?: return emptyMap()
        return parseProperties(content)
    }

    private fun parseProperties(raw: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) return@forEach
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                    .replace("\\n", "\n")
                if (key.isNotEmpty()) out[key] = value
            }
        return out
    }
}

internal expect fun readResourceText(path: String): String?
