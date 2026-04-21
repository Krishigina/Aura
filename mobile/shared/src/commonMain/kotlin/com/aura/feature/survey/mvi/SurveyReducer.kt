package com.aura.feature.survey.mvi

fun surveyReduce(state: SurveyUiState, intent: SurveyIntent): SurveyUiState {
    return when (intent) {
        is SurveyIntent.SetAnswers -> state.copy(answers = intent.answers)

        is SurveyIntent.SelectOption -> {
            val current = state.answers[intent.questionId].orEmpty().toMutableSet()
            if (intent.multiChoice) {
                if (current.contains(intent.option)) current.remove(intent.option) else current.add(intent.option)
            } else {
                current.clear()
                current.add(intent.option)
            }
            state.copy(answers = state.answers + (intent.questionId to current))
        }

        SurveyIntent.PreviousSection -> {
            state.copy(sectionIndex = (state.sectionIndex - 1).coerceAtLeast(0))
        }

        SurveyIntent.NextSection -> {
            state.copy(sectionIndex = state.sectionIndex + 1)
        }

        is SurveyIntent.SetSaving -> state.copy(isSaving = intent.value)
    }
}
