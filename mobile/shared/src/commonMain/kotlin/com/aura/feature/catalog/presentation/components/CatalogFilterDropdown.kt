package com.aura.feature.catalog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.core.ui.theme.MintGreen
import com.aura.core.ui.theme.aura

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogFilterDropdown(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    isLoading: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dark: Boolean,
) {
    val tokens = MaterialTheme.aura.catalog
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (selected.isEmpty()) "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d\u043e" else "\u0412\u044b\u0431\u0440\u0430\u043d\u043e: ${selected.size}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.dropdownRadius))
            .background(Color.White.copy(alpha = if (dark) tokens.dropdownDarkAlpha else tokens.dropdownLightAlpha))
            .border(
                tokens.searchBorderWidth,
                Color.White.copy(alpha = if (dark) tokens.dropdownBorderDarkAlpha else tokens.dropdownBorderLightAlpha),
                RoundedCornerShape(tokens.dropdownRadius),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = tokens.dropdownHorizontalPadding, vertical = tokens.dropdownVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "\u0421\u0432\u0435\u0440\u043d\u0443\u0442\u044c" else "\u041e\u0442\u043a\u0440\u044b\u0442\u044c",
                tint = MintGreen,
                modifier = Modifier.size(tokens.dropdownIconSize),
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = tokens.dropdownOptionHorizontalPadding,
                        end = tokens.dropdownOptionHorizontalPadding,
                        bottom = tokens.dropdownOptionsBottomPadding + 6.dp,
                    ),
            ) {
                if (isLoading) {
                    Text(
                        text = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...",
                        modifier = Modifier.padding(vertical = tokens.dropdownOptionVerticalPadding),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                    )
                } else if (options.isEmpty()) {
                    Text(
                        text = "\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0445 \u0432\u0430\u0440\u0438\u0430\u043d\u0442\u043e\u0432",
                        modifier = Modifier.padding(vertical = tokens.dropdownOptionVerticalPadding),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        options.forEach { option ->
                            CatalogFilterChip(
                                label = option,
                                selected = selected.contains(option),
                                onClick = { onToggle(option) },
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                dark = dark,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    dark: Boolean,
) {
    val background = when {
        selected -> MintGreen.copy(alpha = if (dark) 0.24f else 0.26f)
        dark -> Color.White.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.78f)
    }
    val borderColor = when {
        selected -> MintGreen.copy(alpha = 0.9f)
        dark -> Color.White.copy(alpha = 0.12f)
        else -> Color(0xFFD9E4F2)
    }
    val contentColor = if (selected) textPrimary else textSecondary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}
