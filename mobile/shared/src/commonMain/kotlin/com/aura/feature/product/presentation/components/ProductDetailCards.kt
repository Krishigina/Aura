package com.aura.feature.product.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ProductMatchingDetail
import com.aura.core.domain.model.ScoreBreakdown
import com.aura.core.ui.theme.PinkAccent
import com.aura.core.ui.theme.aura
import com.aura.feature.product.brandTrustPreview
import com.aura.feature.product.buildVisibleBrandHeroMeta
import com.aura.feature.product.decisionReasons
import com.aura.feature.product.decisionStateLabel
import com.aura.feature.product.decisionSupportingLine
import com.aura.feature.product.evidenceStatusNote
import com.aura.feature.product.keyCharacteristicsPreview
import com.aura.feature.product.normalizeCompatibilityPercent
import com.aura.feature.product.parseInciPreview
import com.aura.feature.product.primaryReasons
import com.aura.feature.product.scoreBreakdownRows
import com.aura.feature.product.scoreBreakdownTotal
import com.aura.feature.product.shouldShowBrandTrustSection

@Composable
fun HeroCard(product: Product, matching: ProductMatchingDetail?) {
    val tokens = MaterialTheme.aura.product
    UnifiedDetailBlock {
        Column(modifier = Modifier.padding(horizontal = tokens.d16, vertical = tokens.d18), verticalArrangement = Arrangement.spacedBy(tokens.d12)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name ?: "Без названия", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(product.category, product.product_type, product.segment).joinToString(" · "), color = MaterialTheme.aura.product.textMuted)
                    buildVisibleBrandHeroMeta(product)
                        .takeIf { it.isNotBlank() }
                        ?.let {
                            Text(it, color = MaterialTheme.aura.product.textMuted, style = MaterialTheme.typography.bodySmall)
                        }
                }
                CompatibilityBadge(matching?.compatibilityPercent ?: product.compatibilityPercent ?: 0, matching?.decision ?: product.decision ?: "recommend")
            }
            Text(product.description ?: product.desc ?: product.what_is_it ?: "Описание пока не заполнено", color = MaterialTheme.aura.product.textBody)
        }
    }
}

