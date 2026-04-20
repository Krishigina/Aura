package com.aura.feature.journal.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.journal.SkinJournalDayEvents
import com.aura.feature.journal.SkinJournalMonthEvents
import com.aura.feature.journal.firstDayOffsetMonday
import com.aura.feature.journal.journalDaysInMonth
import com.aura.feature.journal.monthName
import com.aura.feature.journal.pad2

@Composable
fun CalendarNavButton(text: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(auraTokenDp(40f))
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
        border = BorderStroke(auraTokenDp(1f), MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.45f))),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = MaterialTheme.aura.journal.slate700, fontSize = auraTokenSp(24f), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun YearNavButton(text: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(auraTokenDp(40f))
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        onClick = onClick,
        shape = RoundedCornerShape(auraTokenDp(16f)),
        color = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
        border = BorderStroke(auraTokenDp(1f), MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.45f))),
    ) {
        Box(modifier = Modifier.padding(horizontal = auraTokenDp(12f)), contentAlignment = Alignment.Center) {
            Text(text, color = MaterialTheme.aura.journal.slate700, fontSize = auraTokenSp(13f), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearPickerSheet(
    selectedYear: Int,
    selectedMonth: Int,
    eventMonths: Map<String, SkinJournalMonthEvents>,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val years = remember(selectedYear, eventMonths) {
        val eventYears = eventMonths.keys.mapNotNull { it.take(4).toIntOrNull() }
        val minYear = minOf(eventYears.minOrNull() ?: selectedYear, selectedYear) - 1
        val maxYear = maxOf(eventYears.maxOrNull() ?: selectedYear, selectedYear) + 1
        (maxYear downTo minYear).toList()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.aura.journal.background) {
        SheetContent(title = "Выберите год") {
            Text("Год покажет выбранный месяц и дни с процедурами или замерами", color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(13f))
            Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
                years.chunked(2).forEach { rowYears ->
                    Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth()) {
                        rowYears.forEach { year ->
                            val monthKey = "$year-${selectedMonth.pad2()}"
                            YearChoiceTile(
                                year = year,
                                selected = year == selectedYear,
                                events = eventMonths[monthKey] ?: SkinJournalMonthEvents(),
                                modifier = Modifier.weight(1f),
                                onClick = { onYearSelected(year) },
                            )
                        }
                        if (rowYears.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun YearChoiceTile(
    year: Int,
    selected: Boolean,
    events: SkinJournalMonthEvents,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hasEvents = events.procedureCount > 0 || events.sensorCount > 0
    Column(
        modifier = modifier
            .height(auraTokenDp(72f))
            .clip(RoundedCornerShape(auraTokenDp(18f)))
            .background(if (selected) MaterialTheme.aura.journal.neutralSoft else Color.White.copy(alpha = if (hasEvents) 0.62f else 0.38f))
            .border(auraTokenDp(1f), if (selected) MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.45f)) else Color.White.copy(alpha = auraTokenAlpha(0.62f)), RoundedCornerShape(auraTokenDp(18f)))
            .clickable { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "Год $year"
            }
            .padding(auraTokenDp(10f)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(year.toString(), color = if (hasEvents || selected) MaterialTheme.aura.journal.slate800 else MaterialTheme.aura.journal.slate400, fontSize = auraTokenSp(16f), fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(5f))) {
            if (events.procedureCount > 0) MonthEventBadge("П ${events.procedureCount}", MaterialTheme.aura.journal.procedureSoft, MaterialTheme.aura.journal.procedureAccent)
            if (events.sensorCount > 0) MonthEventBadge("З ${events.sensorCount}", MaterialTheme.aura.journal.sensorSoft, MaterialTheme.aura.journal.sensorAccent)
        }
    }
}

@Composable
fun MonthEventBadge(text: String, background: Color, border: Color) {
    Text(
        text = text,
        color = MaterialTheme.aura.journal.slate700,
        fontSize = auraTokenSp(10f),
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(auraTokenDp(999f)))
            .background(background)
            .border(auraTokenDp(1f), border.copy(alpha = auraTokenAlpha(0.35f)), RoundedCornerShape(auraTokenDp(999f)))
            .padding(horizontal = auraTokenDp(7f), vertical = auraTokenDp(2f)),
    )
}

@Composable
fun MonthCalendarCard(
    year: Int,
    month: Int,
    selectedDateKey: String,
    todayDateKey: String,
    eventDays: Map<String, SkinJournalDayEvents>,
    onDateSelected: (String) -> Unit,
    onYearClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val daysInMonth = journalDaysInMonth(year, month)
    val leadingBlanks = firstDayOffsetMonday(year, month)
    val cells = List(leadingBlanks) { 0 } + (1..daysInMonth).toList()
    val rows = (cells.size + 6) / 7

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarNavButton("‹", "Предыдущий месяц", onPreviousMonth)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Календарь", color = MaterialTheme.aura.journal.slate800, fontSize = auraTokenSp(18f), fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(auraTokenDp(6f))) {
                    Text(monthName(month), color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(13f))
                    Surface(
                        onClick = onYearClick,
                        shape = RoundedCornerShape(auraTokenDp(999f)),
                        color = MaterialTheme.aura.journal.neutralSoft.copy(alpha = auraTokenAlpha(0.68f)),
                        border = BorderStroke(auraTokenDp(1f), MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.62f))),
                    ) {
                        Text(
                            year.toString(),
                            modifier = Modifier.padding(horizontal = auraTokenDp(9f), vertical = auraTokenDp(2f)),
                            color = MaterialTheme.aura.journal.slate700,
                            fontSize = auraTokenSp(12f),
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            CalendarNavButton("›", "Следующий месяц", onNextMonth)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf<String>("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { dayName ->
                Text(dayName, color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(auraTokenDp(8f)))
        Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(6f)), modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val day = cells.getOrNull(row * 7 + col) ?: 0
                        if (day == 0) {
                            Box(modifier = Modifier.weight(1f).height(auraTokenDp(44f)))
                        } else {
                            val key = "$year-${month.pad2()}-${day.pad2()}"
                            val events = eventDays[key]
                            val selected = selectedDateKey.isNotBlank() && key == selectedDateKey
                            val today = key == todayDateKey
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(auraTokenDp(44f))
                                    .clip(RoundedCornerShape(auraTokenDp(14f)))
                                    .background(if (selected) MaterialTheme.aura.journal.neutralSoft else Color.White.copy(alpha = auraTokenAlpha(0.48f)))
                                    .border(
                                        auraTokenDp(1f),
                                        when {
                                            selected -> MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.45f))
                                            today -> MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.65f))
                                            else -> Color.White.copy(alpha = auraTokenAlpha(0.62f))
                                        },
                                        RoundedCornerShape(auraTokenDp(14f)),
                                    )
                                    .clickable { onDateSelected(key) }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Дата $key"
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(day.toString(), color = MaterialTheme.aura.journal.slate700, fontSize = auraTokenSp(13f), fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(3f))) {
                                    if ((events?.procedureCount ?: 0) > 0) EventMark("П", MaterialTheme.aura.journal.procedureSoft, MaterialTheme.aura.journal.procedureAccent)
                                    if ((events?.sensorCount ?: 0) > 0) EventMark("З", MaterialTheme.aura.journal.sensorSoft, MaterialTheme.aura.journal.sensorAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
