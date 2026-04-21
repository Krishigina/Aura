package com.aura.feature.recommendations.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.data.api.model.RecommendationInputQuality
import com.aura.core.data.api.model.RecommendationLine
import com.aura.core.data.api.model.RecommendationResponse
import com.aura.core.data.api.model.RecommendationStep
import com.aura.core.ui.theme.AuraRecommendationsTokens
import com.aura.core.ui.theme.aura
import com.aura.feature.home.components.glassPanel
import com.aura.feature.product.scoreBreakdownRows

@Composable
fun LoadingPanel(textBody: Color, textSecondary: Color, dark: Boolean) {
    val tokens = MaterialTheme.aura.recommendations
    Column(
        modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(tokens.loadingPanelRadius), dark = dark).padding(tokens.loadingPanelPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.loadingPanelGap),
    ) {
        CircularProgressIndicator(color = tokens.sky)
        Text("Собираем персональную линейку", color = textBody, fontWeight = FontWeight.Bold)
        Text("Учитываем анкету, замеры, процедуры и совместимость продуктов.", color = textSecondary, fontSize = tokens.statusFontSize)
    }
}

@Composable
fun ErrorPanel(
    message: String,
    needsPassport: Boolean,
    onOpenSurvey: () -> Unit,
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.recommendations
    Column(
        modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(tokens.loadingPanelRadius), dark = dark).padding(tokens.errorPanelPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.errorPanelGap),
    ) {
        Icon(if (needsPassport) Icons.Rounded.Info else Icons.Rounded.ErrorOutline, contentDescription = null, tint = tokens.peach)
        Text(message, color = textBody, fontSize = tokens.titleFontSize, fontWeight = FontWeight.Bold)
        Text("Aura сможет подобрать линейку после базовых данных о коже.", color = textSecondary, fontSize = tokens.bodyFontSize, lineHeight = tokens.bodyLineHeight)
        if (needsPassport) {
            Button(
                onClick = onOpenSurvey,
                shape = RoundedCornerShape(tokens.saveButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.violet, contentColor = Color.White),
            ) {
                Text("Пройти анкету", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SummaryCard(response: RecommendationResponse, textBody: Color, textSecondary: Color, dark: Boolean) {
    val tokens = MaterialTheme.aura.recommendations
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.summaryRadius), dark = dark)
            .background(tokens.sky.copy(alpha = if (dark) tokens.summarySurfaceDarkAlpha else tokens.summarySurfaceLightAlpha), RoundedCornerShape(tokens.summaryRadius))
            .padding(tokens.summaryPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.summaryGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.summaryHeaderGap)) {
            Box(modifier = Modifier.size(tokens.summaryIconSize).background(tokens.lavender.copy(alpha = tokens.summaryIconAlpha), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = tokens.violet)
            }
            Text(response.summary.title.ifBlank { "Персональная стратегия" }, color = textBody, fontSize = tokens.titleFontSize, fontWeight = FontWeight.Bold)
        }
        Text(response.summary.description.ifBlank { "Подборка ухода сформирована по текущим данным Aura." }, color = textSecondary, fontSize = tokens.bodyFontSize, lineHeight = tokens.summaryDescriptionLineHeight)
    }
}

@Composable
fun InputQualityChips(inputQuality: RecommendationInputQuality, textBody: Color, textSecondary: Color, dark: Boolean) {
    val tokens = MaterialTheme.aura.recommendations
    Column(verticalArrangement = Arrangement.spacedBy(tokens.sectionGap)) {
        Text("Качество входных данных", color = textBody, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(tokens.chipGap)) {
            QualityChip("Анкета", inputQuality.skinPassport, textSecondary, dark)
            QualityChip("Замеры", inputQuality.sensorReadings, textSecondary, dark)
            QualityChip("Процедуры", inputQuality.procedures, textSecondary, dark)
        }
        inputQuality.notes.takeIf { it.isNotEmpty() }?.let { notes ->
            Text(notes.joinToString(" · "), color = textSecondary, fontSize = tokens.smallTextFontSize)
        }
    }
}

