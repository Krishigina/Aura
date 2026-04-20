package com.aura.feature.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.data.repository.journal.SkinJournalStore
import com.aura.core.domain.model.SkinJournalProcedureCreate
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.core.domain.model.SkinJournalSensorReadingCreate
import com.aura.core.domain.model.SkinJournalZoneAmount
import com.aura.feature.journal.currentJournalDateKey
import com.aura.feature.journal.currentJournalTimestamp
import com.aura.feature.journal.domain.repository.SkinJournalRepository
import com.aura.feature.journal.sixMonthsJournalTimestamp
import com.aura.feature.journal.toJournalDateTimestampOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SkinJournalViewModel(
    private val repository: SkinJournalRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkinJournalUiState())
    val uiState: StateFlow<SkinJournalUiState> = _uiState.asStateFlow()

    init {
        val today = currentJournalDateKey()
        _uiState.update { it.copy(calendarYear = today.take(4).toInt(), calendarMonth = today.substring(5, 7).toInt()) }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                applyJournal(repository.loadJournal())
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Не удалось загрузить журнал") }
            }
        }
    }

    fun updateReminder(reminderId: String, action: SkinJournalReminderAction) {
        viewModelScope.launch {
            try {
                applyJournal(repository.updateReminder(reminderId, action))
                _uiState.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "Не удалось обновить напоминание") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setSheet(sheet: String?) {
        _uiState.update { it.copy(sheet = sheet) }
        if (sheet == "procedure") loadProcedureCatalogForSheet()
    }

    fun loadProcedureCatalogForSheet() {
        viewModelScope.launch {
            runCatching { repository.loadProcedureCatalog() }
                .onSuccess { procedures -> _uiState.update { it.copy(procedureForm = it.procedureForm.copy(procedures = procedures, error = null)) } }
                .onFailure { _uiState.update { it.copy(procedureForm = it.procedureForm.copy(error = "Не удалось загрузить процедуры")) } }
        }
    }

    fun selectProcedure(id: String) {
        _uiState.update { it.copy(procedureForm = it.procedureForm.copy(selectedProcedureId = id, error = null)) }
    }

    fun updateProcedureDate(value: String) {
        _uiState.update { it.copy(procedureForm = it.procedureForm.copy(procedureDate = value.filter { char -> char.isDigit() || char == '.' }.take(10))) }
    }

    fun toggleProcedureZone(zoneId: String) {
        _uiState.update { state ->
            val form = state.procedureForm
            val selectedZones = if (zoneId in form.selectedZones) form.selectedZones - zoneId else form.selectedZones + zoneId
            val zoneAmounts = if (zoneId in selectedZones) form.zoneAmounts else form.zoneAmounts - zoneId
            state.copy(procedureForm = form.copy(selectedZones = selectedZones, zoneAmounts = zoneAmounts, error = null))
        }
    }

    fun updateProcedureZoneAmount(zoneId: String, amount: String) {
        _uiState.update { state ->
            val form = state.procedureForm
            state.copy(procedureForm = form.copy(zoneAmounts = form.zoneAmounts + (zoneId to amount), error = null))
        }
    }

    fun saveProcedureForm() {
        val form = _uiState.value.procedureForm
        val procedure = form.procedures.firstOrNull { it.id.toString() == form.selectedProcedureId }
        val missingAmounts = form.selectedZones.any { form.zoneAmounts[it].orEmpty().trim().isEmpty() }
        val performedAt = form.procedureDate.toJournalDateTimestampOrNull()
        if (performedAt == null) {
            _uiState.update { it.copy(procedureForm = it.procedureForm.copy(error = "Укажите дату процедуры в формате dd.mm.yyyy")) }
            return
        }
        if (procedure == null || form.selectedZones.isEmpty() || missingAmounts) {
            _uiState.update { it.copy(procedureForm = it.procedureForm.copy(error = "Выберите процедуру, зоны и заполните количество для каждой зоны")) }
            return
        }

        _uiState.update { it.copy(procedureForm = it.procedureForm.copy(isSaving = true, error = null)) }
        viewModelScope.launch {
            runCatching {
                repository.createProcedure(
                    SkinJournalProcedureCreate(
                        catalog_procedure_id = procedure.id,
                        procedure_name = procedure.name,
                        performed_at = performedAt,
                        zones = form.selectedZones,
                        zone_amounts = form.selectedZones.map { SkinJournalZoneAmount(zone = it, amount = form.zoneAmounts[it].orEmpty().trim()) },
                        repeat_due_at = sixMonthsJournalTimestamp(),
                        post_care_tasks = listOfNotNull(procedure.rehabilitation?.takeIf { it.isNotBlank() }),
                    ),
                )
            }.onSuccess { journal ->
                applyJournal(journal)
                _uiState.update { it.copy(sheet = null, procedureForm = ProcedureEntryFormState()) }
            }.onFailure {
                _uiState.update { it.copy(procedureForm = it.procedureForm.copy(isSaving = false, error = "Не удалось сохранить процедуру")) }
            }
        }
    }

    fun selectSensorZone(zone: String) {
        _uiState.update { it.copy(sensorForm = it.sensorForm.copy(selectedZone = zone, error = null)) }
    }

    fun updateSensorPercent(value: String) {
        _uiState.update { it.copy(sensorForm = it.sensorForm.copy(percent = value.filter { char -> char.isDigit() }.take(3), error = null)) }
    }

    fun updateSensorHydration(value: Int) {
        _uiState.update { it.copy(sensorForm = it.sensorForm.copy(hydration = value, error = null)) }
    }

    fun updateSensorOiliness(value: Int) {
        _uiState.update { it.copy(sensorForm = it.sensorForm.copy(oiliness = value, error = null)) }
    }

    fun updateSensorSoftness(value: Int) {
        _uiState.update { it.copy(sensorForm = it.sensorForm.copy(softness = value, error = null)) }
    }

    fun saveSensorForm() {
        val form = _uiState.value.sensorForm
        val percentValue = form.percent.toIntOrNull()
        val zone = form.selectedZone
        if (zone == null || percentValue == null || percentValue !in 0..100 || form.hydration !in 1..5 || form.oiliness !in 1..5 || form.softness !in 1..5) {
            _uiState.update { it.copy(sensorForm = it.sensorForm.copy(error = "Выберите зону, укажите % содержания влаги (0-100) и заполните все шкалы (1-5)")) }
            return
        }

        _uiState.update { it.copy(sensorForm = it.sensorForm.copy(isSaving = true, error = null)) }
        viewModelScope.launch {
            runCatching {
                repository.createSensorReading(
                    SkinJournalSensorReadingCreate(
                        measured_at = currentJournalTimestamp(),
                        zone = zone,
                        percent_value = percentValue,
                        hydration = form.hydration,
                        oiliness = form.oiliness,
                        softness = form.softness,
                    ),
                )
            }.onSuccess { journal ->
                applyJournal(journal)
                _uiState.update { it.copy(sheet = null, sensorForm = SensorReadingFormState()) }
            }.onFailure {
                _uiState.update { it.copy(sensorForm = it.sensorForm.copy(isSaving = false, error = "Не удалось сохранить замер")) }
            }
        }
    }

    fun setSelectedMode(mode: String) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun setSelectedDateKey(dateKey: String) {
        _uiState.update { it.copy(selectedDateKey = dateKey) }
    }

    fun setCalendar(year: Int, month: Int, selectedDateKey: String) {
        _uiState.update { it.copy(calendarYear = year, calendarMonth = month, selectedDateKey = selectedDateKey) }
    }

    fun setShowYearPicker(show: Boolean) {
        _uiState.update { it.copy(showYearPicker = show) }
    }

    private fun applyJournal(journal: SkinJournalResponse) {
        SkinJournalStore.save(journal)
        _uiState.update { it.copy(journal = journal) }
    }
}