@Composable
fun PrimaryDecisionZone(matching: ProductMatchingDetail?, product: Product) {
    val tokens = MaterialTheme.aura.product
    val compatibilityPercent = matching?.compatibilityPercent ?: product.compatibilityPercent
    val percent = normalizeCompatibilityPercent(compatibilityPercent ?: 0)
    val decision = matching?.decision ?: product.decision ?: "recommend"
    val color = when (decision) {
        "exclude" -> MaterialTheme.aura.product.danger
        "caution" -> MaterialTheme.aura.product.warn
        else -> MaterialTheme.aura.product.mintSoft
    }
    val reasons = decisionReasons(matching?.explanations.orEmpty())
    val label = decisionStateLabel(
        decision = matching?.decision ?: product.decision,
        compatibilityPercent = compatibilityPercent,
        reasons = reasons,
    )
    val supportingLine = decisionSupportingLine(compatibilityPercent = compatibilityPercent)

    UnifiedDetailBlock {
        Column(modifier = Modifier.padding(horizontal = tokens.d16, vertical = tokens.d18), verticalArrangement = Arrangement.spacedBy(tokens.d14)) {
            SectionTitle("Решение", Icons.Rounded.Science, tint = tokens.indigo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.d16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompatibilityCircle(percent = percent, color = color)
                Column(verticalArrangement = Arrangement.spacedBy(tokens.d8), modifier = Modifier.weight(1f)) {
                    Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(supportingLine, color = MaterialTheme.aura.product.textMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (reasons.isEmpty()) {
                Text(
                    "Соберите больше данных профиля, чтобы получить персональные причины совместимости.",
                    color = MaterialTheme.aura.product.textBody,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                reasons.forEach { Text("• $it", color = MaterialTheme.aura.product.textBody, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun CompatibilityCircle(percent: Int, color: Color) {
    val tokens = MaterialTheme.aura.product
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(tokens.d114)) {
            val strokeWidth = tokens.d11.toPx()
            drawArc(
                color = color.copy(alpha = tokens.alpha20),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (normalizeCompatibilityPercent(percent) / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text("${normalizeCompatibilityPercent(percent)}%", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun CompatibilityBadge(percent: Int, decision: String) {
    val tokens = MaterialTheme.aura.product
    val color = when (decision) {
        "exclude" -> MaterialTheme.aura.product.danger
        "caution" -> MaterialTheme.aura.product.warn
        else -> MaterialTheme.aura.product.mintSoft
    }
    Column(horizontalAlignment = Alignment.End) {
        Surface(shape = RoundedCornerShape(tokens.d18), color = color.copy(alpha = tokens.alpha18)) {
            Text("${percent.coerceIn(0, 100)}%", color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = tokens.d12, vertical = tokens.d8))
        }
        Text(decisionLabel(decision), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WhyFitsCard(matching: ProductMatchingDetail?) {
    val tokens = MaterialTheme.aura.product
    UnifiedDetailBlock {
        Column(modifier = Modifier.padding(horizontal = tokens.d16, vertical = tokens.d18), verticalArrangement = Arrangement.spacedBy(tokens.d8)) {
            SectionTitle("Почему подходит", Icons.Rounded.Spa, tint = tokens.sky)
            val evidenceItems = matching?.evidenceExplanations.orEmpty()
            val evidenceTexts = primaryReasons(evidenceItems.map { it.text }, limit = 3)
            val items = evidenceTexts.ifEmpty { primaryReasons(matching?.explanations.orEmpty(), limit = 3) }
                .ifEmpty { listOf("Индекс будет точнее после заполнения паспорта кожи и анализа состава.") }
            items.forEach { Text("• $it", color = MaterialTheme.aura.product.textBody, style = MaterialTheme.typography.bodyMedium) }
            evidenceStatusNote(evidenceItems.map { it.evidenceStatus })?.let {
                Text(it, color = MaterialTheme.aura.product.textMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WarningsCard(matching: ProductMatchingDetail?) {
    val tokens = MaterialTheme.aura.product
    val warnings = matching?.warnings.orEmpty()
    val contraindications = matching?.contraindications.orEmpty()
    if (warnings.isEmpty() && contraindications.isEmpty()) {
        return
    }

    UnifiedDetailBlock {
        Column(modifier = Modifier.padding(horizontal = tokens.d16, vertical = tokens.d18), verticalArrangement = Arrangement.spacedBy(tokens.d8)) {
            SectionTitle("Предупреждения", Icons.Rounded.Science, tint = tokens.orange)
            warnings.forEach { Text("Предупреждение: $it", color = MaterialTheme.aura.product.warn, style = MaterialTheme.typography.bodySmall) }
            contraindications.forEach { Text("Исключено: $it", color = MaterialTheme.aura.product.danger, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun BreakdownCard(breakdown: ScoreBreakdown) {
    val rows = scoreBreakdownRows(breakdown)
    val total = scoreBreakdownTotal(breakdown)
    val topFactor = rows.maxByOrNull { it.value }?.label ?: "-"

    ExpandableCard(
        title = "Разбор индекса",
        icon = Icons.Rounded.Science,
        iconTint = MaterialTheme.aura.product.indigo,
        preview = "$total/100, ключевой вклад: $topFactor",
    ) {
        rows.forEach { row -> BreakdownRow(row.label, row.value, row.max) }
    }
}

@Composable
fun BreakdownRow(label: String, value: Int, max: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.aura.product.textBody)
        Text("$value/$max", color = PinkAccent, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProductFieldsCard(product: Product) {
    ExpandableCard(
        title = "Характеристики",
        icon = Icons.Rounded.Science,
        iconTint = MaterialTheme.aura.product.teal,
        preview = keyCharacteristicsPreview(product),
    ) {
        FieldLine("Бренд", product.brand)
        FieldLine("Тип", product.product_type)
        FieldLine("Категория", product.category)
        FieldLine("Сегмент", product.segment)
        FieldLine("Для кого", product.for_whom)
        FieldLine("Назначение", product.purpose?.joinToString(", "))
        FieldLine("Тип кожи", product.skin_type?.joinToString(", "))
        FieldLine("Актив", product.active_ingredient)
        FieldLine("Объем", product.volume)
        FieldLine("Применение", product.application_info)
    }
}

@Composable
fun CompositionCard(product: Product, matching: ProductMatchingDetail?) {
    val tokens = MaterialTheme.aura.product
    var expanded by remember { mutableStateOf(false) }
    val previewIngredients = parseInciPreview(product.composition, limit = 5)
    val allIngredients = parseInciPreview(product.composition, limit = Int.MAX_VALUE)
    val canExpand = allIngredients.isNotEmpty()
    val preview = if (previewIngredients.isEmpty()) "Состав не указан" else previewIngredients.joinToString(", ")

    UnifiedDetailBlock {
        Column(modifier = Modifier.padding(horizontal = tokens.d16, vertical = tokens.d14), verticalArrangement = Arrangement.spacedBy(tokens.d10)) {
            Row(
                modifier = Modifier.fillMaxWidth().let { rowModifier ->
                    if (canExpand) rowModifier.clickable { expanded = !expanded } else rowModifier
                },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.d10), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(tokens.d34)
                            .clip(RoundedCornerShape(tokens.d10))
                            .background(MaterialTheme.aura.product.accordionIconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Science, contentDescription = null, tint = tokens.teal, modifier = Modifier.size(tokens.d18))
                    }
                    Column {
                        Text("Состав INCI", fontWeight = FontWeight.Bold, color = MaterialTheme.aura.product.textPrimary)
                        Text(
                            preview,
                            color = MaterialTheme.aura.product.textMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(tokens.d999), color = MaterialTheme.aura.product.accordionIconBg, modifier = Modifier.size(tokens.d28)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (expanded) "−" else "+", color = MaterialTheme.aura.product.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (expanded && canExpand) {
                Text(allIngredients.joinToString(", "), color = MaterialTheme.aura.product.textBody, style = MaterialTheme.typography.bodySmall)
                if (!matching?.matchedConcerns.isNullOrEmpty()) {
                    Text("Совпавшие проблемы: ${matching?.matchedConcerns?.joinToString(", ")}", color = PinkAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun BrandDetailsCard(product: Product) {
    if (!shouldShowBrandTrustSection(product)) return

    val heroDescription = (product.description ?: product.desc ?: product.what_is_it).orEmpty().trim()
    val brandDescription = product.description.orEmpty().trim().takeIf { it != heroDescription }.orEmpty()
    var showFullDescription by remember { mutableStateOf(false) }
    val hasLongDescription = brandDescription.length > 220
    val descriptionText = if (showFullDescription || !hasLongDescription) brandDescription else brandDescription.take(220) + "..."

    ExpandableCard(
        title = "О бренде",
        icon = Icons.Rounded.Spa,
        iconTint = MaterialTheme.aura.product.lavenderSoft,
        preview = brandTrustPreview(product),
    ) {
        if (descriptionText.isNotBlank()) {
            Text(descriptionText, color = MaterialTheme.aura.product.textBody, style = MaterialTheme.typography.bodySmall)
            if (hasLongDescription) {
                Text(
                    if (showFullDescription) "Свернуть" else "Показать полностью",
                    color = PinkAccent,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { showFullDescription = !showFullDescription },
                )
            }
        }
        FieldLine("Страна происхождения", product.country_origin)
        FieldLine("Страна изготовления", product.country)
        FieldLine("Изготовитель", product.manufacturer)
    }
}
