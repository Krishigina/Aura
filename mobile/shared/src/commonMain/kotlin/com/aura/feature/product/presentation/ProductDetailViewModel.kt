package com.aura.feature.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.product.domain.usecase.GetProductDetailUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val getProductDetail: GetProductDetailUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun load(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val detail = getProductDetail(productId)
                _uiState.update { it.copy(detail = detail, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки продукта") }
            }
        }
    }

    fun beginAssistantAction() {
        _uiState.update { it.copy(isAssistantLoading = true) }
    }

    fun endAssistantAction() {
        _uiState.update { it.copy(isAssistantLoading = false) }
    }

    fun addToRoutine(onAddToRoutine: () -> Unit) {
        if (_uiState.value.isRoutineLoading) return
        if (_uiState.value.isInFavorites) {
            _uiState.update { it.copy(isInFavorites = false, routineFeedback = null) }
            return
        }
        _uiState.update { it.copy(isRoutineLoading = true, routineFeedback = null) }
        viewModelScope.launch {
            val feedback = try {
                onAddToRoutine()
                _uiState.update { it.copy(isInFavorites = true) }
                null
            } catch (_: Exception) {
                "Не удалось добавить, попробуйте снова"
            }
            _uiState.update { it.copy(isRoutineLoading = false, routineFeedback = feedback) }
            if (feedback != null) {
                delay(2000)
                _uiState.update { it.copy(routineFeedback = null) }
            }
        }
    }
}
