package com.aura.feature.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.domain.model.ProductDetailResponse
import com.aura.core.domain.model.ScoreBreakdown
import com.aura.core.ui.components.AuraGlassBarAlpha
import com.aura.core.ui.components.AuraGlassBarBorderAlpha
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.components.shimmerOverlay
import com.aura.core.ui.theme.aura
import com.aura.feature.chat.presentation.ChatNavigationState
import com.aura.feature.product.presentation.components.BrandDetailsCard
import com.aura.feature.product.presentation.components.BreakdownCard
import com.aura.feature.product.presentation.components.CompositionCard
import com.aura.feature.product.presentation.components.ErrorState
import com.aura.feature.product.presentation.components.FullscreenMediaOverlay
import com.aura.feature.product.presentation.components.HeroCard
import com.aura.feature.product.presentation.components.MediaZone
import com.aura.feature.product.presentation.components.PrimaryDecisionZone
import com.aura.feature.product.presentation.components.ProductDetailTopBar
import com.aura.feature.product.presentation.components.ProductFieldsCard
import com.aura.feature.product.presentation.components.WarningsCard
import com.aura.feature.product.presentation.components.WhyFitsCard
import com.aura.feature.product.presentation.ProductDetailUiState
import com.aura.feature.product.presentation.ProductDetailViewModel
import org.koin.compose.koinInject

@Composable
fun ProductDetailRoute(
    productId: String,
    onBack: () -> Unit,
    onAskAssistant: () -> Unit = {},
    onAddToRoutine: () -> Unit = {},
    viewModel: ProductDetailViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.load(productId)
    }

    ProductDetailScreen(
        uiState = uiState,
        onRetry = { viewModel.load(productId) },
        onBack = onBack,
        onAskAssistant = onAskAssistant,
        onAddToRoutine = { viewModel.addToRoutine(onAddToRoutine) },
        onAssistantStarted = viewModel::beginAssistantAction,
        onAssistantFinished = viewModel::endAssistantAction,
    )
}

