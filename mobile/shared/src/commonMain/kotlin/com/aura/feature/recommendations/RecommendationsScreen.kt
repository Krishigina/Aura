package com.aura.feature.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.aura.core.data.api.model.RecommendationLine
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.recommendations.presentation.components.ContextBlock
import com.aura.feature.recommendations.presentation.components.ErrorPanel
import com.aura.feature.recommendations.presentation.components.InputQualityChips
import com.aura.feature.recommendations.presentation.components.LineDossier
import com.aura.feature.recommendations.presentation.components.LineSelector
import com.aura.feature.recommendations.presentation.components.LoadingPanel
import com.aura.feature.recommendations.presentation.components.SummaryCard
import com.aura.feature.recommendations.presentation.RecommendationsUiState
import com.aura.feature.recommendations.presentation.RecommendationsViewModel
import org.koin.compose.koinInject

@Composable
fun RecommendationsRoute(
    onBack: () -> Unit,
    onProductOpen: (Int) -> Unit = {},
    onOpenSurvey: () -> Unit,
    viewModel: RecommendationsViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadGenerated()
    }

    RecommendationsScreen(
        uiState = uiState,
        onLineSelected = viewModel::onLineSelected,
        onSaveFavorite = viewModel::saveCurrentFavorite,
        onBack = onBack,
        onProductOpen = onProductOpen,
        onOpenSurvey = onOpenSurvey,
    )
}

