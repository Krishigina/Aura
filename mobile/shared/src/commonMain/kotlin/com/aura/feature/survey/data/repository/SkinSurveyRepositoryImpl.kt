package com.aura.feature.survey.data.repository

import com.aura.core.domain.model.SkinJournalSettingsUpdate
import com.aura.core.domain.repository.SessionRepository
import com.aura.core.domain.repository.SkinPassportRepository
import com.aura.feature.survey.data.api.SkinSurveyApi
import com.aura.feature.survey.domain.model.SkinSurveyInitialData
import com.aura.feature.survey.domain.repository.SkinSurveyRepository

class SkinSurveyRepositoryImpl(
    private val skinSurveyApi: SkinSurveyApi,
    private val sessionRepository: SessionRepository,
    private val skinPassportRepository: SkinPassportRepository,
) : SkinSurveyRepository {
    override suspend fun loadInitialData(): SkinSurveyInitialData {
        val localAnswers = skinPassportRepository.answers()
        val token = sessionRepository.token().takeUnless { it.isNullOrBlank() }
            ?: return SkinSurveyInitialData(emptyList(), localAnswers)

        val sections = runCatching { skinSurveyApi.getSurveySchema(token).sections }.getOrDefault(emptyList())
        val serverAnswers = runCatching { skinSurveyApi.getSkinPassport(token) }
            .getOrNull()
            ?.answers
            .orEmpty()
        if (serverAnswers.isNotEmpty()) {
            skinPassportRepository.save(serverAnswers)
        }
        return SkinSurveyInitialData(sections, serverAnswers.ifEmpty { localAnswers })
    }

    override suspend fun savePassport(answers: Map<String, List<String>>): Map<String, List<String>> {
        val token = requireToken()
        val savedPassport = skinSurveyApi.saveSkinPassport(token = token, answers = answers)
        if (savedPassport.answers.isEmpty()) {
            error("Сервер вернул пустой паспорт кожи")
        }
        val storedPassport = skinSurveyApi.getSkinPassport(token)
        if (storedPassport?.answers.isNullOrEmpty()) {
            error("Паспорт кожи не найден в базе после сохранения")
        }
        skinPassportRepository.save(storedPassport!!.answers)
        return storedPassport.answers
    }

    override suspend fun saveSensorOwnership(hasSensor: Boolean) {
        val token = sessionRepository.token().takeUnless { it.isNullOrBlank() } ?: return
        skinSurveyApi.saveSkinJournalSettings(token, SkinJournalSettingsUpdate(has_sensor = hasSensor))
    }

    private fun requireToken(): String = sessionRepository.token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException("Не удалось сохранить анкету: войдите в аккаунт")
}
