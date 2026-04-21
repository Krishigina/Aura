# MVI Conventions (shared)

Use this layout for each feature:

```
feature/<name>/mvi/
  <Feature>Contract.kt   // UiState + Intent (+ Effect if needed)
  <Feature>Reducer.kt    // pure reducer(state, intent)
  <Feature>Store.kt      // extends MviStore, owns side effects/data calls
```

## Rules

1. `UiState` is immutable (`data class`) and implements `UiState` marker.
2. `Intent` and `Effect` are sealed interfaces and implement shared markers.
3. `Reducer` is pure and does not call APIs/repositories.
4. `Store` is the only place for I/O (API, storage, managers).
5. Compose screens read `currentState()`, dispatch intents, react to effects.
6. Theme values in UI must come from `AuraPalette` / `auraThemeColors`.

## Minimal template

```kotlin
data class FeatureUiState(...) : UiState

sealed interface FeatureIntent : Intent { ... }
sealed interface FeatureEffect : Effect { ... }

fun featureReduce(state: FeatureUiState, intent: FeatureIntent): FeatureUiState = ...

class FeatureStore(...) : MviStore<FeatureUiState, FeatureIntent>(FeatureUiState()) {
    override fun reduce(state: FeatureUiState, intent: FeatureIntent): FeatureUiState =
        featureReduce(state, intent)
}
```
