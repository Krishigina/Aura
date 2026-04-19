package com.aura.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.data.api.model.HomeRitualItem
import com.aura.core.data.api.model.WeatherCoordinates
import com.aura.feature.home.domain.usecase.GetHomeDashboardUseCase
import com.aura.feature.home.domain.usecase.GetHomeStatusUseCase
import com.aura.feature.home.domain.usecase.GetHomeUserNameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeDashboard: GetHomeDashboardUseCase,
    private val getHomeStatus: GetHomeStatusUseCase,
    private val getHomeUserName: GetHomeUserNameUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(userName = getHomeUserName()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(feedLoading = true, routineLoading = true, userName = getHomeUserName()) }
            try {
                val dashboard = getHomeDashboard()
                _uiState.update {
                    it.copy(
                        ritualItems = dashboard.ritualItems,
                        ritualCheckedStates = dashboard.ritualItems.associate { item -> item.id to item.checked },
                        insights = dashboard.insights,
                        activeJournalReminders = dashboard.activeJournalReminders,
                        routineSteps = dashboard.routineSteps,
                        routineLoadFailed = dashboard.routineLoadFailed,
                        routineLoaded = dashboard.routineLoaded,
                        routineLoading = false,
                        feedLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        ritualItems = emptyList(),
                        insights = emptyList(),
                        activeJournalReminders = emptyList(),
                        routineSteps = emptyList(),
                        routineLoadFailed = true,
                        routineLoaded = false,
                        routineLoading = false,
                        feedLoading = false,
                    )
                }
            }
        }
    }

    fun loadStatus(latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusLoading = true) }
            val status = getHomeStatus(latitude, longitude).status
            _uiState.update { it.copy(homeStatus = status, statusLoading = false) }
        }
    }

    fun onLocationReceived(coordinates: WeatherCoordinates?) {
        _uiState.update { it.copy(weatherCoordinates = coordinates, locationRequested = true) }
        loadStatus(coordinates?.latitude, coordinates?.longitude)
    }

    fun syncRitualChecks(items: List<HomeRitualItem>) {
        _uiState.update { state ->
            val synced = items.associate { item -> item.id to (state.ritualCheckedStates[item.id] ?: item.checked) }
            state.copy(ritualCheckedStates = synced)
        }
    }

    fun setRitualChecked(itemId: String, checked: Boolean) {
        _uiState.update { it.copy(ritualCheckedStates = it.ritualCheckedStates + (itemId to checked)) }
    }
}
