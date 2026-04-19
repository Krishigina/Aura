package com.aura.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.domain.model.SkinJournalReminder
import com.aura.core.ui.theme.aura
import com.aura.feature.journal.skinJournalTodayStatus

@Composable
fun TodayNoRoutineStepsCard(
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.home
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.cardRadius), dark = dark)
            .padding(tokens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.cardGapSmall),
    ) {
        Text("Уход на сегодня", fontSize = tokens.cardTitleFontSize, fontWeight = FontWeight.Bold, color = textBody)
        Text("Сегодня по вашей рутине шагов нет", fontSize = tokens.bodyFontSize, color = textSecondary)
    }
}

@Composable
fun RoutineSetupCard(
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
    onOpenSettings: () -> Unit,
) {
    val tokens = MaterialTheme.aura.home
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.cardRadius), dark = dark)
            .padding(tokens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.metricGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ритуал не настроен",
                fontSize = tokens.setupTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = textBody,
            )
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.aura.home.pink)
        }
        Text(
            text = "Добавьте шаги ухода в настройках профиля, чтобы получать персональный чеклист на главной.",
            fontSize = tokens.smallFontSize,
            lineHeight = tokens.bodyLineHeight,
            color = textSecondary,
        )
        TextButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(tokens.setupButtonRadius),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.aura.home.pink),
        ) {
            Text("Открыть настройки профиля", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SkinJournalTodayWidget(
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
    activeReminders: List<SkinJournalReminder>,
    onOpen: () -> Unit,
) {
    val tokens = MaterialTheme.aura.home
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.largeCardRadius), dark = dark)
            .clickable { onOpen() }
            .padding(tokens.largeCardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.cardGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Сегодня в журнале", fontSize = tokens.cardTitleFontSize, fontWeight = FontWeight.Bold, color = textBody)
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.aura.home.peach)
        }
        Text(skinJournalTodayStatus(activeReminders), fontSize = tokens.bodyFontSize, color = textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.cardGapSmall)) {
            listOf("Замер" to MaterialTheme.aura.home.softBlue, "Процедура" to MaterialTheme.aura.home.softPink).forEach { (label, color) ->
                Box(
                    modifier = Modifier
                        .background(color, RoundedCornerShape(tokens.pillRadius))
                        .padding(horizontal = tokens.pillHorizontalPadding, vertical = tokens.pillVerticalPadding),
                ) {
                    Text(label, color = tokens.slate700, fontSize = tokens.smallFontSize, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RecommendationEntryCard(
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
    onOpen: () -> Unit,
) {
    val tokens = MaterialTheme.aura.home
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.largeCardRadius), dark = dark)
            .padding(tokens.largeCardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.cardGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Персональная линейка ухода", fontSize = tokens.cardTitleFontSize, fontWeight = FontWeight.Bold, color = textBody)
                Spacer(modifier = Modifier.height(tokens.cardGapSmall))
                Text(
                    "Aura учтет анкету, замеры, процедуры и совместимость продуктов.",
                    fontSize = tokens.bodyFontSize,
                    color = textSecondary,
                    lineHeight = tokens.bodyLineHeight,
                )
            }
            Box(
                modifier = Modifier
                    .size(tokens.recommendationIconSize)
                    .background(tokens.pink.copy(alpha = if (dark) tokens.recommendationIconDarkAlpha else tokens.recommendationIconLightAlpha), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.aura.home.pink)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(tokens.cardGapSmall)) {
            listOf("Анкета", "Замеры", "Процедуры").forEach { label ->
                Box(
                    modifier = Modifier
                        .background(tokens.softPink, RoundedCornerShape(tokens.pillRadius))
                        .padding(horizontal = tokens.pillHorizontalPadding, vertical = tokens.pillVerticalPadding),
                ) {
                    Text(label, color = tokens.slate700, fontSize = tokens.smallFontSize, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(tokens.buttonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.aura.home.pink, contentColor = Color.White),
        ) {
            Text("Создать рекомендацию", fontWeight = FontWeight.Bold)
        }
    }
}