@Composable
fun QualityChip(label: String, value: String, textSecondary: Color, dark: Boolean) {
    val tokens = MaterialTheme.aura.recommendations
    val available = value == "available"
    Row(
        modifier = Modifier.glassPanel(RoundedCornerShape(50), dark = dark).padding(horizontal = tokens.chipHorizontalPadding, vertical = tokens.chipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.chipIconGap),
    ) {
        Icon(if (available) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, contentDescription = null, tint = if (available) tokens.mint else tokens.peach, modifier = Modifier.size(tokens.chipIconSize))
        Text("$label: ${qualityLabel(value)}", color = textSecondary, fontSize = tokens.smallTextFontSize, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LineSelector(
    lines: List<RecommendationLine>,
    selectedKey: String?,
    onSelect: (RecommendationLine) -> Unit,
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.recommendations
    Column(verticalArrangement = Arrangement.spacedBy(tokens.sectionGap)) {
        Text("Варианты линейки", color = textBody, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(tokens.sectionGap)) {
            lines.forEach { line ->
                val selected = line.key == selectedKey
                Column(
                    modifier = Modifier
                        .width(tokens.lineCardWidth)
                        .glassPanel(RoundedCornerShape(tokens.lineCardRadius), dark = dark)
                        .background(if (selected) lineAccent(line.key, tokens).copy(alpha = tokens.lineSelectedAlpha) else Color.Transparent, RoundedCornerShape(tokens.lineCardRadius))
                        .clickable { onSelect(line) }
                        .padding(tokens.lineCardPadding),
                    verticalArrangement = Arrangement.spacedBy(tokens.lineCardGap),
                ) {
                    Text(line.title, color = textBody, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(line.positioning.ifBlank { "Персональный вариант" }, color = textSecondary, fontSize = tokens.smallTextFontSize, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun LineDossier(line: RecommendationLine, textBody: Color, textSecondary: Color, dark: Boolean, onProductOpen: (Int) -> Unit) {
    val tokens = MaterialTheme.aura.recommendations
    Column(
        modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(tokens.dossierRadius), dark = dark).padding(tokens.dossierPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.dossierGap),
    ) {
        Text(line.title, color = textBody, fontSize = tokens.dossierTitleFontSize, fontWeight = FontWeight.Bold)
        if (line.positioning.isNotBlank()) {
            Text(line.positioning, color = textSecondary, fontSize = tokens.bodyFontSize, lineHeight = tokens.bodyLineHeight)
        }
        RoutineSection("Утро", line.routine.morning, tokens.peach, textBody, textSecondary, dark, onProductOpen)
        RoutineSection("Вечер", line.routine.evening, tokens.violet, textBody, textSecondary, dark, onProductOpen)
        RoutineSection("1-2 раза в неделю", line.routine.weekly, tokens.mint, textBody, textSecondary, dark, onProductOpen)
    }
}

@Composable
fun RoutineSection(title: String, steps: List<RecommendationStep>, accent: Color, textBody: Color, textSecondary: Color, dark: Boolean, onProductOpen: (Int) -> Unit) {
    val tokens = MaterialTheme.aura.recommendations
    Column(verticalArrangement = Arrangement.spacedBy(tokens.sectionGap)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.routineHeaderGap)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = accent, modifier = Modifier.size(tokens.routineIconSize))
            Text(title, color = textBody, fontWeight = FontWeight.Bold)
        }
        if (steps.isEmpty()) {
            Text("Шагов пока нет", color = textSecondary, fontSize = tokens.statusFontSize)
        } else {
            steps.forEach { step -> ProductStepCard(step = step, accent = accent, textBody = textBody, textSecondary = textSecondary, dark = dark, onProductOpen = onProductOpen) }
        }
    }
}

@Composable
fun ProductStepCard(step: RecommendationStep, accent: Color, textBody: Color, textSecondary: Color, dark: Boolean, onProductOpen: (Int) -> Unit) {
    val tokens = MaterialTheme.aura.recommendations
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.productCardRadius), dark = dark)
            .background(accent.copy(alpha = if (dark) tokens.productCardDarkAlpha else tokens.productCardLightAlpha), RoundedCornerShape(tokens.productCardRadius))
            .clickable { if (step.productId > 0) onProductOpen(step.productId) }
            .padding(tokens.productCardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.productCardGap),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(tokens.productContentGap), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(tokens.sequenceBadgeSize).background(accent.copy(alpha = tokens.sequenceBadgeAlpha), CircleShape), contentAlignment = Alignment.Center) {
                    Text((step.sequence.takeIf { it > 0 } ?: 1).toString(), color = tokens.textBodyLight, fontSize = tokens.smallTextFontSize, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(step.productName.ifBlank { step.step.ifBlank { "Средство ухода" } }, color = textBody, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOf(step.brand, step.step).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Персональный шаг" }, color = textSecondary, fontSize = tokens.smallTextFontSize)
                }
            }
            Text("${step.compatibilityPercent.coerceIn(0, 100)}%", color = accent, fontWeight = FontWeight.Bold)
        }
        DetailText("Почему", step.reason, textSecondary)
        val skinStateRow = scoreBreakdownRows(step.scoreBreakdown).firstOrNull { it.label == "Состояние кожи" }
        DetailText("Состояние кожи", skinStateRow?.let { "${it.value}/${it.max}" } ?: "0/20", textSecondary)
        step.explanations.take(2).forEach { explanation ->
            Text("• $explanation", color = textSecondary, fontSize = tokens.smallTextFontSize, lineHeight = tokens.smallTextLineHeight)
        }
        DetailText("Как использовать", step.instruction, textSecondary)
        DetailText("Частота", step.frequency, textSecondary)
        step.warnings.forEach { warning ->
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.warningGap), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = tokens.amber, modifier = Modifier.size(tokens.warningIconSize))
                Text(warning, color = textSecondary, fontSize = tokens.smallTextFontSize, lineHeight = tokens.smallTextLineHeight)
            }
        }
    }
}

