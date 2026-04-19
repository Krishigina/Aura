package com.aura.feature.diagnostics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.data.api.model.DiagnosticsDevice
import com.aura.core.data.api.model.DiagnosticsMetrics
import com.aura.core.ui.components.GlassCard
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.theme.Lavender
import com.aura.core.ui.theme.MintGreen
import com.aura.core.ui.theme.PinkAccent
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.runtime.AppState
import com.aura.feature.diagnostics.presentation.DiagnosticsViewModel
import org.koin.compose.koinInject

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit = {},
    viewModel: DiagnosticsViewModel = koinInject(),
) {
    val tokens = MaterialTheme.aura.diagnostics
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SoftPastelBackground(dark = dark, variant = SoftPastelVariant.Diagnostics)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(tokens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "Анализ кожи",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(tokens.headerBottomGap))

            Spacer(modifier = Modifier.height(tokens.heroTopGap))

            ScanningSection()

            Spacer(modifier = Modifier.height(tokens.sectionGapLarge))

            MetricsSection(
                metrics = uiState.metrics,
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(tokens.sectionGapMedium))
            IoTSection(
                device = uiState.device,
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(tokens.sectionGapMedium))
        }
    }
}

@Composable
private fun ScanningSection() {
    val tokens = MaterialTheme.aura.diagnostics

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MintGreen.copy(alpha = tokens.scanCardAlpha),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ScanningAnimation()

            Spacer(modifier = Modifier.height(tokens.scanTitleGap))

            Text(
                text = "Сканирование...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MintGreen,
            )

            Spacer(modifier = Modifier.height(tokens.scanSubtitleGap))

            Text(
                text = "Поднесите датчик к коже",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = tokens.secondaryTextAlpha),
            )
        }
    }
}

@Composable
private fun ScanningAnimation() {
    val tokens = MaterialTheme.aura.diagnostics
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = tokens.scanPulseInitialAlpha,
        targetValue = tokens.scanPulseTargetAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(tokens.scanPulseDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .size(tokens.scanCanvasSize)
            .padding(tokens.scanCanvasPadding),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(tokens.scanCanvasSize)) {
            val center = Offset(size.width / 2, size.height / 2)
            val strokeWidth = tokens.scanStrokeWidth.toPx()
            val baseRadius = size.minDimension / 2 - tokens.scanBaseRadiusInset.toPx()

            for (ring in 0..3) {
                val radius = baseRadius - ring * tokens.scanRingGap.toPx()
                val alpha = (pulseAlpha - ring * tokens.scanRingAlphaStep).coerceAtLeast(tokens.scanRingMinAlpha)
                drawCircle(
                    color = MintGreen.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth - ring),
                )
            }
        }

        Surface(
            modifier = Modifier.size(tokens.scanIconSurfaceSize),
            shape = CircleShape,
            color = MintGreen.copy(alpha = tokens.scanIconSurfaceAlpha),
            shadowElevation = tokens.scanIconSurfaceElevation,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = MintGreen,
                    modifier = Modifier.size(tokens.scanIconSize),
                )
            }
        }
    }
}

@Composable
private fun MetricsSection(
    metrics: DiagnosticsMetrics,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.aura.diagnostics
    val hydration = metrics.hydration.takeIf { it.isNotBlank() } ?: "—"
    val oiliness = metrics.oiliness.takeIf { it.isNotBlank() } ?: "—"
    val ph = metrics.ph.takeIf { it.isNotBlank() } ?: "—"
    val sensitivity = metrics.sensitivity.takeIf { it.isNotBlank() } ?: "—"

    Column(
        verticalArrangement = Arrangement.spacedBy(tokens.metricsGap),
    ) {
        Text(
            text = "Показатели",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = tokens.sectionTitleStartPadding),
        )

        if (isLoading) {
            Text(
                text = "Загрузка показателей...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = tokens.loadingTextAlpha),
                modifier = Modifier.padding(start = tokens.sectionTitleStartPadding),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.metricRowGap),
        ) {
            MetricCard(
                label = "Влага",
                value = hydration,
                color = MintGreen,
                icon = "💧",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Жирность",
                value = oiliness,
                color = PinkAccent,
                icon = "✨",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.metricRowGap),
        ) {
            MetricCard(
                label = "pH",
                value = ph,
                color = Lavender,
                icon = "⚖️",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Чувствит.",
                value = sensitivity,
                color = tokens.sensitivityColor,
                icon = "🎯",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    icon: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.aura.diagnostics

    Box(
        modifier = modifier
            .shadow(
                elevation = tokens.metricCardShadowElevation,
                shape = RoundedCornerShape(tokens.metricCardRadius),
                ambientColor = color.copy(alpha = tokens.metricCardShadowAlpha),
            )
            .clip(RoundedCornerShape(tokens.metricCardRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = tokens.metricCardGradientStartAlpha),
                        color.copy(alpha = tokens.metricCardGradientEndAlpha),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.padding(tokens.metricCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(tokens.metricIconBottomGap))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(modifier = Modifier.height(tokens.metricValueBottomGap))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = tokens.secondaryTextAlpha),
            )
        }
    }
}

@Composable
private fun IoTSection(
    device: DiagnosticsDevice,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.aura.diagnostics
    val deviceName = device.name.takeIf { it.isNotBlank() } ?: "Датчик не найден"
    val deviceStatus = device.status.takeIf { it.isNotBlank() } ?: "Нет соединения"
    val battery = device.battery.takeIf { it.isNotBlank() } ?: "—"

    Column(verticalArrangement = Arrangement.spacedBy(tokens.iotGap)) {
        Text(
            text = "Устройства",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = tokens.sectionTitleStartPadding),
        )

        if (isLoading) {
            Text(
                text = "Поиск устройств...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = tokens.loadingTextAlpha),
                modifier = Modifier.padding(start = tokens.sectionTitleStartPadding),
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = tokens.iotCardSurfaceAlpha),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    modifier = Modifier.size(tokens.iotIconSurfaceSize),
                    shape = CircleShape,
                    color = MintGreen.copy(alpha = tokens.iotIconSurfaceAlpha),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(tokens.iotIconSize),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(tokens.iotContentGap))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(tokens.iotStatusIconSize),
                        )
                        Spacer(modifier = Modifier.width(tokens.iotStatusGap))
                        Text(
                            text = deviceStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MintGreen,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(tokens.batteryRadius),
                    color = MintGreen.copy(alpha = tokens.batterySurfaceAlpha),
                ) {
                    Text(
                        text = battery,
                        style = MaterialTheme.typography.labelMedium,
                        color = MintGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = tokens.batteryHorizontalPadding, vertical = tokens.batteryVerticalPadding),
                    )
                }
            }
        }
    }
}
