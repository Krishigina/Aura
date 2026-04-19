package com.aura.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.aura.core.data.api.model.HomeRitualItem
import com.aura.core.data.api.model.WeatherCoordinates
import com.aura.core.domain.model.ReminderFrequency
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarTopOffset
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.home.components.AiInsightsSection
import com.aura.feature.home.components.HeaderSection
import com.aura.feature.home.components.RecommendationEntryCard
import com.aura.feature.home.components.RitualSection
import com.aura.feature.home.components.RoutineSetupCard
import com.aura.feature.home.components.SkinJournalTodayWidget
import com.aura.feature.home.components.TodayNoRoutineStepsCard
import com.aura.feature.home.components.TopWidgetSection
import com.aura.feature.home.presentation.HomeViewModel
import com.aura.feature.profile.logic.isScheduledToday
import com.aura.feature.profile.logic.shouldHideHomeRoutineChecklist
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

object HomeLocationBridge {
    var requestLocation: (() -> Unit)? = null
    var onLocation: ((WeatherCoordinates?) -> Unit)? = null

    fun request() {
        val request = requestLocation
        if (request == null) {
            deliver(null)
        } else {
            request()
        }
    }

    fun deliver(coordinates: WeatherCoordinates?) {
        onLocation?.invoke(coordinates)
    }
}

@Composable
fun HomeScreen(
    onNavigateToJournal: () -> Unit = {},
    onNavigateToRecommendations: () -> Unit = {},
    onNavigateToProfileSettings: () -> Unit = {},
    viewModel: HomeViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val tokens = MaterialTheme.aura.home
    val bg = if (dark) tokens.darkBackground else tokens.ice
    val textPrimary = if (dark) tokens.textPrimaryDark else tokens.slate800
    val textSecondary = if (dark) tokens.textSecondaryDark else tokens.slate500
    val textBody = if (dark) tokens.textBodyDark else tokens.slate700
    val userName = uiState.userName ?: StringsRu.Common.userFallback

    DisposableEffect(Unit) {
        HomeLocationBridge.onLocation = { coordinates ->
            viewModel.onLocationReceived(coordinates)
        }
        HomeLocationBridge.request()
        onDispose {
            HomeLocationBridge.onLocation = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    val nowDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val todayRoutineItems = remember(uiState.routineSteps, nowDate) {
        uiState.routineSteps
            .filter {
                isScheduledToday(
                    step = it,
                    year = nowDate.year,
                    month = nowDate.monthNumber,
                    dayOfMonth = nowDate.dayOfMonth,
                    dayOfWeek = nowDate.dayOfWeek.ordinal + 1,
                )
            }
            .sortedBy { it.order }
            .map { step ->
                val subtitle = when (step.frequency) {
                    ReminderFrequency.DAILY -> "Ежедневно"
                    ReminderFrequency.WEEKLY -> "Еженедельно"
                    ReminderFrequency.MONTHLY -> "Ежемесячно"
                    ReminderFrequency.NONE -> ""
                }
                HomeRitualItem(
                    id = step.id.ifBlank { "routine-${step.order}" },
                    title = step.product_label.ifBlank { "Шаг ухода" },
                    subtitle = subtitle,
                )
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Home)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.screenHorizontalPadding)
                .padding(top = auraToolbarTopOffset(tokens.toolbarTopOffset), bottom = tokens.screenBottomPadding),
        ) {
            HeaderSection(
                userName = userName,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                dark = dark,
                weather = uiState.homeStatus.weather,
                isLoading = uiState.statusLoading,
            )
            Spacer(modifier = Modifier.height(tokens.sectionGapLarge))
            TopWidgetSection(
                textSecondary = textSecondary,
                textBody = textBody,
                dark = dark,
                topWidget = uiState.homeStatus.top_widget,
                isLoading = uiState.statusLoading,
            )
            Spacer(modifier = Modifier.height(tokens.sectionGapMedium))
            RecommendationEntryCard(
                textBody = textBody,
                textSecondary = textSecondary,
                dark = dark,
                onOpen = onNavigateToRecommendations,
            )
            Spacer(modifier = Modifier.height(tokens.sectionGapMedium))
            SkinJournalTodayWidget(
                textBody = textBody,
                textSecondary = textSecondary,
                dark = dark,
                activeReminders = uiState.activeJournalReminders,
                onOpen = onNavigateToJournal,
            )
            Spacer(modifier = Modifier.height(tokens.sectionGapLarge))
            when {
                uiState.routineLoading -> {
                    RitualSection(
                        textSecondary = textSecondary,
                        textBody = textBody,
                        dark = dark,
                        ritualItems = uiState.ritualItems,
                        checkedStates = uiState.ritualCheckedStates,
                        isLoading = true,
                        onSyncCheckedStates = viewModel::syncRitualChecks,
                        onCheckedChange = viewModel::setRitualChecked,
                    )
                }

                uiState.routineLoadFailed -> {
                    RitualSection(
                        textSecondary = textSecondary,
                        textBody = textBody,
                        dark = dark,
                        ritualItems = uiState.ritualItems,
                        checkedStates = uiState.ritualCheckedStates,
                        isLoading = uiState.feedLoading,
                        onSyncCheckedStates = viewModel::syncRitualChecks,
                        onCheckedChange = viewModel::setRitualChecked,
                    )
                }

                uiState.routineLoaded && shouldHideHomeRoutineChecklist(uiState.routineSteps) -> {
                    RoutineSetupCard(
                        textBody = textBody,
                        textSecondary = textSecondary,
                        dark = dark,
                        onOpenSettings = onNavigateToProfileSettings,
                    )
                }

                uiState.routineLoaded && uiState.routineSteps.isNotEmpty() && todayRoutineItems.isEmpty() -> {
                    TodayNoRoutineStepsCard(
                        textBody = textBody,
                        textSecondary = textSecondary,
                        dark = dark,
                    )
                }

                else -> {
                    RitualSection(
                        textSecondary = textSecondary,
                        textBody = textBody,
                        dark = dark,
                        ritualItems = if (uiState.routineLoaded && uiState.routineSteps.isNotEmpty()) todayRoutineItems else uiState.ritualItems,
                        checkedStates = uiState.ritualCheckedStates,
                        isLoading = uiState.feedLoading,
                        onSyncCheckedStates = viewModel::syncRitualChecks,
                        onCheckedChange = viewModel::setRitualChecked,
                    )
                }
            }
            Spacer(modifier = Modifier.height(tokens.sectionGapLarge))
            AiInsightsSection(
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                textBody = textBody,
                dark = dark,
                insights = uiState.insights,
                isLoading = uiState.feedLoading,
            )
        }
    }
}