fun lineAccent(key: String, tokens: AuraRecommendationsTokens): Color = when (key) {
    "budget" -> tokens.mint
    "professional" -> tokens.sky
    "luxury" -> tokens.violet
    "cosmeceutical" -> tokens.peach
    else -> tokens.lavender
}

@Composable
fun DetailText(label: String, value: String, textSecondary: Color) {
    val tokens = MaterialTheme.aura.recommendations
    if (value.isNotBlank()) {
        Text("$label: $value", color = textSecondary, fontSize = tokens.smallTextFontSize, lineHeight = tokens.smallTextLineHeight)
    }
}

@Composable
fun ContextBlock(warnings: List<String>, procedureContext: List<String>, textBody: Color, textSecondary: Color, dark: Boolean) {
    val tokens = MaterialTheme.aura.recommendations
    Column(
        modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(tokens.contextRadius), dark = dark).padding(tokens.contextPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.contextGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.inlineIconGap)) {
            Icon(Icons.Rounded.Spa, contentDescription = null, tint = tokens.amber)
            Text("Контекст процедур и предупреждения", color = textBody, fontWeight = FontWeight.Bold)
        }
        (procedureContext + warnings).distinct().forEach { item ->
            Text("• $item", color = textSecondary, fontSize = tokens.statusFontSize, lineHeight = tokens.contextLineHeight)
        }
    }
}

fun qualityLabel(value: String): String = when (value) {
    "available" -> "есть"
    "missing" -> "нет"
    "none" -> "нет"
    else -> value.ifBlank { "нет" }
}
