package com.aura.feature.survey.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.core.data.api.model.SurveyQuestionSchema
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.auraToolbarTopOffset
import com.aura.core.ui.theme.aura

@Composable
fun SensorOnboardingPrompt(
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val survey = MaterialTheme.aura.survey

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = survey.promptOuterHorizontalPadding, vertical = survey.promptOuterVerticalPadding)
            .shadow(elevation = survey.promptShadowElevation, shape = RoundedCornerShape(survey.glassRadius), spotColor = survey.emerald500.copy(alpha = survey.promptShadowAlpha))
            .surveyGlassCard()
            .padding(survey.promptPadding),
        verticalArrangement = Arrangement.spacedBy(survey.promptGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(survey.promptHeaderGap)) {
            Box(
                modifier = Modifier
                    .size(survey.promptIconBoxSize)
                    .background(survey.emerald100.copy(alpha = survey.promptIconBoxAlpha), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = survey.emerald600, modifier = Modifier.size(survey.promptIconSize))
            }
            Column(verticalArrangement = Arrangement.spacedBy(survey.promptTextGap), modifier = Modifier.weight(1f)) {
                Text("Подключить журнал замеров", fontSize = survey.promptTitleFontSize, fontWeight = FontWeight.Bold, color = survey.textStrong)
                Text("Если используете датчик, откроем журнал кожи для первого замера.", fontSize = survey.promptSubtitleFontSize, fontWeight = FontWeight.Medium, color = survey.textMuted)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(survey.promptButtonGap), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(survey.promptButtonHeight)
                    .clip(CircleShape)
                    .background(survey.glassSurfaceColor.copy(alpha = survey.promptSecondaryAlpha))
                    .border(survey.glassBorderWidth, survey.glassBorderColor.copy(alpha = survey.promptSecondaryBorderAlpha), CircleShape)
                    .clickable { onSkip() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Позже", fontSize = survey.promptButtonFontSize, fontWeight = FontWeight.Bold, color = survey.textBody)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(survey.promptButtonHeight)
                    .shadow(elevation = survey.promptPrimaryShadowElevation, shape = CircleShape, spotColor = survey.vibrantPink.copy(alpha = survey.promptPrimaryShadowAlpha))
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(survey.dustyRose, survey.vibrantPink)))
                    .clickable { onConnect() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("К замеру", fontSize = survey.promptButtonFontSize, fontWeight = FontWeight.Bold, color = survey.glassSurfaceColor)
            }
        }
    }
}

fun shouldHideSurveyQuestion(id: String, title: String): Boolean {
    val key = "$id $title".lowercase()
    return id == "budget" || key.contains("бюджет") || key.contains("budget")
}

@Composable
fun SurveyHeader(
    sectionIndex: Int,
    sectionCount: Int,
    progress: Float,
    allowSkip: Boolean,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val survey = MaterialTheme.aura.survey

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(survey.glassSurfaceColor.copy(alpha = survey.headerSurfaceAlpha))
            .border(survey.glassBorderWidth, survey.glassBorderColor.copy(alpha = survey.headerBorderAlpha)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = survey.headerHorizontalPadding,
                    end = survey.headerHorizontalPadding,
                    top = auraToolbarTopOffset(),
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(survey.headerHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(survey.headerIconButtonSize)) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = StringsRu.Common.back, tint = survey.textBody)
                }
                Text(StringsRu.Survey.title, fontSize = survey.headerTitleFontSize, fontWeight = FontWeight.Bold, color = survey.textStrong)
                if (allowSkip) {
                    Text(
                        text = StringsRu.Common.skip,
                        fontSize = survey.skipFontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = survey.textBody,
                        modifier = Modifier
                            .clip(RoundedCornerShape(survey.skipRadius))
                            .clickable { onSkip() }
                            .padding(horizontal = survey.skipHorizontalPadding, vertical = survey.skipVerticalPadding),
                    )
                } else {
                    Spacer(modifier = Modifier.size(survey.headerIconButtonSize))
                }
            }

            Column(modifier = Modifier.padding(horizontal = survey.progressHorizontalPadding, vertical = survey.progressVerticalPadding)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(StringsRu.Survey.progress, fontSize = survey.progressLabelFontSize, fontWeight = FontWeight.Bold, color = survey.emerald700, letterSpacing = survey.progressLetterSpacing)
                    Text("${sectionIndex + 1} ИЗ $sectionCount ${StringsRu.Survey.blocksSuffix}", fontSize = survey.progressLabelFontSize, fontWeight = FontWeight.Bold, color = survey.textMuted, letterSpacing = survey.progressLetterSpacing)
                }
                Spacer(modifier = Modifier.height(survey.progressBarTopGap))
                Box(modifier = Modifier.fillMaxWidth().height(survey.progressBarHeight).clip(CircleShape).background(survey.glassSurfaceColor.copy(alpha = survey.progressTrackAlpha))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(survey.emerald500))
                }
            }
        }
    }
}

