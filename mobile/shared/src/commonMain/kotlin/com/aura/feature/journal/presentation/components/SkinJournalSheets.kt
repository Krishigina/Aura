package com.aura.feature.journal.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.journal.journalZoneLabel
import com.aura.feature.journal.presentation.SkinJournalViewModel
import com.aura.feature.journal.procedureFaceZones
import com.aura.feature.journal.sensorFaceZones

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcedureEntrySheet(
    viewModel: SkinJournalViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.procedureForm

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.aura.journal.procedureSoftAlt) {
        SheetContent(title = "Добавить процедуру") {
            GlassInset(background = MaterialTheme.aura.journal.procedureSoft, border = MaterialTheme.aura.journal.procedureAccent.copy(alpha = auraTokenAlpha(0.28f))) {
                SearchableSelect(
                    label = "Процедура",
                    selectedLabel = form.procedures.firstOrNull { it.id.toString() == form.selectedProcedureId }?.name,
                    placeholder = "Выберите процедуру",
                    options = form.procedures.map { it.id.toString() to it.name },
                    onSelected = viewModel::selectProcedure,
                )
            }
            GlassInset(background = MaterialTheme.aura.journal.procedureSoftAlt, border = MaterialTheme.aura.journal.procedureAccent.copy(alpha = auraTokenAlpha(0.24f))) {
                OutlinedTextField(
                    value = form.procedureDate,
                    onValueChange = viewModel::updateProcedureDate,
                    label = { Text("Дата процедуры, dd.mm.yyyy") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = auraTextFieldColors(),
                )
            }
            GlassInset(background = MaterialTheme.aura.journal.procedureSoft, border = MaterialTheme.aura.journal.procedureAccent.copy(alpha = auraTokenAlpha(0.28f))) {
                SearchableMultiSelect(
                    label = "Зоны",
                    placeholder = "Выберите зоны",
                    selectedValues = form.selectedZones,
                    options = procedureFaceZones.map { it.id to it.label },
                    onToggle = viewModel::toggleProcedureZone,
                )
                form.selectedZones.forEach { zone ->
                    OutlinedTextField(
                        value = form.zoneAmounts[zone].orEmpty(),
                        onValueChange = { viewModel.updateProcedureZoneAmount(zone, it) },
                        label = { Text("Количество: ${journalZoneLabel(zone)}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = auraTextFieldColors(),
                    )
                }
            }
            form.error?.let { InlineError(it) }
            SheetActions(
                isSaving = form.isSaving,
                onCancel = onDismiss,
                saveBackground = MaterialTheme.aura.journal.procedureAccent,
                onSave = viewModel::saveProcedureForm,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorReadingSheet(
    viewModel: SkinJournalViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = uiState.sensorForm

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.aura.journal.sensorSoftAlt) {
        SheetContent(title = "Добавить замер") {
            GlassInset(background = MaterialTheme.aura.journal.sensorSoft, border = MaterialTheme.aura.journal.sensorAccent.copy(alpha = auraTokenAlpha(0.28f))) {
                SearchableSelect(
                    label = "Зона датчика",
                    selectedLabel = form.selectedZone?.let { journalZoneLabel(it) },
                    placeholder = "Выберите зону",
                    options = sensorFaceZones.map { it.id to it.label },
                    onSelected = viewModel::selectSensorZone,
                )
                OutlinedTextField(
                    value = form.percent,
                    onValueChange = viewModel::updateSensorPercent,
                    label = { Text("% содержания влаги, 0-100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = auraTextFieldColors(),
                )
            }
            GlassInset(background = MaterialTheme.aura.journal.sensorSoftAlt, border = MaterialTheme.aura.journal.sensorAccent.copy(alpha = auraTokenAlpha(0.24f))) {
                DotMetric("Влага (1-5)", form.hydration, viewModel::updateSensorHydration)
                DotMetric("Жирность (1-5)", form.oiliness, viewModel::updateSensorOiliness)
                DotMetric("Мягкость (1-5)", form.softness, viewModel::updateSensorSoftness)
            }
            form.error?.let { InlineError(it) }
            SheetActions(
                isSaving = form.isSaving,
                onCancel = onDismiss,
                saveBackground = MaterialTheme.aura.journal.sensorAccent,
                onSave = viewModel::saveSensorForm,
            )
        }
    }
}

@Composable
fun SheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = auraTokenDp(20f))
            .padding(bottom = auraTokenDp(28f)),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(14f)),
    ) {
        Text(title, color = MaterialTheme.aura.journal.slate800, fontSize = auraTokenSp(22f), fontWeight = FontWeight.ExtraBold)
        content()
    }
}

@Composable
fun SheetActions(
    isSaving: Boolean,
    onCancel: () -> Unit,
    saveBackground: Color = MaterialTheme.aura.journal.neutral,
    onSave: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(10f)), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !isSaving) { Text("Отмена") }
        Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = !isSaving, colors = ButtonDefaults.buttonColors(containerColor = saveBackground, contentColor = MaterialTheme.aura.journal.slate700)) {
            Text(if (isSaving) "Сохранение..." else "Сохранить", fontWeight = FontWeight.Bold)
        }
    }
}
