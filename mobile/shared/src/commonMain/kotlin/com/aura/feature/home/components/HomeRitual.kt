package com.aura.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.aura.core.data.api.model.HomeRitualItem
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.theme.aura

fun shouldShowRitualGlow(hasRitualItems: Boolean): Boolean = hasRitualItems

@Composable
fun RitualSection(
    textSecondary: Color,
    textBody: Color,
    dark: Boolean,
    ritualItems: List<HomeRitualItem>,
    checkedStates: Map<String, Boolean>,
    isLoading: Boolean,
    onSyncCheckedStates: (List<HomeRitualItem>) -> Unit,
    onCheckedChange: (String, Boolean) -> Unit,
) {
    val tokens = MaterialTheme.aura.home

    LaunchedEffect(ritualItems) {
        onSyncCheckedStates(ritualItems)
    }
    val remainingSteps = ritualItems.count { item -> !(checkedStates[item.id] ?: item.checked) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.ritualHeaderHorizontalPadding, vertical = tokens.ritualHeaderVerticalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = StringsRu.Home.ritual, fontSize = tokens.sectionTitleFontSize, fontWeight = FontWeight.Bold, color = textBody)
            Box(modifier = Modifier.background(tokens.softPink, RoundedCornerShape(tokens.pillRadius)).padding(horizontal = tokens.pillHorizontalPadding, vertical = tokens.smallGap)) {
                Text(text = ritualStepsLeftLabel(remainingSteps), fontSize = tokens.smallFontSize, fontWeight = FontWeight.SemiBold, color = tokens.slate700)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(tokens.ritualCardRadius), dark = dark)) {
            Column(modifier = Modifier.padding(tokens.ritualInnerPadding)) {
                when {
                    isLoading -> {
                        Text(
                            text = "Загрузка ритуала...",
                            fontSize = tokens.bodyFontSize,
                            color = textSecondary,
                        )
                    }

                    ritualItems.isEmpty() -> {
                        Text(
                            text = "Ритуал пока не сформирован",
                            fontSize = tokens.bodyFontSize,
                            color = textSecondary,
                        )
                    }

                    else -> {
                        ritualItems.forEachIndexed { index, item ->
                            RitualItem(
                                checked = checkedStates[item.id] ?: item.checked,
                                title = item.title,
                                subtitle = item.subtitle,
                                isActive = item.is_active,
                                isWarning = item.is_warning,
                                textBody = textBody,
                                textSecondary = textSecondary,
                                dark = dark,
                                onCheckedChange = { onCheckedChange(item.id, it) },
                            )
                            if (index < ritualItems.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(start = tokens.dividerStartPadding, top = tokens.dividerVerticalPadding, bottom = tokens.dividerVerticalPadding),
                                    color = Color.White.copy(alpha = if (dark) tokens.dividerDarkAlpha else tokens.dividerLightAlpha),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun ritualStepsLeftLabel(count: Int): String {
    val normalized = count.coerceAtLeast(0)
    val mod10 = normalized % 10
    val mod100 = normalized % 100
    val noun = when {
        mod10 == 1 && mod100 != 11 -> "шаг"
        mod10 in 2..4 && mod100 !in 12..14 -> "шага"
        else -> "шагов"
    }
    return "Осталось $normalized $noun"
}

@Composable
fun RitualItem(
    checked: Boolean,
    title: String,
    subtitle: String,
    isActive: Boolean = false,
    isWarning: Boolean = false,
    textBody: Color,
    textSecondary: Color,
    dark: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.aura.home
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    ) {
        CustomCheckbox(checked = checked, dark = dark)
        Spacer(modifier = Modifier.width(tokens.ritualItemGap))
        Column(modifier = Modifier.weight(1f).alpha(if (checked) tokens.checkedAlpha else 1f)) {
            Text(
                text = title,
                fontSize = if (isActive) tokens.activeRitualFontSize else tokens.inactiveRitualFontSize,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = textBody,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(tokens.activeIconSize), tint = tokens.pink)
                    Spacer(modifier = Modifier.width(tokens.activeIconGap))
                }
                Text(
                    text = subtitle,
                    fontSize = tokens.smallFontSize,
                    fontWeight = if (isActive || isWarning) FontWeight.Medium else FontWeight.Normal,
                    color = when {
                        isWarning -> tokens.errorColor
                        isActive -> tokens.slate700
                        else -> textSecondary
                    },
                )
            }
        }
        if (isActive) {
            Box(modifier = Modifier.size(tokens.activeBadgeSize).background(Color.White.copy(alpha = if (dark) tokens.activeBadgeDarkAlpha else tokens.activeBadgeLightAlpha), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = tokens.pink, modifier = Modifier.size(tokens.activeBadgeIconSize))
            }
        }
    }
}

@Composable
fun CustomCheckbox(checked: Boolean, dark: Boolean) {
    val tokens = MaterialTheme.aura.home
    Box(
        modifier = Modifier.size(tokens.checkboxSize).clip(CircleShape).background(if (checked) tokens.pink else Color.White.copy(alpha = if (dark) tokens.checkboxDarkAlpha else tokens.checkboxLightAlpha)).border(tokens.checkboxBorderWidth, if (checked) tokens.pink else tokens.pink.copy(alpha = tokens.checkboxBorderAlpha), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(imageVector = Icons.Rounded.Check, contentDescription = StringsRu.Home.checked, tint = Color.White, modifier = Modifier.size(tokens.checkboxIconSize))
    }
}
