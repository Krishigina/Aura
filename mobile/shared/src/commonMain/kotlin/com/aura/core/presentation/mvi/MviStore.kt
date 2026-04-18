package com.aura.core.presentation.mvi

/**
 * Lightweight base store for MVI-style state containers.
 * Keeps mutation semantics explicit and consistent across features.
 */
abstract class MviStore<S : UiState, I : Intent>(initialState: S) {
    private var _state: S = initialState

    protected val state: S
        get() = _state

    fun currentState(): S = _state

    fun dispatch(intent: I): S {
        _state = reduce(_state, intent)
        return _state
    }

    protected fun setState(next: S): S {
        _state = next
        return _state
    }

    protected abstract fun reduce(state: S, intent: I): S
}
