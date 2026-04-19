package com.aura.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.data.api.model.HomeInsightItem
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.defaultAuraDesignTokens
@Composable
fun AiInsightsSection(
    textPrimary: Color,
    textSecondary: Color,
    textBody: Color,
    dark: Boolean,
    insights: List<HomeInsightItem>,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.aura.home
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.ritualHeaderHorizontalPadding).padding(bottom = tokens.insightsHeaderBottomPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(text = StringsRu.Home.aiInsights, fontSize = tokens.cardTitleFontSize, fontWeight = FontWeight.Bold, color = textPrimary)
            Text(text = StringsRu.Home.viewAll, fontSize = tokens.smallFontSize, fontWeight = FontWeight.SemiBold, color = tokens.slate700)
        }
        when {
            isLoading -> {
                Text(
                    text = "Загрузка инсайтов...",
                    fontSize = tokens.bodyFontSize,
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = tokens.ritualHeaderHorizontalPadding),
                )
            }

            insights.isEmpty() -> {
                Text(
                    text = "Инсайты появятся после публикации контента",
                    fontSize = tokens.bodyFontSize,
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = tokens.ritualHeaderHorizontalPadding),
                )
            }

            else -> {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(tokens.insightsRowGap)) {
                    insights.forEach { insight ->
                        val (icon, iconTint, bgTint) = when (insight.type.lowercase()) {
                            "result" -> Triple(Icons.Rounded.Star, tokens.successColor, tokens.successColor.copy(alpha = tokens.insightBgAlpha))
                            "hydration" -> Triple(Icons.Rounded.WaterDrop, tokens.hydrationColor, tokens.hydrationBgColor.copy(alpha = tokens.insightBgAlpha))
                            else -> Triple(Icons.Rounded.Face, tokens.pink, tokens.softPink)
                        }

                        InsightCard(
                            icon = icon,
                            iconTint = iconTint,
                            bgTint = bgTint,
                            title = insight.title,
                            subtitle = insight.subtitle,
                            textBody = textBody,
                            textSecondary = textSecondary,
                            dark = dark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCard(icon: ImageVector, iconTint: Color, bgTint: Color, title: String, subtitle: String, textBody: Color, textSecondary: Color, dark: Boolean) {
    val tokens = MaterialTheme.aura.home
    Column(
        modifier = Modifier.width(tokens.insightCardWidth).height(tokens.insightCardHeight).glassPanel(RoundedCornerShape(tokens.insightCardRadius), dark = dark).padding(tokens.insightCardPadding),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.size(tokens.insightIconBoxSize).background(bgTint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(tokens.insightIconSize))
        }
        Column {
            Text(text = title, fontSize = tokens.bodyFontSize, fontWeight = FontWeight.Bold, color = textBody, lineHeight = tokens.insightTitleLineHeight)
            Spacer(modifier = Modifier.height(tokens.smallGap))
            Text(text = subtitle, fontSize = tokens.microFontSize, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
fun Modifier.glassPanel(shape: Shape, dark: Boolean): Modifier {
    val tokens = defaultAuraDesignTokens().home
    val bgAlpha = if (dark) tokens.glassDarkAlpha else tokens.glassLightAlpha
    val borderAlpha = if (dark) tokens.glassBorderDarkAlpha else tokens.glassBorderLightAlpha
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = bgAlpha))
        .border(tokens.glassBorderWidth, Color.White.copy(alpha = borderAlpha), shape)
}
