package com.aura.feature.profile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aura.core.domain.model.ReminderFrequency
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenDp
import com.aura.feature.profile.presentation.components.notifications.NotificationsInlineCard
import com.aura.feature.profile.logic.formatMaskedTime
import com.aura.feature.profile.logic.friendlyProfileError
import com.aura.feature.profile.logic.validateNotificationSettingsBeforeSave
import com.aura.feature.profile.presentation.notifications.ProfileNotificationsViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileNotificationsScreen(
    onBack: () -> Unit,
    viewModel: ProfileNotificationsViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.aura.profile.iceBlue)) {
        SoftPastelBackground(dark = false, variant = SoftPastelVariant.Default)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = auraToolbarContentTopPadding(), bottom = auraTokenDp(32f))) {
            Column(modifier = Modifier.padding(horizontal = auraTokenDp(24f))) {
                NotificationsInlineCard(
                    settings = settings,
                    loading = uiState.loading,
                    saving = uiState.saving,
                    error = friendlyProfileError(uiState.error),
                    success = uiState.success,
                    onToggleAll = { viewModel.updateSettings(settings.copy(disable_all = it)) },
                    onRoutineFrequencyChange = { f -> viewModel.updateSettings(settings.copy(routine = settings.routine.copy(frequency = f, reminder_time = if (f == ReminderFrequency.NONE) null else settings.routine.reminder_time))) },
                    onRoutineWeekdayChange = { value -> viewModel.updateSettings(settings.copy(routine = settings.routine.copy(weekday = value))) },
                    onRoutineMonthDayChange = { value -> viewModel.updateSettings(settings.copy(routine = settings.routine.copy(month_day = value))) },
                    onRoutineTimeChange = { t -> viewModel.updateSettings(settings.copy(routine = settings.routine.copy(reminder_time = formatMaskedTime(t).ifBlank { null }))) },
                    onJournalFrequencyChange = { f -> viewModel.updateSettings(settings.copy(journal = settings.journal.copy(frequency = f, reminder_time = if (f == ReminderFrequency.NONE) null else settings.journal.reminder_time))) },
                    onJournalWeekdayChange = { value -> viewModel.updateSettings(settings.copy(journal = settings.journal.copy(weekday = value))) },
                    onJournalMonthDayChange = { value -> viewModel.updateSettings(settings.copy(journal = settings.journal.copy(month_day = value))) },
                    onJournalTimeChange = { t -> viewModel.updateSettings(settings.copy(journal = settings.journal.copy(reminder_time = formatMaskedTime(t).ifBlank { null }))) },
                    onSave = {
                        val validationError = validateNotificationSettingsBeforeSave(settings)
                        if (validationError != null) {
                            return@NotificationsInlineCard
                        }
                        viewModel.save()
                    },
                )
            }
        }

        AuraTopBar(
            title = "Уведомления",
            onBack = onBack,
            titleColor = MaterialTheme.aura.profile.slate800,
            iconTint = MaterialTheme.aura.profile.slate500,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
