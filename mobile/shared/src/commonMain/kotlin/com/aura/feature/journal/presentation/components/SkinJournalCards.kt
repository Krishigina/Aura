package com.aura.feature.journal.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.domain.model.SkinJournalProcedureEntry
import com.aura.core.domain.model.SkinJournalReminder
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReading
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.journal.SkinJournalDateEntries
import com.aura.feature.journal.activeTodayReminders
import com.aura.feature.journal.displayDate
import com.aura.feature.journal.displayTime
import com.aura.feature.journal.hydrationScaleInterpretation
import com.aura.feature.journal.journalZoneLabel
import com.aura.feature.journal.moisturePercentInterpretation
import com.aura.feature.journal.oilinessScaleInterpretation
import com.aura.feature.journal.procedureCountText
import com.aura.feature.journal.sensorCountText
import com.aura.feature.journal.softnessScaleInterpretation

@Composable
fun JournalSummaryCard(
    journal: SkinJournalResponse,
    onAddProcedure: () -> Unit,
    onAddSensor: () -> Unit,
) {
    GlassCard {
        Text("Сегодня в журнале", color = MaterialTheme.aura.journal.slate800, fontSize = auraTokenSp(20f), fontWeight = FontWeight.Bold)
        Text("Процедуры и замеры кожи в одном месте", color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(14f))
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
            StatPill(journal.procedures.size.procedureCountText(), "в журнале", MaterialTheme.aura.journal.procedureAccent, MaterialTheme.aura.journal.procedureSoft, Modifier.weight(1f))
            StatPill(journal.sensor_readings.size.sensorCountText(), "с датчика", MaterialTheme.aura.journal.sensorAccent, MaterialTheme.aura.journal.sensorSoft, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
            SoftActionButton(
                text = "+ Процедура",
                background = MaterialTheme.aura.journal.neutralSoft,
                content = MaterialTheme.aura.journal.slate700,
                accent = MaterialTheme.aura.journal.procedureAccent,
                onClick = onAddProcedure,
                modifier = Modifier.weight(1f),
            )
            SoftActionButton(
                text = "+ Замер",
                background = MaterialTheme.aura.journal.neutralSoft,
                content = MaterialTheme.aura.journal.slate700,
                accent = MaterialTheme.aura.journal.sensorAccent,
                onClick = onAddSensor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun JournalModeSwitch(selectedMode: String, onModeSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(auraTokenDp(22f)))
            .background(Color.White.copy(alpha = auraTokenAlpha(0.48f)))
            .border(auraTokenDp(1f), Color.White.copy(alpha = auraTokenAlpha(0.62f)), RoundedCornerShape(auraTokenDp(22f)))
            .padding(auraTokenDp(6f)),
        horizontalArrangement = Arrangement.spacedBy(auraTokenDp(6f)),
    ) {
        JournalModeButton("Лента", selectedMode == "feed", MaterialTheme.aura.journal.neutralSoft, MaterialTheme.aura.journal.neutral, Modifier.weight(1f)) {
            onModeSelected("feed")
        }
        JournalModeButton("Календарь", selectedMode == "calendar", MaterialTheme.aura.journal.neutralSoft, MaterialTheme.aura.journal.neutral, Modifier.weight(1f)) {
            onModeSelected("calendar")
        }
    }
}

@Composable
fun JournalModeButton(
    text: String,
    selected: Boolean,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(auraTokenDp(42f)),
        onClick = onClick,
        shape = RoundedCornerShape(auraTokenDp(17f)),
        color = if (selected) background else Color.Transparent,
        border = if (selected) BorderStroke(auraTokenDp(1f), accent.copy(alpha = auraTokenAlpha(0.34f))) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (selected) MaterialTheme.aura.journal.slate800 else MaterialTheme.aura.journal.slate500, fontWeight = FontWeight.Bold, fontSize = auraTokenSp(14f))
        }
    }
}

@Composable
fun SoftActionButton(
    text: String,
    background: Color,
    content: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(auraTokenDp(56f)),
        onClick = onClick,
        shape = RoundedCornerShape(auraTokenDp(18f)),
        color = background,
        border = BorderStroke(auraTokenDp(1f), accent.copy(alpha = auraTokenAlpha(0.35f))),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = content, fontWeight = FontWeight.ExtraBold, fontSize = auraTokenSp(15f))
        }
    }
}

