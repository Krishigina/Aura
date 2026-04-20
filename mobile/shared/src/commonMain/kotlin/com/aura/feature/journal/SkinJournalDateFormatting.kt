package com.aura.feature.journal

import androidx.compose.runtime.getValue
import kotlinx.datetime.Clock

fun currentJournalTimestamp(): String = Clock.System.now().toString()

fun currentJournalDateKey(): String = currentJournalTimestamp().take(10)

fun currentJournalDateDisplay(): String = currentJournalDateKey().isoDateToDisplayDate()

fun firstDayOffsetMonday(year: Int, month: Int): Int {
    val adjustedMonth = if (month < 3) month + 12 else month
    val adjustedYear = if (month < 3) year - 1 else year
    val k = adjustedYear % 100
    val j = adjustedYear / 100
    val h = (1 + (13 * (adjustedMonth + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
    val sundayZero = (h + 6) % 7
    return (sundayZero + 6) % 7
}

fun monthTitle(year: Int, month: Int): String = "${monthName(month)} $year"

fun monthName(month: Int): String = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
).getOrElse(month - 1) { "Месяц" }

fun displayDate(timestamp: String): String = displayJournalDate(timestamp)

fun displayTime(timestamp: String): String = timestamp.substringOrNull(11, 16) ?: displayDate(timestamp)
