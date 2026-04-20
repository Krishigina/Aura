package com.aura.feature.product.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.theme.PinkAccent
import com.aura.core.ui.theme.aura

@Composable
fun ExpandableCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.aura.product.pinkSoft,
    preview: String,
    content: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.aura.product
    var expanded by remember { mutableStateOf(false) }
    UnifiedDetailBlock {
        Column(modifier = Modifier.padding(horizontal = tokens.d16, vertical = tokens.d18), verticalArrangement = Arrangement.spacedBy(tokens.d10)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
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
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(tokens.d18))
                    }
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.aura.product.textPrimary)
                        Text(preview, color = MaterialTheme.aura.product.textMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(shape = RoundedCornerShape(tokens.d999), color = MaterialTheme.aura.product.accordionIconBg, modifier = Modifier.size(tokens.d28)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (expanded) "−" else "+", color = MaterialTheme.aura.product.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (expanded) content()
        }
    }
}

@Composable
fun UnifiedDetailBlock(content: @Composable () -> Unit) {
    val tokens = MaterialTheme.aura.product
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.d24))
            .border(tokens.d1, MaterialTheme.aura.product.accordionBorder, RoundedCornerShape(tokens.d24)),
        color = MaterialTheme.aura.product.accordionBg.copy(alpha = tokens.alpha80),
    ) {
        content()
    }
}

@Composable
fun ProductDetailTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    AuraTopBar(
        title = "Детали продукта",
        onBack = onBack,
        titleColor = MaterialTheme.aura.product.textMuted,
        iconTint = MaterialTheme.aura.product.textMuted,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(
    text: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.aura.product.pinkSoft,
) {
    val tokens = MaterialTheme.aura.product
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.d8)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(tokens.d18))
        Text(text, fontWeight = FontWeight.Bold, color = MaterialTheme.aura.product.textPrimary)
    }
}

@Composable
fun FieldLine(label: String, value: String?) {
    val tokens = MaterialTheme.aura.product
    if (!value.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = tokens.d6),
            verticalArrangement = Arrangement.spacedBy(tokens.d2),
        ) {
            Text(label, color = MaterialTheme.aura.product.textMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = MaterialTheme.aura.product.textPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ErrorState(error: String, onBack: () -> Unit, onRetry: () -> Unit) {
    val tokens = MaterialTheme.aura.product
    Column(modifier = Modifier.fillMaxSize().padding(tokens.d24), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(error, color = MaterialTheme.aura.product.danger)
        Spacer(modifier = Modifier.height(tokens.d12))
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.d10)) {
            Text(
                "Повторить",
                color = PinkAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.d12))
                    .clickable(onClick = onRetry)
                    .border(tokens.d1, PinkAccent, RoundedCornerShape(tokens.d12))
                    .padding(horizontal = tokens.d16, vertical = tokens.d10),
            )
            Text(
                "Назад",
                color = PinkAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.d12))
                    .clickable(onClick = onBack)
                    .border(tokens.d1, PinkAccent, RoundedCornerShape(tokens.d12))
                    .padding(horizontal = tokens.d16, vertical = tokens.d10),
            )
        }
    }
}

fun decisionLabel(decision: String): String = when (decision) {
    "exclude" -> "Исключён"
    "caution" -> "С осторожностью"
    else -> "Подходит"
}
