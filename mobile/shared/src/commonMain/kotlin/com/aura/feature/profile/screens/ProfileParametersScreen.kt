package com.aura.feature.profile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.profile.presentation.components.parameters.PassportFieldMeta
import com.aura.feature.profile.presentation.components.parameters.PassportTimelineRow
import com.aura.feature.profile.presentation.components.parameters.passportFieldMeta
import com.aura.feature.profile.presentation.components.parameters.passportSections
import com.aura.feature.profile.presentation.parameters.ProfileParametersViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileAllParametersScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileParametersViewModel = koinInject(),
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val bg = if (dark) auraHex(0xFF0A0A0A) else MaterialTheme.aura.profile.iceBlue
    val glassAlpha = if (dark) 0.08f else 0.45f
    val cardBorder = if (dark) Color.White.copy(alpha = auraTokenAlpha(0.1f)) else Color.White.copy(alpha = auraTokenAlpha(0.6f))
    val textPrimary = if (dark) auraHex(0xFFF1F5F9) else MaterialTheme.aura.profile.slate800
    val textSecondary = if (dark) auraHex(0xFF94A3B8) else MaterialTheme.aura.profile.slate500

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val uiState by viewModel.uiState.collectAsState()
    val answerEntries = uiState.skinPassportAnswers.entries
        .filter { it.value.isNotEmpty() }
        .filterNot { (key, _) -> key == "budget" }
        .sortedBy { it.key }
    val groupedEntries = answerEntries.groupBy { passportFieldMeta(it.key).section }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Default)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = auraTokenDp(24f))
                .padding(top = auraToolbarContentTopPadding(), bottom = auraTokenDp(24f)),
            verticalArrangement = Arrangement.spacedBy(auraTokenDp(18f)),
        ) {
            if (answerEntries.isEmpty()) {
                PassportTimelineRow(
                    meta = PassportFieldMeta("Паспорт кожи", Icons.Rounded.Assignment, auraHex(0xFF94A3B8), ""),
                    value = "Анкета пока не заполнена",
                    glassAlpha = glassAlpha,
                    cardBorder = cardBorder,
                    dark = dark,
                    showLine = false,
                )
            } else {
                passportSections.forEach { section ->
                    val entries = groupedEntries[section].orEmpty()
                    if (entries.isNotEmpty()) {
                        Text(
                            text = section.uppercase(),
                            fontSize = auraTokenSp(11f),
                            fontWeight = FontWeight.Black,
                            color = textSecondary,
                            letterSpacing = auraTokenSp(1.2f),
                            modifier = Modifier.padding(start = auraTokenDp(11f)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(0f))) {
                            entries.forEachIndexed { index, (key, values) ->
                                PassportTimelineRow(
                                    meta = passportFieldMeta(key),
                                    value = values.joinToString(" • "),
                                    glassAlpha = glassAlpha,
                                    cardBorder = cardBorder,
                                    dark = dark,
                                    showLine = index != entries.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
        }

        AuraTopBar(
            title = "Все параметры",
            onBack = onBack,
            titleColor = textPrimary,
            iconTint = textSecondary,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