@Composable
fun ProductDetailScreen(
    uiState: ProductDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onAskAssistant: () -> Unit = {},
    onAddToRoutine: () -> Unit = {},
    onAssistantStarted: () -> Unit = {},
    onAssistantFinished: () -> Unit = {},
    chatNavigationState: ChatNavigationState = koinInject(),
) {
    val tokens = MaterialTheme.aura.product

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SoftPastelBackground(variant = SoftPastelVariant.Catalog)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tokens.gradientTop.copy(alpha = tokens.alpha72),
                            tokens.gradientMiddle.copy(alpha = tokens.alpha78),
                            tokens.gradientBottom.copy(alpha = tokens.alpha74),
                        ),
                    ),
                ),
        )
        when {
            uiState.isLoading -> ProductDetailLoadingSkeleton()
            uiState.error != null -> ErrorState(
                error = uiState.error.orEmpty(),
                onBack = onBack,
                onRetry = onRetry,
            )
            uiState.detail != null -> ProductDetailContent(
                detail = uiState.detail,
                isAssistantLoading = uiState.isAssistantLoading,
                isRoutineLoading = uiState.isRoutineLoading,
                isInFavorites = uiState.isInFavorites,
                routineFeedback = uiState.routineFeedback,
                onAskAssistant = onAskAssistant,
                onAddToRoutine = onAddToRoutine,
                onAssistantStarted = onAssistantStarted,
                onAssistantFinished = onAssistantFinished,
                chatNavigationState = chatNavigationState,
            )
        }

        ProductDetailTopBar(onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun ProductDetailContent(
    detail: ProductDetailResponse,
    isAssistantLoading: Boolean,
    isRoutineLoading: Boolean,
    isInFavorites: Boolean,
    routineFeedback: String?,
    onAskAssistant: () -> Unit,
    onAddToRoutine: () -> Unit,
    onAssistantStarted: () -> Unit,
    onAssistantFinished: () -> Unit,
    chatNavigationState: ChatNavigationState,
) {
    val tokens = MaterialTheme.aura.product
    val product = detail.product
    val matching = detail.matching
    val photos = product.photos.orEmpty()
    val galleryState = remember(product.id, photos.size) { ProductDetailGalleryState(totalItems = photos.size) }
    val isAssistantEnabled = detail.assistantContext != null && !isAssistantLoading

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.d20)
                .padding(top = auraToolbarContentTopPadding(), bottom = tokens.d188),
            verticalArrangement = Arrangement.spacedBy(tokens.d16),
        ) {
            MediaZone(product = product, onImageTap = galleryState::openAt)

            HeroCard(product = product, matching = matching)
            PrimaryDecisionZone(matching = matching, product = product)
            WarningsCard(matching = matching)
            WhyFitsCard(matching = matching)

            ProductFieldsCard(product)
            CompositionCard(product, matching)
            BreakdownCard(matching?.scoreBreakdown ?: ScoreBreakdown())
            BrandDetailsCard(product)
        }

        if (galleryState.isFullscreen) {
            FullscreenMediaOverlay(
                photos = photos,
                currentIndex = galleryState.currentIndex,
                onClose = galleryState::close,
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = tokens.d12, vertical = tokens.d12)
                .padding(bottom = tokens.d92),
            color = Color.Transparent,
            tonalElevation = tokens.d0,
            shape = RoundedCornerShape(tokens.d20),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(tokens.d12),
                horizontalArrangement = Arrangement.spacedBy(tokens.d10),
            ) {
                Button(
                    onClick = {
                        onAssistantStarted()
                        chatNavigationState.startProductChat(detail.assistantContext)
                        try {
                            onAskAssistant()
                        } finally {
                            onAssistantFinished()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(tokens.d52),
                    shape = RoundedCornerShape(tokens.d12),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = AuraGlassBarAlpha),
                        contentColor = MaterialTheme.aura.product.textPrimary,
                        disabledContainerColor = Color.White.copy(alpha = AuraGlassBarAlpha),
                        disabledContentColor = MaterialTheme.aura.product.textMuted,
                    ),
                    enabled = isAssistantEnabled,
                ) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.aura.product.textPrimary)
                    Spacer(modifier = Modifier.width(tokens.d8))
                    Text(assistantCtaLabel(isAssistantLoading), color = MaterialTheme.aura.product.textPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onAddToRoutine()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(tokens.d52)
                        .border(tokens.d1, Color.White.copy(alpha = AuraGlassBarBorderAlpha), RoundedCornerShape(tokens.d12)),
                    shape = RoundedCornerShape(tokens.d12),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = AuraGlassBarAlpha),
                        disabledContainerColor = Color.White.copy(alpha = AuraGlassBarAlpha),
                        contentColor = MaterialTheme.aura.product.textPrimary,
                    ),
                    enabled = !isRoutineLoading,
                ) {
                    Icon(
                        if (isInFavorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isInFavorites) tokens.pinkSoft else MaterialTheme.aura.product.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(tokens.d8))
                    Text(addToRoutineCtaLabel(isInFavorites), color = MaterialTheme.aura.product.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
            routineFeedback?.let {
                Text(
                    text = it,
                    color = MaterialTheme.aura.product.textBody,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = tokens.d12, vertical = tokens.d2),
                )
            }
        }
    }
}

@Composable
private fun ProductDetailLoadingSkeleton() {
    val tokens = MaterialTheme.aura.product
    val heroShape = RoundedCornerShape(tokens.d24)
    val ctaShape = RoundedCornerShape(tokens.d12)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.d20)
                .padding(top = auraToolbarContentTopPadding(), bottom = tokens.d188),
            verticalArrangement = Arrangement.spacedBy(tokens.d16),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.d286)
                    .shimmerOverlay(
                        baseColor = MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha22),
                        shape = heroShape,
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.d156)
                    .shimmerOverlay(
                        baseColor = MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha20),
                        shape = heroShape,
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.d118)
                    .shimmerOverlay(
                        baseColor = MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha20),
                        shape = heroShape,
                    ),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = tokens.d12, vertical = tokens.d12)
                .padding(bottom = tokens.d92),
            color = Color.Transparent,
            tonalElevation = tokens.d0,
            shape = RoundedCornerShape(tokens.d20),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(tokens.d12),
                horizontalArrangement = Arrangement.spacedBy(tokens.d10),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(tokens.d52)
                        .shimmerOverlay(
                            baseColor = MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha20),
                            shape = ctaShape,
                        )
                        .border(tokens.d1, MaterialTheme.aura.product.lavenderSoft.copy(alpha = tokens.alpha90), ctaShape),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(tokens.d52)
                        .shimmerOverlay(
                            baseColor = MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha20),
                            shape = ctaShape,
                        )
                        .border(tokens.d1, MaterialTheme.aura.product.skySoft.copy(alpha = tokens.alpha90), ctaShape),
                )
            }
        }
    }
}
