package com.aura.feature.profile.presentation.components.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ReminderFrequency
import com.aura.core.domain.model.ReminderPreference
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.profile.presentation.components.form.FrequencyButtonInline
import com.aura.feature.profile.presentation.components.form.RoutineTextField

@Composable
fun NotificationsInlineCard(
    settings: ProfileNotificationSettings,
    loading: Boolean,
    saving: Boolean,
    error: String?,
    success: String?,
    onToggleAll: (Boolean) -> Unit,
    onRoutineFrequencyChange: (ReminderFrequency) -> Unit,
    onRoutineWeekdayChange: (Int) -> Unit,
    onRoutineMonthDayChange: (Int) -> Unit,
    onRoutineTimeChange: (String) -> Unit,
    onJournalFrequencyChange: (ReminderFrequency) -> Unit,
    onJournalWeekdayChange: (Int) -> Unit,
    onJournalMonthDayChange: (Int) -> Unit,
    onJournalTimeChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(10f)),
    ) {
        error?.let { Text(it, color = auraHex(0xFFDC2626), fontSize = auraTokenSp(12f)) }
        success?.let { Text(it, color = MaterialTheme.aura.profile.mint500, fontSize = auraTokenSp(12f)) }
        if (loading) {
            Text("Загрузка настроек уведомлений...", color = auraHex(0xFF475569), fontSize = auraTokenSp(13f))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(auraTokenDp(14f)))
                    .background(Color.White.copy(alpha = auraTokenAlpha(0.92f)))
                    .border(auraTokenDp(1f), MaterialTheme.aura.profile.mint200, RoundedCornerShape(auraTokenDp(14f)))
                    .padding(horizontal = auraTokenDp(12f), vertical = auraTokenDp(10f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Все уведомления", color = auraHex(0xFF0F172A), fontSize = auraTokenSp(13f), fontWeight = FontWeight.SemiBold)
                    Text(if (settings.disable_all) "Отключены" else "Включены", color = auraHex(0xFF64748B), fontSize = auraTokenSp(12f))
                }
                Switch(checked = settings.disable_all, onCheckedChange = onToggleAll, enabled = !saving)
            }

            NotificationDomainInlineEditor(
                title = "Рутина",
                preference = settings.routine,
                enabled = !settings.disable_all && !saving,
                onFrequencyChange = onRoutineFrequencyChange,
                onWeekdayChange = onRoutineWeekdayChange,
                onMonthDayChange = onRoutineMonthDayChange,
                onReminderTimeChange = onRoutineTimeChange,
                accent = MaterialTheme.aura.profile.themePink,
                accentSoft = MaterialTheme.aura.profile.themePinkSoft,
                accentBorder = MaterialTheme.aura.profile.coral200,
            )
            NotificationDomainInlineEditor(
                title = "Журнал кожи",
                preference = settings.journal,
                enabled = !settings.disable_all && !saving,
                onFrequencyChange = onJournalFrequencyChange,
                onWeekdayChange = onJournalWeekdayChange,
                onMonthDayChange = onJournalMonthDayChange,
                onReminderTimeChange = onJournalTimeChange,
                accent = MaterialTheme.aura.profile.themeLavender,
                accentSoft = MaterialTheme.aura.profile.themeLavenderSoft,
                accentBorder = MaterialTheme.aura.profile.themeLavender,
            )

            Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().height(auraTokenDp(48f)), shape = RoundedCornerShape(auraTokenDp(14f)), border = BorderStroke(auraTokenDp(1f), MaterialTheme.aura.profile.sky500), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.aura.profile.sky500, contentColor = Color.White)) {
                Text(if (saving) "Сохранение..." else "Сохранить уведомления", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NotificationDomainInlineEditor(
    title: String,
    preference: ReminderPreference,
    enabled: Boolean,
    onFrequencyChange: (ReminderFrequency) -> Unit,
    onWeekdayChange: (Int) -> Unit,
    onMonthDayChange: (Int) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    accent: Color,
    accentSoft: Color,
    accentBorder: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(auraTokenDp(14f))).background(accentSoft).border(auraTokenDp(1f), accentBorder, RoundedCornerShape(auraTokenDp(14f))).padding(auraTokenDp(12f)),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f)),
    ) {
        Text(title, color = auraHex(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = auraTokenSp(13f))
        Text("Частота", color = accent, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            FrequencyButtonInline("Никогда", preference.frequency == ReminderFrequency.NONE, enabled, accent = accent, accentBorder = accentBorder) { onFrequencyChange(ReminderFrequency.NONE) }
            FrequencyButtonInline("Ежедневно", preference.frequency == ReminderFrequency.DAILY, enabled, accent = accent, accentBorder = accentBorder) { onFrequencyChange(ReminderFrequency.DAILY) }
            FrequencyButtonInline("Еженедельно", preference.frequency == ReminderFrequency.WEEKLY, enabled, accent = accent, accentBorder = accentBorder) { onFrequencyChange(ReminderFrequency.WEEKLY) }
            FrequencyButtonInline("Ежемесячно", preference.frequency == ReminderFrequency.MONTHLY, enabled, accent = accent, accentBorder = accentBorder) { onFrequencyChange(ReminderFrequency.MONTHLY) }
        }
        if (preference.frequency != ReminderFrequency.NONE) {
            if (preference.frequency == ReminderFrequency.WEEKLY) {
                Text("День недели", color = accent, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { idx, label ->
                        val value = idx + 1
                        FrequencyButtonInline(label, preference.weekday == value, enabled, accent = accent, accentBorder = accentBorder) { onWeekdayChange(value) }
                    }
                }
            }
            if (preference.frequency == ReminderFrequency.MONTHLY) {
                Text("День месяца", color = accent, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    (1..31).forEach { value ->
                        FrequencyButtonInline(value.toString(), preference.month_day == value, enabled, accent = accent, accentBorder = accentBorder) { onMonthDayChange(value) }
                    }
                }
            }
            RoutineTextField(
                value = preference.reminder_time.orEmpty(),
                onValueChange = { if (enabled) onReminderTimeChange(it) },
                label = "Время напоминания",
                placeholder = "__:__",
                keyboardType = KeyboardType.Number,
                accent = accent,
            )
        } else {
            Text("Напоминание отключено", color = auraHex(0xFF94A3B8), fontSize = auraTokenSp(12f))
        }
    }
}