@Composable
fun SurveyQuestionCard(
    question: SurveyQuestionSchema,
    selected: Set<String>,
    onSelect: (String) -> Unit,
) {
    val survey = MaterialTheme.aura.survey

    Column(verticalArrangement = Arrangement.spacedBy(survey.questionGap)) {
        Text(question.title, fontSize = survey.questionTitleFontSize, fontWeight = FontWeight.Bold, color = survey.textStrong)
        if (question.subtitle.isNotBlank()) {
            Text(question.subtitle, fontSize = survey.questionSubtitleFontSize, fontWeight = FontWeight.Medium, color = survey.textMuted)
        }

        Column(verticalArrangement = Arrangement.spacedBy(survey.optionsGap)) {
            question.options.forEachIndexed { index, option ->
                val selectedState = selected.contains(option)
                val icon = surveyOptionIcon(question, option, index)
                SurveyOptionRow(
                    title = option,
                    icon = icon,
                    isSelected = selectedState,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

fun surveyOptionIcon(question: SurveyQuestionSchema, option: String, index: Int): ImageVector {
    val key = "${question.id} ${question.title} $option".lowercase()
    return when {
        key.contains("sun") || key.contains("солн") || key.contains("spf") -> Icons.Rounded.WbSunny
        key.contains("dry") || key.contains("сух") || key.contains("hydr") || key.contains("увлаж") -> Icons.Rounded.WaterDrop
        key.contains("oil") || key.contains("жир") || key.contains("комбини") -> Icons.Rounded.InvertColors
        key.contains("acne") || key.contains("акне") || key.contains("прыщ") || key.contains("воспал") -> Icons.Rounded.LocalHospital
        key.contains("sensitive") || key.contains("чувств") || key.contains("reaction") || key.contains("реакц") -> Icons.Rounded.Warning
        key.contains("goal") || key.contains("цель") || key.contains("tone") || key.contains("тон") -> Icons.Rounded.Verified
        key.contains("age") || key.contains("возраст") || key.contains("elastic") || key.contains("эласт") -> Icons.Rounded.Face
        key.contains("health") || key.contains("barrier") || key.contains("барьер") -> Icons.Rounded.HealthAndSafety
        else -> when (index % 4) {
            0 -> Icons.Rounded.AutoAwesome
            1 -> Icons.Rounded.Face
            2 -> Icons.Rounded.WaterDrop
            else -> Icons.Rounded.Verified
        }
    }
}

@Composable
fun SurveyOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val survey = MaterialTheme.aura.survey

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surveyGlassCard(isSelected)
            .clickable { onClick() }
            .padding(survey.optionPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(survey.optionIconBoxSize)
                .background(if (isSelected) survey.emerald500 else survey.emerald100.copy(alpha = survey.optionIconBoxAlpha), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) survey.glassSurfaceColor else survey.emerald600,
                modifier = Modifier.size(survey.optionIconSize),
            )
        }

        Spacer(modifier = Modifier.size(survey.optionContentGap))

        Text(
            text = title,
            fontSize = survey.optionTextFontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) survey.emerald800 else survey.textBody,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .size(survey.optionCheckSize)
                .background(if (isSelected) survey.emerald500 else Color.Transparent, CircleShape)
                .border(
                    width = if (isSelected) survey.optionCheckSelectedBorderWidth else survey.optionCheckBorderWidth,
                    color = if (isSelected) Color.Transparent else survey.borderMuted,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(modifier = Modifier.size(survey.optionCheckDotSize).background(survey.glassSurfaceColor, CircleShape))
            }
        }
    }
}

@Composable
fun SurveyFooter(
    isLast: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit,
) {
    val survey = MaterialTheme.aura.survey

    Box(modifier = modifier.fillMaxWidth().background(survey.glassSurfaceColor.copy(alpha = survey.footerBlurSurfaceAlpha)).blur(survey.footerBlurRadius))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(survey.glassBorderWidth, survey.glassBorderColor.copy(alpha = survey.footerBorderAlpha), RoundedCornerShape(topStart = survey.footerRadius, topEnd = survey.footerRadius))
            .padding(horizontal = survey.footerHorizontalPadding, vertical = survey.footerVerticalPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(survey.footerButtonHeight)
                .shadow(elevation = survey.footerButtonShadowElevation, shape = CircleShape, spotColor = survey.vibrantPink.copy(alpha = survey.footerButtonShadowAlpha))
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(survey.dustyRose, survey.vibrantPink)))
                .clickable(enabled = !isLoading) { onContinue() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isLoading) {
                    StringsRu.Common.saving
                } else if (isLast) {
                    StringsRu.Common.completeSurvey
                } else {
                    StringsRu.Common.continueAction
                },
                fontSize = survey.footerButtonFontSize,
                fontWeight = FontWeight.Bold,
                color = survey.glassSurfaceColor,
                letterSpacing = survey.footerButtonLetterSpacing,
            )
            Spacer(modifier = Modifier.size(survey.footerButtonIconGap))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = survey.glassSurfaceColor)
        }
    }
}

@Composable
fun Modifier.surveyGlassCard(isSelected: Boolean = false): Modifier {
    val survey = MaterialTheme.aura.survey
    val bgColor = if (isSelected) survey.emerald50.copy(alpha = survey.glassSelectedAlpha) else survey.glassSurfaceColor.copy(alpha = survey.glassDefaultAlpha)
    val borderColor = if (isSelected) survey.selectedBorderColor.copy(alpha = survey.glassSelectedBorderAlpha) else survey.glassBorderColor.copy(alpha = survey.glassDefaultBorderAlpha)
    return this
        .clip(RoundedCornerShape(survey.glassRadius))
        .background(bgColor)
        .border(survey.glassBorderWidth, borderColor, RoundedCornerShape(survey.glassRadius))
}
