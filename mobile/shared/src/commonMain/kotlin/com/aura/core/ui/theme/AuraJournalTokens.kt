package com.aura.core.ui.theme

import androidx.compose.ui.graphics.Color

data class AuraJournalTokens(
    val background: Color,
    val neutral: Color,
    val neutralSoft: Color,
    val gradientTop: Color,
    val gradientMid: Color,
    val gradientBottom: Color,
    val procedureAccent: Color,
    val procedureSoft: Color,
    val sensorAccent: Color,
    val sensorSoft: Color,
    val procedureSoftAlt: Color,
    val sensorSoftAlt: Color,
    val slate800: Color,
    val slate700: Color,
    val slate500: Color,
    val slate400: Color,
    val errorRed: Color,
)

fun defaultAuraJournalTokens() = AuraJournalTokens(
    background = auraHex(0xFFF4F7FE),
    neutral = auraHex(0xFFCBD5E1),
    neutralSoft = auraHex(0xFFF1F5F9),
    gradientTop = auraHex(0xFFEFF6FF),
    gradientMid = auraHex(0xFFF8FAFC),
    gradientBottom = auraHex(0xFFFFF1F2),
    procedureAccent = auraHex(0xFFF2778E),
    procedureSoft = auraHex(0xFFFFE3E9),
    sensorAccent = auraHex(0xFF5FA8FF),
    sensorSoft = auraHex(0xFFE2F0FF),
    procedureSoftAlt = auraHex(0xFFFFEDF1),
    sensorSoftAlt = auraHex(0xFFECF5FF),
    slate800 = auraHex(0xFF1E293B),
    slate700 = auraHex(0xFF334155),
    slate500 = auraHex(0xFF64748B),
    slate400 = auraHex(0xFF94A3B8),
    errorRed = auraHex(0xFFB91C1C),
)