@Composable
fun RecommendationsScreen(
    uiState: RecommendationsUiState,
    onLineSelected: (RecommendationLine) -> Unit,
    onSaveFavorite: () -> Unit,
    onBack: () -> Unit,
    onProductOpen: (Int) -> Unit = {},
    onOpenSurvey: () -> Unit,
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val tokens = MaterialTheme.aura.recommendations
    val bg = if (dark) tokens.backgroundDark else tokens.backgroundLight
    val textPrimary = if (dark) tokens.textPrimaryDark else tokens.textPrimaryLight
    val textSecondary = if (dark) tokens.textSecondaryDark else tokens.textSecondaryLight
    val textBody = if (dark) tokens.textBodyDark else tokens.textBodyLight

    val response = uiState.recommendation
    val selectedLine = response?.lines?.firstOrNull { it.key == uiState.selectedLineKey } ?: response?.lines?.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Home)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.listHorizontalPadding)
                .padding(top = auraToolbarContentTopPadding()),
            contentPadding = PaddingValues(bottom = tokens.listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.listItemGap),
        ) {
            when {
                uiState.isLoading -> item { LoadingPanel(textBody = textBody, textSecondary = textSecondary, dark = dark) }
                uiState.error != null -> item {
                    ErrorPanel(
                        message = uiState.error.orEmpty(),
                        needsPassport = uiState.needsPassport,
                        onOpenSurvey = onOpenSurvey,
                        textBody = textBody,
                        textSecondary = textSecondary,
                        dark = dark,
                    )
                }
                response != null -> {
                    item { SummaryCard(response = response, textBody = textBody, textSecondary = textSecondary, dark = dark) }
                    item { InputQualityChips(inputQuality = response.inputQuality, textBody = textBody, textSecondary = textSecondary, dark = dark) }

                    if (response.lines.isNotEmpty()) {
                        item {
                            LineSelector(
                                lines = response.lines,
                                selectedKey = selectedLine?.key,
                                onSelect = onLineSelected,
                                textBody = textBody,
                                textSecondary = textSecondary,
                                dark = dark,
                            )
                        }
                    }

                    if (selectedLine != null) {
                        item { LineDossier(line = selectedLine, textBody = textBody, textSecondary = textSecondary, dark = dark, onProductOpen = onProductOpen) }
                    }

                    if (response.warnings.isNotEmpty() || response.procedureContext.isNotEmpty() || selectedLine?.warnings?.isNotEmpty() == true) {
                        item {
                            ContextBlock(
                                warnings = response.warnings + selectedLine?.warnings.orEmpty(),
                                procedureContext = response.procedureContext,
                                textBody = textBody,
                                textSecondary = textSecondary,
                                dark = dark,
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                onSaveFavorite()
                            },
                            enabled = !uiState.isSaving && !uiState.saved,
                            modifier = Modifier.fillMaxWidth().height(tokens.saveButtonHeight),
                            shape = RoundedCornerShape(tokens.saveButtonRadius),
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.violet, contentColor = Color.White),
                        ) {
                            Icon(if (uiState.saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.Favorite, contentDescription = null)
                            Spacer(modifier = Modifier.width(tokens.inlineIconGap))
                            Text(if (uiState.saved) "Сохранено" else "Сохранить в избранное", fontWeight = FontWeight.Bold)
                        }

                        uiState.saveError?.let { message ->
                            Spacer(modifier = Modifier.height(tokens.saveErrorTopGap))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(tokens.saveErrorRowGap),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = tokens.errorColor,
                                    modifier = Modifier.size(tokens.saveErrorIconSize),
                                )
                                Text(
                                    text = message,
                                    color = tokens.errorColor,
                                    fontSize = tokens.smallTextFontSize,
                                    lineHeight = tokens.smallTextLineHeight,
                                )
                            }
                        }
                    }
                }
            }
        }

        AuraTopBar(
            title = "Линейка ухода",
            onBack = onBack,
            titleColor = textPrimary,
            iconTint = textSecondary,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
fun SavedRecommendationScreen(
    favoriteId: String,
    onBack: () -> Unit,
    onProductOpen: (Int) -> Unit = {},
    viewModel: RecommendationsViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(favoriteId) {
        viewModel.loadSaved(favoriteId)
    }

    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val tokens = MaterialTheme.aura.recommendations
    val bg = if (dark) tokens.backgroundDark else tokens.backgroundLight
    val textPrimary = if (dark) tokens.textPrimaryDark else tokens.textPrimaryLight
    val textSecondary = if (dark) tokens.textSecondaryDark else tokens.textSecondaryLight
    val textBody = if (dark) tokens.textBodyDark else tokens.textBodyLight

    val response = uiState.recommendation
    val selectedLine = response?.lines?.firstOrNull { it.key == uiState.selectedLineKey } ?: response?.lines?.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Home)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = tokens.listHorizontalPadding).padding(top = auraToolbarContentTopPadding()),
            contentPadding = PaddingValues(bottom = tokens.listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.listItemGap),
        ) {
            when {
                uiState.isLoading -> item { LoadingPanel(textBody = textBody, textSecondary = textSecondary, dark = dark) }
                uiState.error != null -> item {
                    ErrorPanel(
                        message = uiState.error.orEmpty(),
                        needsPassport = false,
                        onOpenSurvey = {},
                        textBody = textBody,
                        textSecondary = textSecondary,
                        dark = dark,
                    )
                }
                response != null -> {
                    item { SummaryCard(response = response, textBody = textBody, textSecondary = textSecondary, dark = dark) }
                    if (response.lines.isNotEmpty()) {
                        item {
                            LineSelector(
                                lines = response.lines,
                                selectedKey = selectedLine?.key,
                                onSelect = viewModel::onLineSelected,
                                textBody = textBody,
                                textSecondary = textSecondary,
                                dark = dark,
                            )
                        }
                    }
                    if (selectedLine != null) {
                        item { LineDossier(line = selectedLine, textBody = textBody, textSecondary = textSecondary, dark = dark, onProductOpen = onProductOpen) }
                    }
                    if (response.warnings.isNotEmpty() || response.procedureContext.isNotEmpty() || selectedLine?.warnings?.isNotEmpty() == true) {
                        item {
                            ContextBlock(
                                warnings = response.warnings + selectedLine?.warnings.orEmpty(),
                                procedureContext = response.procedureContext,
                                textBody = textBody,
                                textSecondary = textSecondary,
                                dark = dark,
                            )
                        }
                    }
                }
            }
        }

        AuraTopBar(
            title = "Сохранённая линейка",
            onBack = onBack,
            titleColor = textPrimary,
            iconTint = textSecondary,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
