package com.aura.feature.journal.presentation

import com.aura.core.data.repository.journal.SkinJournalStore
import com.aura.core.domain.model.ProcedureCatalogItem
import com.aura.core.domain.model.SkinJournalResponse
import com.aura.feature.journal.currentJournalDateDisplay

data class ProcedureEntryFormState(
    val procedures: List<ProcedureCatalogItem> = emptyList(),
    val selectedProcedureId: String? = null,
    val procedureDate: String = currentJournalDateDisplay(),
    val selectedZones: List<String> = emptyList(),
    val zoneAmounts: Map<String, String> = emptyMap(),
    val error: String? = null,
    val isSaving: Boolean = false,
)

data class SensorReadingFormState(
    val selectedZone: String? = null,
    val percent: String = "",
    val hydration: Int = 0,
    val oiliness: Int = 0,
    val softness: Int = 0,
    val error: String? = null,
    val isSaving: Boolean = false,
)

data class SkinJournalUiState(
    val journal: SkinJournalResponse = SkinJournalStore.journal,
    val sheet: String? = null,
    val procedureForm: ProcedureEntryFormState = ProcedureEntryFormState(),
    val sensorForm: SensorReadingFormState = SensorReadingFormState(),
    val selectedMode: String = "feed",
    val selectedDateKey: String = "",
    val calendarYear: Int = 0,
    val calendarMonth: Int = 0,
    val showYearPicker: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
