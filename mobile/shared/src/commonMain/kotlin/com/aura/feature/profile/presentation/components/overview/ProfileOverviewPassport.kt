package com.aura.feature.profile.presentation.components.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp

@Composable
fun ProfileSection(userName: String, textPrimary: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = auraTokenDp(24f)), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = userName, fontSize = auraTokenSp(24f), fontWeight = FontWeight.Bold, color = textPrimary)
    }
}

@Composable
fun SkinPassportSection(
    skinPassportAnswers: Map<String, List<String>>,
    textBody: Color,
    textSecondary: Color,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    onNavigateToAllParameters: () -> Unit,
    onNavigateToSurvey: () -> Unit,
) {
    val surveyCompleted = skinPassportAnswers.isNotEmpty()
    val skinType = skinPassportAnswers["skin_type"]?.firstOrNull() ?: "Не заполнен"
    val mainIssue = skinPassportAnswers["skin_issues"]?.firstOrNull() ?: "Не определено"
    val sensitivity = skinPassportAnswers["new_products_reaction"]?.firstOrNull() ?: "Не указано"
    val goal = skinPassportAnswers["goals"]?.firstOrNull() ?: "Не выбрана"

    val surveyStatus = if (surveyCompleted) "Анкета пройдена" else "Пройдите анкету"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = auraTokenDp(24f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Паспорт кожи", fontSize = auraTokenSp(18f), fontWeight = FontWeight.SemiBold, color = textBody)
            Text(
                text = "Все параметры",
                fontSize = auraTokenSp(12f),
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(auraTokenDp(10f)))
                    .clickable { onNavigateToAllParameters() }
                    .padding(horizontal = auraTokenDp(8f), vertical = auraTokenDp(4f)),
            )
        }
        Spacer(modifier = Modifier.height(auraTokenDp(16f)))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = auraTokenDp(24f)),
            horizontalArrangement = Arrangement.spacedBy(auraTokenDp(16f)),
        ) {
            PassportCard(Icons.Rounded.WaterDrop, auraHex(0xFF60A5FA), auraHex(0xFFDBEAFE), skinType, "ТИП КОЖИ", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.BubbleChart, auraHex(0xFFF87171), auraHex(0xFFFEE2E2), mainIssue, "КЛЮЧЕВАЯ ПРОБЛЕМА", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.WbSunny, auraHex(0xFFFB923C), auraHex(0xFFFFEDD5), sensitivity, "ЧУВСТВИТЕЛЬНОСТЬ", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.Face, auraHex(0xFFA855F7), auraHex(0xFFF3E8FF), goal, "ЦЕЛЬ УХОДА", glassAlpha, cardBorder, dark)
            PassportCard(
                icon = Icons.Rounded.Assignment,
                color = auraHex(0xFF059669),
                bgColor = auraHex(0xFFD1FAE5),
                title = surveyStatus,
                subtitle = "АНКЕТА",
                glassAlpha = glassAlpha,
                cardBorder = cardBorder,
                dark = dark,
                onClick = onNavigateToSurvey,
            )
        }
    }
}

@Composable
fun PassportCard(
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    title: String,
    subtitle: String,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Column(
        modifier = Modifier
            .width(auraTokenDp(144f))
            .height(auraTokenDp(128f))
            .clip(RoundedCornerShape(auraTokenDp(16f)))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(auraTokenDp(1f), cardBorder, RoundedCornerShape(auraTokenDp(16f)))
            .then(clickableModifier)
            .padding(auraTokenDp(14f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(auraTokenDp(40f))
                .background(bgColor.copy(alpha = auraTokenAlpha(0.5f)), CircleShape)
                .border(auraTokenDp(1f), bgColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(auraTokenDp(20f)))
        }
        Spacer(modifier = Modifier.height(auraTokenDp(12f)))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(auraTokenDp(2f)),
        ) {
            Text(
                text = title,
                fontSize = auraTokenSp(13f),
                fontWeight = FontWeight.SemiBold,
                color = if (dark) auraHex(0xFFCBD5E1) else MaterialTheme.aura.profile.slate700,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = auraTokenSp(16f),
            )
            Text(
                text = subtitle,
                fontSize = auraTokenSp(10f),
                fontWeight = FontWeight.Bold,
                color = if (dark) auraHex(0xFF94A3B8) else MaterialTheme.aura.profile.slate500,
                letterSpacing = auraTokenSp(0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = auraTokenSp(12f),
            )
        }
    }
}
