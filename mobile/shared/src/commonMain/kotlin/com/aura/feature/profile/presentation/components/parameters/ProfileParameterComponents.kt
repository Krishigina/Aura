package com.aura.feature.profile.presentation.components.parameters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
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
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp

internal data class PassportFieldMeta(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val section: String,
)

internal val passportSections = listOf("База кожи", "Чувствительность", "Образ жизни", "Уход", "Цели")

@Composable
internal fun PassportTimelineRow(
    meta: PassportFieldMeta,
    value: String,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    showLine: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(auraTokenDp(34f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(auraTokenDp(22f)))
            Box(
                modifier = Modifier
                    .size(auraTokenDp(34f))
                    .clip(CircleShape)
                    .background(meta.color.copy(alpha = if (dark) 0.22f else 0.16f))
                    .border(auraTokenDp(1f), meta.color.copy(alpha = auraTokenAlpha(0.35f)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(auraTokenDp(18f)))
            }
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(auraTokenDp(2f))
                        .height(auraTokenDp(52f))
                        .background(meta.color.copy(alpha = if (dark) 0.22f else 0.18f), RoundedCornerShape(auraTokenDp(2f))),
                )
            }
        }

        Spacer(modifier = Modifier.width(auraTokenDp(12f)))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showLine) auraTokenDp(10f) else auraTokenDp(0f))
                .clip(RoundedCornerShape(auraTokenDp(20f)))
                .background(Color.White.copy(alpha = glassAlpha))
                .border(auraTokenDp(1f), cardBorder, RoundedCornerShape(auraTokenDp(20f)))
                .padding(horizontal = auraTokenDp(16f), vertical = auraTokenDp(14f)),
            verticalArrangement = Arrangement.spacedBy(auraTokenDp(7f)),
        ) {
            Text(
                text = meta.label.uppercase(),
                fontSize = auraTokenSp(10f),
                fontWeight = FontWeight.Black,
                color = meta.color,
                letterSpacing = auraTokenSp(0.9f),
            )
            Text(
                text = value,
                fontSize = auraTokenSp(15f),
                fontWeight = FontWeight.SemiBold,
                color = if (dark) auraHex(0xFFCBD5E1) else MaterialTheme.aura.profile.slate700,
                lineHeight = auraTokenSp(20f),
            )
        }
    }
}

internal fun passportFieldMeta(key: String): PassportFieldMeta {
    return when (key) {
        "skin_type" -> PassportFieldMeta("Тип кожи", Icons.Rounded.WaterDrop, auraHex(0xFF60A5FA), "База кожи")
        "after_wash" -> PassportFieldMeta("Ощущения после умывания", Icons.Rounded.BubbleChart, auraHex(0xFF38BDF8), "База кожи")
        "pores" -> PassportFieldMeta("Размер пор", Icons.Rounded.BubbleChart, auraHex(0xFFF87171), "База кожи")
        "flaking" -> PassportFieldMeta("Шелушение", Icons.Rounded.HealthAndSafety, auraHex(0xFFF59E0B), "База кожи")
        "age_group" -> PassportFieldMeta("Возрастная группа", Icons.Rounded.Face, auraHex(0xFFA855F7), "База кожи")
        "phototype" -> PassportFieldMeta("Фототип", Icons.Rounded.WbSunny, auraHex(0xFFFB923C), "База кожи")
        "skin_issues" -> PassportFieldMeta("Ключевые проблемы", Icons.Rounded.LocalHospital, auraHex(0xFFF43F5E), "Чувствительность")
        "new_products_reaction" -> PassportFieldMeta("Реакция на новые средства", Icons.Rounded.Warning, auraHex(0xFFFB923C), "Чувствительность")
        "allergy", "allergies" -> PassportFieldMeta("Аллергии", Icons.Rounded.HealthAndSafety, auraHex(0xFFEF4444), "Чувствительность")
        "triggers" -> PassportFieldMeta("Триггеры", Icons.Rounded.Warning, auraHex(0xFFF97316), "Чувствительность")
        "diagnosis" -> PassportFieldMeta("Диагнозы кожи", Icons.Rounded.LocalHospital, auraHex(0xFFDC2626), "Чувствительность")
        "climate" -> PassportFieldMeta("Климат", Icons.Rounded.WbSunny, auraHex(0xFFF59E0B), "Образ жизни")
        "stress" -> PassportFieldMeta("Уровень стресса", Icons.Rounded.Face, auraHex(0xFFA855F7), "Образ жизни")
        "sleep" -> PassportFieldMeta("Сон", Icons.Rounded.AutoAwesome, auraHex(0xFF818CF8), "Образ жизни")
        "food" -> PassportFieldMeta("Питание", Icons.Rounded.HealthAndSafety, auraHex(0xFF10B981), "Образ жизни")
        "water" -> PassportFieldMeta("Потребление воды", Icons.Rounded.WaterDrop, auraHex(0xFF0EA5E9), "Образ жизни")
        "smoking" -> PassportFieldMeta("Курение", Icons.Rounded.Warning, auraHex(0xFF64748B), "Образ жизни")
        "activity" -> PassportFieldMeta("Физическая активность", Icons.Rounded.TrendingUp, auraHex(0xFF22C55E), "Образ жизни")
        "environment" -> PassportFieldMeta("Окружающая среда", Icons.Rounded.WbSunny, auraHex(0xFF14B8A6), "Образ жизни")
        "routine_level" -> PassportFieldMeta("Уровень ухода", Icons.Rounded.Spa, auraHex(0xFF10B981), "Уход")
        "used_products" -> PassportFieldMeta("Используемые средства", Icons.Rounded.Spa, auraHex(0xFF34D399), "Уход")
        "negative_reactions" -> PassportFieldMeta("Негативные реакции", Icons.Rounded.Warning, auraHex(0xFFEF4444), "Уход")
        "actives_experience" -> PassportFieldMeta("Опыт с активами", Icons.Rounded.AutoAwesome, auraHex(0xFF8B5CF6), "Уход")
        "goals" -> PassportFieldMeta("Цель ухода", Icons.Rounded.Star, auraHex(0xFFA855F7), "Цели")
        "priority" -> PassportFieldMeta("Приоритет результата", Icons.Rounded.Verified, auraHex(0xFF059669), "Цели")
        "active_readiness" -> PassportFieldMeta("Готовность к активам", Icons.Rounded.AutoAwesome, auraHex(0xFF7C3AED), "Цели")
        "preferred_format" -> PassportFieldMeta("Формат рекомендаций", Icons.Rounded.Assignment, auraHex(0xFF64748B), "Цели")
        "sensitivity_level" -> PassportFieldMeta("Уровень чувствительности", Icons.Rounded.Warning, auraHex(0xFFF97316), "Чувствительность")
        else -> PassportFieldMeta("Дополнительный параметр", Icons.Rounded.Assignment, auraHex(0xFF94A3B8), "Цели")
    }
}
