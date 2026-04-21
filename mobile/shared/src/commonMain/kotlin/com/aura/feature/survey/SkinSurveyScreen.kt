package com.aura.feature.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.theme.aura
import com.aura.feature.survey.components.SensorOnboardingPrompt
import com.aura.feature.survey.components.SurveyFooter
import com.aura.feature.survey.components.SurveyHeader
import com.aura.feature.survey.components.SurveyQuestionCard
import com.aura.feature.survey.components.shouldHideSurveyQuestion
import com.aura.feature.survey.presentation.SkinSurveyViewModel
import org.koin.compose.koinInject

@Composable
fun AuraSkinSurveyScreen(
    allowSkip: Boolean,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onSensorConnected: () -> Unit,
    viewModel: SkinSurveyViewModel = koinInject(),
) {
    val survey = MaterialTheme.aura.survey
    val uiState by viewModel.uiState.collectAsState()
    val sectionIndex = uiState.sectionIndex
    val surveySections = uiState.sections
    val answers = uiState.selectedAnswers
    val scrollState = rememberScrollState()

    fun saveSensorOwnership(hasSensor: Boolean) {
        viewModel.saveSensorOwnership(hasSensor)
        if (hasSensor) onSensorConnected() else onComplete()
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val currentSection = surveySections.getOrNull(sectionIndex)
    val progress = if (surveySections.isNotEmpty()) {
        (sectionIndex + 1).toFloat() / surveySections.size
    } else {
        0f
    }

    LaunchedEffect(sectionIndex) {
        scrollState.scrollTo(0)
    }

    Box(modifier = Modifier.fillMaxSize().background(survey.backgroundColor)) {
        SoftPastelBackground(variant = SoftPastelVariant.Survey)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = survey.screenHorizontalPadding)
                .padding(top = auraToolbarContentTopPadding(168.dp), bottom = survey.screenBottomPadding),
            verticalArrangement = Arrangement.spacedBy(survey.screenGap),
        ) {
            when {
                uiState.isSchemaLoading -> {
                    Text("Загрузка анкеты...", fontSize = survey.statusFontSize, fontWeight = FontWeight.Medium, color = survey.textBody)
                }

                currentSection == null -> {
                    Text("Схема анкеты недоступна", fontSize = survey.statusFontSize, fontWeight = FontWeight.Medium, color = survey.textBody)
                }

                else -> {
                    Text(currentSection.title, fontSize = survey.sectionTitleFontSize, fontWeight = FontWeight.Bold, color = survey.textStrong)

                    val visibleQuestions = currentSection.questions.filterNot { shouldHideSurveyQuestion(it.id, it.title) }
                    visibleQuestions.forEach { question ->
                        SurveyQuestionCard(
                            question = question,
                            selected = answers[question.id].orEmpty(),
                            onSelect = { option -> viewModel.selectAnswer(question, option) },
                        )
                    }
                }
            }
        }

        SurveyHeader(
            sectionIndex = sectionIndex,
            sectionCount = surveySections.size.coerceAtLeast(1),
            progress = progress,
            allowSkip = allowSkip,
            onSkip = onSkip,
            onBack = {
                if (sectionIndex == 0) onBack() else viewModel.previousSection()
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (currentSection != null) {
            uiState.saveError?.let { message ->
                Text(
                    text = message,
                    fontSize = survey.errorFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = survey.errorTextColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = survey.errorHorizontalPadding)
                        .padding(bottom = survey.errorBottomPadding)
                        .clip(RoundedCornerShape(survey.errorRadius))
                        .background(survey.errorSurfaceColor.copy(alpha = survey.errorSurfaceAlpha))
                        .padding(horizontal = survey.errorContentHorizontalPadding, vertical = survey.errorContentVerticalPadding),
                )
            }

            SurveyFooter(
                isLast = sectionIndex == surveySections.lastIndex,
                isLoading = uiState.isSaving,
                modifier = Modifier.align(Alignment.BottomCenter),
                onContinue = {
                    if (uiState.isSaving) return@SurveyFooter
                    viewModel.clearSaveError()

                    if (sectionIndex == surveySections.lastIndex) {
                        val normalizedAnswers = answers
                            .filterKeys { !shouldHideSurveyQuestion(it, "") }
                            .mapValues { it.value.toList() }

                        if (normalizedAnswers.isEmpty()) {
                            viewModel.savePassport(emptyMap())
                            return@SurveyFooter
                        }

                        viewModel.savePassport(normalizedAnswers)
                    } else {
                        viewModel.nextSection()
                    }
                },
            )
        }

        if (uiState.showSensorPrompt) {
            SensorOnboardingPrompt(
                modifier = Modifier.align(Alignment.BottomCenter),
                onConnect = { saveSensorOwnership(true) },
                onSkip = { saveSensorOwnership(false) },
            )
        }
    }
}