@Composable
fun ActiveRemindersCard(
    reminders: List<SkinJournalReminder>,
    onAction: (String, String) -> Unit,
) {
    val activeReminders = activeTodayReminders(reminders)
    if (activeReminders.isEmpty()) return

    GlassCard {
        SectionTitle("Напоминания", "Активные задачи на сегодня")
        Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
            activeReminders.forEach { reminder ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(auraTokenDp(18f)))
                        .background(Color.White.copy(alpha = auraTokenAlpha(0.48f)))
                        .border(auraTokenDp(1f), Color.White.copy(alpha = auraTokenAlpha(0.62f)), RoundedCornerShape(auraTokenDp(18f)))
                        .padding(auraTokenDp(12f)),
                    verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(auraTokenDp(10f)).clip(CircleShape).background(MaterialTheme.aura.journal.neutral))
                        Spacer(Modifier.width(auraTokenDp(10f)))
                        Text(reminder.title, color = MaterialTheme.aura.journal.slate800, fontWeight = FontWeight.Bold, fontSize = auraTokenSp(15f))
                    }
                    Text(displayDate(reminder.due_at), color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(12f))
                    Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
                        SoftActionButton(
                            text = "Выполнено",
                            background = MaterialTheme.aura.journal.neutralSoft,
                            content = MaterialTheme.aura.journal.slate700,
                            accent = MaterialTheme.aura.journal.procedureAccent,
                            onClick = { onAction(reminder.id, "done") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { onAction(reminder.id, "skip") },
                            modifier = Modifier.weight(1f),
                        ) { Text("Пропустить", fontSize = auraTokenSp(12f), color = MaterialTheme.aura.journal.slate500) }
                    }
                    OutlinedButton(
                        onClick = { onAction(reminder.id, "reschedule") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Завтра", fontSize = auraTokenSp(12f), color = MaterialTheme.aura.journal.slate500) }
                }
            }
        }
    }
}

@Composable
fun LatestProceduresCard(procedures: List<SkinJournalProcedureEntry>) {
    GlassCard {
        SectionTitle("Последние процедуры", "Новые записи сверху")
        if (procedures.isEmpty()) {
            EmptyText("Пока нет процедур. Добавьте первую запись после визита.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
                procedures.forEach { procedure ->
                    SoftRow(
                        title = procedure.procedure_name.ifBlank { "Процедура" },
                        subtitle = listOfNotNull(
                            displayDate(procedure.performed_at),
                            procedure.zones.takeIf { it.isNotEmpty() }?.joinToString { journalZoneLabel(it) },
                        ).joinToString(" • "),
                        accent = MaterialTheme.aura.journal.procedureAccent,
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedDateEntriesCard(entries: SkinJournalDateEntries) {
    GlassCard {
        SectionTitle("Записи выбранного дня", "Процедуры и замеры")
        if (entries.procedures.isEmpty() && entries.sensorReadings.isEmpty()) {
            EmptyText("Для этой даты пока нет записей.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
                entries.procedures.forEach { procedure ->
                    SoftRow(
                        title = procedure.procedure_name.ifBlank { "Процедура" },
                        subtitle = procedure.zones.joinToString { journalZoneLabel(it) }.ifBlank { displayTime(procedure.performed_at) },
                        accent = MaterialTheme.aura.journal.procedureAccent,
                    )
                }
                entries.sensorReadings.forEach { reading ->
                    val moistureStatus = moisturePercentInterpretation(reading.percent_value)
                    val hydrationStatus = hydrationScaleInterpretation(reading.hydration)
                    val oilinessStatus = oilinessScaleInterpretation(reading.oiliness)
                    val softnessStatus = softnessScaleInterpretation(reading.softness)
                    SoftRow(
                        title = "Замер ${journalZoneLabel(reading.zone)}",
                        subtitle = "% содержания влаги: ${reading.percent_value}% ($moistureStatus)\n" +
                            "Влага: ${reading.hydration}/5 ($hydrationStatus)\n" +
                            "Жирность: ${reading.oiliness}/5 ($oilinessStatus)\n" +
                            "Мягкость: ${reading.softness}/5 ($softnessStatus)\n" +
                            displayTime(reading.measured_at),
                        accent = MaterialTheme.aura.journal.sensorAccent,
                    )
                }
            }
        }
    }
}

@Composable
fun SensorReadingsCard(readings: List<SkinJournalSensorReading>) {
    GlassCard {
        SectionTitle("Последние замеры", "% содержания влаги, влага, жирность, мягкость")
        if (readings.isEmpty()) {
            EmptyText("Добавьте замер, чтобы увидеть динамику датчика.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
                readings.forEach { reading ->
                    val moistureStatus = moisturePercentInterpretation(reading.percent_value)
                    val hydrationStatus = hydrationScaleInterpretation(reading.hydration)
                    val oilinessStatus = oilinessScaleInterpretation(reading.oiliness)
                    val softnessStatus = softnessScaleInterpretation(reading.softness)
                    SoftRow(
                        title = "${journalZoneLabel(reading.zone)} • ${reading.percent_value}%",
                        subtitle = "% содержания влаги: ${reading.percent_value}% ($moistureStatus)\n" +
                            "Влага: ${reading.hydration}/5 ($hydrationStatus)\n" +
                            "Жирность: ${reading.oiliness}/5 ($oilinessStatus)\n" +
                            "Мягкость: ${reading.softness}/5 ($softnessStatus)",
                        accent = MaterialTheme.aura.journal.sensorAccent,
                    )
                }
            }
        }
    }
}
