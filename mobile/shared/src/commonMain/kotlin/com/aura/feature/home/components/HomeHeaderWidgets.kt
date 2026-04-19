package com.aura.feature.home.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.data.api.model.HomeTopWidget
import com.aura.core.data.api.model.HomeWeather
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.theme.aura
@Composable
fun HeaderSection(
    userName: String,
    textPrimary: Color,
    textSecondary: Color,
    dark: Boolean,
    weather: HomeWeather,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.aura.home
    val temperature = if (isLoading) "..." else weather.temperature.takeIf { it.isNotBlank() } ?: "—"
    val uv = if (isLoading) "..." else weather.uv_index.takeIf { it.isNotBlank() } ?: "UV —"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = StringsRu.Home.today, fontSize = tokens.todayFontSize, fontWeight = FontWeight.Medium, color = textSecondary, modifier = Modifier.alpha(0.8f))
            Spacer(modifier = Modifier.height(tokens.smallGap))
            Text(text = "${StringsRu.Home.goodMorningPrefix},\n$userName", fontSize = tokens.greetingFontSize, fontWeight = FontWeight.Bold, color = textPrimary, lineHeight = tokens.greetingLineHeight)
        }
        Column(
            modifier = Modifier
                .size(width = tokens.weatherCardWidth, height = tokens.weatherCardHeight)
                .glassPanel(RoundedCornerShape(tokens.insightCardRadius), dark = dark)
                .padding(tokens.metricHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = Icons.Rounded.WbSunny, contentDescription = StringsRu.Home.weather, tint = tokens.sunColor, modifier = Modifier.size(tokens.weatherIconSize))
            Spacer(modifier = Modifier.height(tokens.smallGap))
            Text(text = temperature, fontSize = tokens.smallFontSize, fontWeight = FontWeight.Bold, color = if (dark) tokens.textBodyDark else tokens.slate700)
            Spacer(modifier = Modifier.height(tokens.smallGap))
            Box(modifier = Modifier.background(tokens.uvSurfaceColor, RoundedCornerShape(percent = 50)).padding(horizontal = tokens.weatherUvHorizontalPadding, vertical = tokens.weatherUvVerticalPadding)) {
                Text(text = uv, fontSize = tokens.microFontSize, fontWeight = FontWeight.Medium, color = tokens.errorColor)
            }
        }
    }
}

@Composable
fun TopWidgetSection(
    textSecondary: Color,
    textBody: Color,
    dark: Boolean,
    topWidget: HomeTopWidget,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.aura.home
    val humidityValue = if (isLoading) "..." else topWidget.humidity_value.takeIf { it.isNotBlank() } ?: "—"
    val humiditySubtitle = if (isLoading) "Загрузка..." else topWidget.humidity_subtitle.takeIf { it.isNotBlank() } ?: StringsRu.Home.humiditySubtitle
    val airQuality = if (isLoading) "..." else topWidget.air_quality.takeIf { it.isNotBlank() } ?: StringsRu.Home.airGood
    val weatherAdvice = if (isLoading) "Анализируем погоду..." else topWidget.weather_advice.takeIf { it.isNotBlank() } ?: humiditySubtitle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(RoundedCornerShape(tokens.cardRadius), dark = dark)
            .padding(tokens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.cardGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.metricGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeatherMetricBlock(
                label = "Влажность",
                value = humidityValue,
                icon = Icons.Rounded.WaterDrop,
                iconTint = MaterialTheme.aura.home.softBlue,
                textBody = textBody,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f),
            )
            WeatherMetricBlock(
                label = "Воздух",
                value = airQuality,
                icon = Icons.Rounded.Air,
                iconTint = tokens.successColor,
                textBody = textBody,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = weatherAdvice,
            fontSize = tokens.smallFontSize,
            lineHeight = tokens.smallLineHeight,
            color = textSecondary,
        )
    }
}

@Composable
fun WeatherMetricBlock(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    textBody: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.aura.home
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = tokens.metricSurfaceAlpha), RoundedCornerShape(tokens.metricRadius))
            .padding(horizontal = tokens.metricHorizontalPadding, vertical = tokens.metricVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(tokens.metricIconBoxSize)
                .background(iconTint.copy(alpha = tokens.metricIconBoxAlpha), RoundedCornerShape(tokens.metricIconBoxRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(tokens.metricIconSize))
        }
        Spacer(modifier = Modifier.width(tokens.metricTextGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = tokens.metricLabelFontSize, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = value, fontSize = tokens.metricValueFontSize, fontWeight = FontWeight.Bold, color = textBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
