package com.aura.feature.profile.logic

fun formatMaskedTime(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.isEmpty() -> ""
        digits.length <= 2 -> digits
        else -> "${digits.substring(0, 2)}:${digits.substring(2)}"
    }
}

fun friendlyProfileError(message: String?): String? {
    val msg = message?.trim().orEmpty()
    if (msg.isEmpty()) return null
    if (msg.contains("not found", ignoreCase = true)) return "Данные временно недоступны"
    return msg
}
