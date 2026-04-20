package com.aura.feature.profile.presentation.components.routine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ReminderFrequency
import com.aura.core.domain.model.RoutineProductOption
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.profile.presentation.components.form.FrequencyButtonInline
import com.aura.feature.profile.presentation.components.form.RoutineTextField

@Composable
fun RoutineInlineCard(
    routineSteps: List<ProfileRoutineStep>,
    routineLoading: Boolean,
    routineSaving: Boolean,
    routineError: String?,
    routineSuccess: String?,
    onAddStep: () -> Unit,
    onProductQueryChange: (Int, String) -> Unit,
    routineSearchResults: Map<String, List<RoutineProductOption>>,
    routineSearchQueries: Map<String, String>,
    onClearProductQuery: (Int) -> Unit,
    onSelectProduct: (Int, RoutineProductOption) -> Unit,
    onFrequencyChange: (Int, ReminderFrequency) -> Unit,
    onWeekdayChange: (Int, Int) -> Unit,
    onMonthDayChange: (Int, Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    pendingRemoveStepId: String?,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(10f)),
    ) {
        routineError?.let { Text(it, color = auraHex(0xFFDC2626), fontSize = auraTokenSp(12f)) }
        routineSuccess?.let { Text(it, color = MaterialTheme.aura.profile.mint500, fontSize = auraTokenSp(12f)) }
        if (routineLoading) {
            Text("Загрузка рутины...", color = auraHex(0xFF475569), fontSize = auraTokenSp(13f))
        } else {
            routineSteps.forEachIndexed { index, step ->
                key(step.id) {
                    RoutineInlineStepEditor(
                        step = step,
                        index = index,
                        total = routineSteps.size,
                        query = routineSearchQueries[step.id].orEmpty(),
                        searchOptions = routineSearchResults[step.id].orEmpty(),
                        onClearQuery = { onClearProductQuery(index) },
                        removePending = pendingRemoveStepId == step.id,
                        onProductQueryChange = { onProductQueryChange(index, it) },
                        onSelectProduct = { onSelectProduct(index, it) },
                        onFrequencyChange = { onFrequencyChange(index, it) },
                        onWeekdayChange = { onWeekdayChange(index, it) },
                        onMonthDayChange = { onMonthDayChange(index, it) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
            Button(onClick = onAddStep, enabled = !routineSaving, modifier = Modifier.fillMaxWidth().height(auraTokenDp(46f)), shape = RoundedCornerShape(auraTokenDp(14f)), border = BorderStroke(auraTokenDp(1f), MaterialTheme.aura.profile.themeLavender), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.aura.profile.themeLavenderSoft, contentColor = auraHex(0xFF5B21B6))) {
                Text("Добавить шаг", fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onSave, enabled = !routineSaving, modifier = Modifier.fillMaxWidth().height(auraTokenDp(48f)), shape = RoundedCornerShape(auraTokenDp(14f)), border = BorderStroke(auraTokenDp(1f), MaterialTheme.aura.profile.mint500), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.aura.profile.mint500, contentColor = Color.White)) {
                Text(if (routineSaving) "Сохранение..." else "Сохранить рутину", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RoutineInlineStepEditor(
    step: ProfileRoutineStep,
    index: Int,
    total: Int,
    query: String,
    searchOptions: List<RoutineProductOption>,
    onClearQuery: () -> Unit,
    removePending: Boolean,
    onProductQueryChange: (String) -> Unit,
    onSelectProduct: (RoutineProductOption) -> Unit,
    onFrequencyChange: (ReminderFrequency) -> Unit,
    onWeekdayChange: (Int) -> Unit,
    onMonthDayChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = auraTokenDp(4f)), verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
        Text("Шаг ${index + 1}", color = auraHex(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = auraTokenSp(13f))
        RoutineTextField(step.product_label, onProductQueryChange, "Продукт", "Начните вводить бренд или название", accent = MaterialTheme.aura.profile.sky500)
        if (searchOptions.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(auraTokenDp(12f))).background(MaterialTheme.aura.profile.skySoft).border(auraTokenDp(1f), MaterialTheme.aura.profile.sky200, RoundedCornerShape(auraTokenDp(12f)))) {
                searchOptions.take(6).forEach { option ->
                    Text(option.displayLabel, color = auraHex(0xFF0F172A), fontSize = auraTokenSp(13f), modifier = Modifier.fillMaxWidth().clickable { onSelectProduct(option) }.padding(horizontal = auraTokenDp(12f), vertical = auraTokenDp(9f)))
                }
            }
        } else if (query.trim().length >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(auraTokenDp(12f)))
                    .background(MaterialTheme.aura.profile.skySoft)
                    .border(auraTokenDp(1f), MaterialTheme.aura.profile.sky200, RoundedCornerShape(auraTokenDp(12f)))
                    .padding(horizontal = auraTokenDp(12f), vertical = auraTokenDp(10f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ничего не найдено", color = MaterialTheme.aura.profile.slate700, fontSize = auraTokenSp(12f))
                Text(
                    "Очистить",
                    color = MaterialTheme.aura.profile.sky500,
                    fontSize = auraTokenSp(12f),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onClearQuery() },
                )
            }
        }
        Text("Частота", color = MaterialTheme.aura.profile.mint500, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            FrequencyButtonInline("Ежедневно", step.frequency == ReminderFrequency.DAILY, accent = MaterialTheme.aura.profile.mint500, accentBorder = MaterialTheme.aura.profile.mint200) { onFrequencyChange(ReminderFrequency.DAILY) }
            FrequencyButtonInline("Еженедельно", step.frequency == ReminderFrequency.WEEKLY, accent = MaterialTheme.aura.profile.mint500, accentBorder = MaterialTheme.aura.profile.mint200) { onFrequencyChange(ReminderFrequency.WEEKLY) }
            FrequencyButtonInline("Ежемесячно", step.frequency == ReminderFrequency.MONTHLY, accent = MaterialTheme.aura.profile.mint500, accentBorder = MaterialTheme.aura.profile.mint200) { onFrequencyChange(ReminderFrequency.MONTHLY) }
        }
        if (step.frequency == ReminderFrequency.WEEKLY) {
            Text("День недели", color = MaterialTheme.aura.profile.mint500, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEachIndexed { idx, label ->
                    val value = idx + 1
                    FrequencyButtonInline(label, step.weekday == value, accent = MaterialTheme.aura.profile.sky500, accentBorder = MaterialTheme.aura.profile.sky200) { onWeekdayChange(value) }
                }
            }
        }
        if (step.frequency == ReminderFrequency.MONTHLY) {
            Text("День месяца", color = MaterialTheme.aura.profile.mint500, fontSize = auraTokenSp(12f), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                (1..31).forEach { value ->
                    FrequencyButtonInline(value.toString(), step.month_day == value, accent = MaterialTheme.aura.profile.sky500, accentBorder = MaterialTheme.aura.profile.sky200) { onMonthDayChange(value) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(auraTokenDp(36f)).border(auraTokenDp(1f), MaterialTheme.aura.profile.sky200, RoundedCornerShape(auraTokenDp(10f))).background(MaterialTheme.aura.profile.skySoft, RoundedCornerShape(auraTokenDp(10f)))) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Вверх", tint = MaterialTheme.aura.profile.sky500)
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(auraTokenDp(36f)).border(auraTokenDp(1f), MaterialTheme.aura.profile.sky200, RoundedCornerShape(auraTokenDp(10f))).background(MaterialTheme.aura.profile.skySoft, RoundedCornerShape(auraTokenDp(10f)))) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Вниз", tint = MaterialTheme.aura.profile.sky500)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = if (removePending) "Подтвердите удаление шага" else "Удалить шаг",
                    tint = if (removePending) auraHex(0xFFB91C1C) else MaterialTheme.aura.profile.coral500,
                )
            }
        }
        if (removePending) {
            Text("Нажмите удаление еще раз для подтверждения", color = auraHex(0xFFB91C1C), fontSize = auraTokenSp(11f))
        }
    }
}
