package com.aura.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.aura.core.domain.model.SkinJournalReminderAction
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenDp
import com.aura.feature.journal.presentation.components.ActiveRemindersCard
import com.aura.feature.journal.presentation.components.InlineError
import com.aura.feature.journal.presentation.components.JournalModeSwitch
import com.aura.feature.journal.presentation.components.JournalSummaryCard
import com.aura.feature.journal.presentation.components.LatestProceduresCard
import com.aura.feature.journal.presentation.components.MonthCalendarCard
import com.aura.feature.journal.presentation.components.ProcedureEntrySheet
import com.aura.feature.journal.presentation.components.SelectedDateEntriesCard
import com.aura.feature.journal.presentation.components.SensorReadingSheet
import com.aura.feature.journal.presentation.components.SensorReadingsCard
import com.aura.feature.journal.presentation.components.YearPickerSheet
import com.aura.feature.journal.presentation.SkinJournalViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinJournalScreen(
    onBack: () -> Unit,
    viewModel: SkinJournalViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val journal = uiState.journal
    val initialDateKey = remember { currentJournalDateKey() }
    val sheet = uiState.sheet
    val selectedMode = uiState.selectedMode
    val selectedDateKey = uiState.selectedDateKey
    val calendarYear = uiState.calendarYear.takeIf { it > 0 } ?: initialDateKey.take(4).toInt()
    val calendarMonth = uiState.calendarMonth.takeIf { it > 0 } ?: initialDateKey.substring(5, 7).toInt()
    val showYearPicker = uiState.showYearPicker

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.aura.journal.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.aura.journal.gradientTop, MaterialTheme.aura.journal.gradientMid, MaterialTheme.aura.journal.gradientBottom),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = auraTokenDp(24f))
                .padding(top = auraToolbarContentTopPadding(), bottom = auraTokenDp(120f)),
            verticalArrangement = Arrangement.spacedBy(auraTokenDp(24f)),
        ) {
            JournalSummaryCard(
                journal = journal,
                onAddProcedure = { viewModel.setSheet("procedure") },
                onAddSensor = { viewModel.setSheet("sensor") },
            )
            JournalModeSwitch(selectedMode) { viewModel.setSelectedMode(it) }
            if (selectedMode == "feed") {
                ActiveRemindersCard(journal.reminders) { reminderId, action ->
                    viewModel.updateReminder(
                        reminderId,
                        SkinJournalReminderAction(
                            action = action,
                            rescheduled_due_at = if (action == "reschedule") tomorrowJournalTimestamp() else null,
                        ),
                    )
                }
            }
            uiState.error?.let { InlineError(it) }
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(auraTokenDp(22f)), color = MaterialTheme.aura.journal.neutral)
                }
            }
            if (selectedMode == "feed") {
                SelectedDateEntriesCard(entriesForJournalDate(selectedDateKey, journal.procedures, journal.sensor_readings))
                SensorReadingsCard(journal.sensor_readings.takeLast(8).reversed())
                LatestProceduresCard(latestSkinJournalProcedures(journal.procedures, limit = 12))
            } else {
                val eventDays = skinJournalEventDays(journal.procedures, journal.sensor_readings)
                val eventMonths = skinJournalEventMonths(journal.procedures, journal.sensor_readings)
                SelectedDateEntriesCard(entriesForJournalDate(selectedDateKey, journal.procedures, journal.sensor_readings))
                Spacer(modifier = Modifier.height(auraTokenDp(8f)))
                MonthCalendarCard(
                    year = calendarYear,
                    month = calendarMonth,
                    selectedDateKey = selectedDateKey,
                    todayDateKey = initialDateKey,
                    eventDays = eventDays,
                    onDateSelected = viewModel::setSelectedDateKey,
                    onYearClick = { viewModel.setShowYearPicker(true) },
                    onPreviousMonth = {
                        val previous = shiftJournalMonth(calendarYear, calendarMonth, -1)
                        viewModel.setCalendar(previous.first, previous.second, selectedJournalMonthDate(previous.first, previous.second, eventDays))
                    },
                    onNextMonth = {
                        val next = shiftJournalMonth(calendarYear, calendarMonth, 1)
                        viewModel.setCalendar(next.first, next.second, selectedJournalMonthDate(next.first, next.second, eventDays))
                    },
                )
                if (showYearPicker) {
                    YearPickerSheet(
                        selectedYear = calendarYear,
                        selectedMonth = calendarMonth,
                        eventMonths = eventMonths,
                        onYearSelected = { year ->
                            viewModel.setCalendar(year, calendarMonth, selectedJournalMonthDate(year, calendarMonth, eventDays))
                            viewModel.setShowYearPicker(false)
                        },
                        onDismiss = { viewModel.setShowYearPicker(false) },
                    )
                }
            }
        }
        AuraTopBar(
            title = "Журнал кожи",
            onBack = onBack,
            titleColor = MaterialTheme.aura.journal.slate800,
            iconTint = MaterialTheme.aura.journal.slate500,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    when (sheet) {
        "procedure" -> ProcedureEntrySheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setSheet(null) },
        )

        "sensor" -> SensorReadingSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setSheet(null) },
        )
    }
}
