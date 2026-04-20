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
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ReminderFrequency
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenDp
import com.aura.feature.profile.presentation.components.routine.RoutineInlineCard
import com.aura.feature.profile.logic.friendlyProfileError
import com.aura.feature.profile.logic.normalizeRoutineStepOrder
import com.aura.feature.profile.logic.validateRoutineStep
import com.aura.feature.profile.presentation.routine.ProfileRoutineViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileRoutineScreen(
    onBack: () -> Unit,
    viewModel: ProfileRoutineViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val routineSteps = uiState.routineSteps
    val searchQueries = uiState.searchQueries
    val pendingRemoveStepId = uiState.pendingRemoveStepId

    fun clearSearchForStep(stepId: String) {
        viewModel.clearSearchForStep(stepId)
    }

    fun requestProductSearch(stepId: String, query: String) {
        viewModel.search(stepId, query)
    }

    LaunchedEffect(Unit) { viewModel.load(viewModel::withStableStepIds) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.aura.profile.iceBlue)) {
        SoftPastelBackground(dark = false, variant = SoftPastelVariant.Default)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = auraToolbarContentTopPadding(), bottom = auraTokenDp(32f))) {
            Column(modifier = Modifier.padding(horizontal = auraTokenDp(24f))) {
                RoutineInlineCard(
                    routineSteps = routineSteps,
                    routineLoading = uiState.loading,
                    routineSaving = uiState.saving,
                    routineError = friendlyProfileError(uiState.error),
                    routineSuccess = uiState.success,
                    onAddStep = {
                        viewModel.updateSteps(normalizeRoutineStepOrder(routineSteps + ProfileRoutineStep(viewModel.newLocalStepId(), "", routineSteps.size + 1, ReminderFrequency.DAILY, null)))
                    },
                    onProductQueryChange = { index, query ->
                        val stepId = routineSteps[index].id
                        viewModel.updateSteps(routineSteps.toMutableList().also { it[index] = it[index].copy(product_label = query) })
                        requestProductSearch(stepId, query)
                    },
                    routineSearchResults = uiState.searchResults,
                    routineSearchQueries = searchQueries,
                    onClearProductQuery = { index ->
                        val stepId = routineSteps[index].id
                        viewModel.updateSteps(routineSteps.toMutableList().also { it[index] = it[index].copy(product_label = "") })
                        clearSearchForStep(stepId)
                    },
                    onSelectProduct = { index, option ->
                        val stepId = routineSteps[index].id
                        viewModel.updateSteps(routineSteps.toMutableList().also { it[index] = it[index].copy(product_label = option.displayLabel) })
                        viewModel.clearSearchForStep(stepId)
                    },
                    onFrequencyChange = { index, frequency ->
                        viewModel.updateSteps(
                            routineSteps.toMutableList().also {
                                it[index] = it[index].copy(
                                    frequency = frequency,
                                    weekday = if (frequency == ReminderFrequency.WEEKLY) it[index].weekday else null,
                                    month_day = if (frequency == ReminderFrequency.MONTHLY) it[index].month_day else null,
                                )
                            },
                        )
                    },
                    onWeekdayChange = { index, weekday ->
                        viewModel.updateSteps(routineSteps.toMutableList().also { it[index] = it[index].copy(weekday = weekday) })
                    },
                    onMonthDayChange = { index, monthDay ->
                        viewModel.updateSteps(routineSteps.toMutableList().also { it[index] = it[index].copy(month_day = monthDay) })
                    },
                    onMoveUp = { index ->
                        if (index > 0) {
                            val updated = routineSteps.toMutableList()
                            val curr = updated[index]
                            updated[index] = updated[index - 1]
                            updated[index - 1] = curr
                            viewModel.updateSteps(normalizeRoutineStepOrder(updated))
                        }
                    },
                    onMoveDown = { index ->
                        if (index < routineSteps.lastIndex) {
                            val updated = routineSteps.toMutableList()
                            val curr = updated[index]
                            updated[index] = updated[index + 1]
                            updated[index + 1] = curr
                            viewModel.updateSteps(normalizeRoutineStepOrder(updated))
                        }
                    },
                    onRemove = { index ->
                        val stepId = routineSteps[index].id
                        if (pendingRemoveStepId != stepId) {
                            viewModel.setPendingRemoveStepId(stepId)
                        } else {
                            clearSearchForStep(stepId)
                            viewModel.updateSteps(normalizeRoutineStepOrder(routineSteps.toMutableList().also { it.removeAt(index) }))
                            viewModel.setPendingRemoveStepId(null)
                        }
                    },
                    pendingRemoveStepId = pendingRemoveStepId,
                    onSave = {
                        val normalized = normalizeRoutineStepOrder(routineSteps)
                        for (step in normalized) {
                            val err = validateRoutineStep(step)
                            if (err != null) {
                                return@RoutineInlineCard
                            }
                        }
                        viewModel.save(normalized, viewModel::withStableStepIds)
                    },
                )
            }
        }

        AuraTopBar(
            title = "Моя рутина",
            onBack = onBack,
            titleColor = MaterialTheme.aura.profile.slate800,
            iconTint = MaterialTheme.aura.profile.slate500,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
