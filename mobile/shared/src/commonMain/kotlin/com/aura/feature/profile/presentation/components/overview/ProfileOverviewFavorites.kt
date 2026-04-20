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
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.data.api.model.RecommendationFavorite
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp

@Composable
fun FavoriteRecommendationsSection(
    favorites: List<RecommendationFavorite>,
    loading: Boolean,
    error: String?,
    textBody: Color,
    textSecondary: Color,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    onOpenFavoriteRecommendation: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = auraTokenDp(24f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Избранные рекомендации", fontSize = auraTokenSp(18f), fontWeight = FontWeight.SemiBold, color = textBody)
            Text(
                text = when {
                    loading -> "Загрузка"
                    favorites.isNotEmpty() -> "${favorites.size} сохранено"
                    else -> "Пока пусто"
                },
                fontSize = auraTokenSp(12f),
                fontWeight = FontWeight.Medium,
                color = textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(auraTokenDp(16f)))
        when {
            loading -> Text(
                text = "Загружаем сохранённые линейки...",
                color = textSecondary,
                fontSize = auraTokenSp(13f),
                modifier = Modifier.padding(horizontal = auraTokenDp(24f)),
            )
            error != null -> Text(
                text = error,
                color = MaterialTheme.aura.profile.coral500,
                fontSize = auraTokenSp(13f),
                modifier = Modifier.padding(horizontal = auraTokenDp(24f)),
            )
            favorites.isEmpty() -> FavoriteRecommendationEmptyCard(glassAlpha, cardBorder, dark)
            else -> Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = auraTokenDp(24f)),
                horizontalArrangement = Arrangement.spacedBy(auraTokenDp(16f)),
            ) {
                favorites.forEach { favorite ->
                    FavoriteRecommendationCard(
                        favorite = favorite,
                        textBody = textBody,
                        textSecondary = textSecondary,
                        glassAlpha = glassAlpha,
                        cardBorder = cardBorder,
                        dark = dark,
                        onClick = { if (favorite.favoriteId.isNotBlank()) onOpenFavoriteRecommendation(favorite.favoriteId) },
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteRecommendationEmptyCard(glassAlpha: Float, cardBorder: Color, dark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = auraTokenDp(24f))
            .clip(RoundedCornerShape(auraTokenDp(18f)))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(auraTokenDp(1f), cardBorder, RoundedCornerShape(auraTokenDp(18f)))
            .padding(auraTokenDp(16f)),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(6f)),
    ) {
        Text("Сохранённых рекомендаций пока нет", color = if (dark) auraHex(0xFFCBD5E1) else MaterialTheme.aura.profile.slate700, fontWeight = FontWeight.Bold)
        Text("Соберите линейку ухода и нажмите «Сохранить в избранное».", color = if (dark) auraHex(0xFF94A3B8) else MaterialTheme.aura.profile.slate500, fontSize = auraTokenSp(13f), lineHeight = auraTokenSp(18f))
    }
}

@Composable
fun FavoriteRecommendationCard(
    favorite: RecommendationFavorite,
    textBody: Color,
    textSecondary: Color,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val routineCount = favorite.lines.sumOf { line ->
        line.routine.morning.size + line.routine.evening.size + line.routine.weekly.size
    }
    val savedDate = favorite.savedAt.take(10).ifBlank { "Дата не указана" }
    Column(
        modifier = Modifier
            .width(auraTokenDp(260f))
            .height(auraTokenDp(154f))
            .clip(RoundedCornerShape(auraTokenDp(18f)))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(auraTokenDp(1f), cardBorder, RoundedCornerShape(auraTokenDp(18f)))
            .clickable { onClick() }
            .padding(auraTokenDp(16f)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
            Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f)), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(auraTokenDp(34f)).background(MaterialTheme.aura.profile.themePinkSoft.copy(alpha = if (dark) 0.18f else 1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.aura.profile.themePink, modifier = Modifier.size(auraTokenDp(18f)))
                }
                Text(savedDate, color = textSecondary, fontSize = auraTokenSp(12f), fontWeight = FontWeight.SemiBold)
            }
            Text(
                favorite.summary.title.ifBlank { "Сохранённая линейка" },
                color = textBody,
                fontWeight = FontWeight.Bold,
                fontSize = auraTokenSp(15f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = auraTokenSp(18f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("$routineCount средств", color = textSecondary, fontSize = auraTokenSp(12f))
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = textSecondary)
        }
    }
}
