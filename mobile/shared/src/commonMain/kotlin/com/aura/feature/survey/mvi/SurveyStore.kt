package com.aura.feature.survey.mvi

import com.aura.core.data.api.AuraApiClient
import com.aura.core.data.repository.SkinPassportManager
import com.aura.core.data.repository.TokenManager
import com.aura.core.presentation.mvi.MviStore

class SurveyStore(private val apiClient: AuraApiClient) : MviStore<SurveyUiState, SurveyIntent>(SurveyUiState()) {
    override fun reduce(state: SurveyUiState, intent: SurveyIntent): SurveyUiState = surveyReduce(state, intent)

    private fun isAuthError(message: String?): Boolean {
        val msg = message?.lowercase() ?: return false
        return msg.contains("invalid token") ||
            msg.contains("token") && (msg.contains("expired") || msg.contains("invalid")) ||
            msg.contains("401") ||
            msg.contains("unauthorized") ||
            msg.contains("неавтор") ||
            msg.contains("сессия")
    }

    suspend fun preload(): SurveyUiState {
        val localAnswers = SkinPassportManager.passport?.answers.orEmpty().mapValues { it.value.toSet() }
        if (localAnswers.isNotEmpty()) {
            dispatch(SurveyIntent.SetAnswers(localAnswers))
        }

        val token = TokenManager.getToken()
        if (!token.isNullOrBlank()) {
            runCatching { apiClient.getSkinPassport(token) }
                .getOrNull()
                ?.let { serverPassport ->
                    if (serverPassport.answers.isNotEmpty()) {
                        SkinPassportManager.save(serverPassport.answers)
                        dispatch(SurveyIntent.SetAnswers(serverPassport.answers.mapValues { it.value.toSet() }))
                    }
                }
        }

        return state
    }

    suspend fun continueOrComplete(totalSections: Int): SurveyEffect {
        if (state.isSaving) return SurveyEffect.MovedNext

        if (state.sectionIndex < totalSections - 1) {
            dispatch(SurveyIntent.NextSection)
            return SurveyEffect.MovedNext
        }

        val normalizedAnswers = state.answers.mapValues { it.value.toList() }
        SkinPassportManager.save(normalizedAnswers)

        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            return SurveyEffect.SaveFailed("Сессия истекла. Войдите снова, чтобы сохранить анкету")
        }

        dispatch(SurveyIntent.SetSaving(true))
        return runCatching {
            apiClient.saveSkinPassport(token = token, answers = normalizedAnswers)
            dispatch(SurveyIntent.SetSaving(false))
            SurveyEffect.Completed
        }.getOrElse { e ->
            dispatch(SurveyIntent.SetSaving(false))
            if (isAuthError(e.message)) {
                TokenManager.clearToken()
                SurveyEffect.SaveFailed("Сессия истекла. Войдите снова, чтобы сохранить анкету")
            } else {
                SurveyEffect.SaveFailed(e.message ?: "Ошибка сохранения")
            }
        }
    }
}
