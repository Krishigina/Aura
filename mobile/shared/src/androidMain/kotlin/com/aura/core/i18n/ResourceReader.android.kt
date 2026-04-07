package com.aura.core.i18n

internal actual fun readResourceText(path: String): String? {
    val stream = object {}.javaClass.classLoader?.getResourceAsStream(path) ?: return null
    return stream.bufferedReader().use { it.readText() }
}
