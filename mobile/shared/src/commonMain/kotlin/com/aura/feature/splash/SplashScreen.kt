package com.aura.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aura.core.ui.components.AuraLotusLogo
import com.aura.core.ui.theme.aura
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AuraSplashScreen(
    isAppReady: Boolean,
    isWarmStart: Boolean,
    onSplashFinished: () -> Unit,
) {
    val splash = MaterialTheme.aura.splash
    val durations = remember(isWarmStart) { SplashTimelineDurations.forStart(isWarmStart) }
    val completionGuard = remember { SplashCompletionGuard() }

    val overlayAlpha = remember { Animatable(1f) }
    val plateBlurGlow = remember { Animatable(0.18f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.94f) }
    val backgroundShift = remember { Animatable(0f) }
    val glowDrift = remember { Animatable(0f) }
    val gradientAlpha = remember { Animatable(0f) }
    val colorBloom = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(true) }
    var minimumVisibleReached by remember { mutableStateOf(false) }

    LaunchedEffect(durations) {
        launch {
            gradientAlpha.animateTo(1f, tween(durationMillis = (durations.introMs * 0.92f).toInt(), easing = FastOutSlowInEasing))
        }
        launch {
            plateBlurGlow.animateTo(1f, tween(durationMillis = (durations.introMs * 0.55f).toInt(), easing = FastOutSlowInEasing))
        }
        launch {
            logoAlpha.animateTo(1f, tween(durationMillis = (durations.introMs * 0.22f).toInt(), easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(durationMillis = (durations.introMs * 0.32f).toInt(), easing = FastOutSlowInEasing))
        }
        launch {
            backgroundShift.animateTo(1f, tween(durationMillis = durations.totalMs + 520, easing = LinearEasing))
        }
        launch {
            glowDrift.animateTo(1f, tween(durationMillis = durations.totalMs + 420, easing = FastOutSlowInEasing))
        }
        launch {
            colorBloom.animateTo(1f, tween(durationMillis = durations.totalMs + 260, easing = FastOutSlowInEasing))
        }
        launch {
            delay(durations.minimumVisibleMs.toLong())
            minimumVisibleReached = true
        }
    }

    LaunchedEffect(isAppReady, minimumVisibleReached, visible) {
        if (!isAppReady || !minimumVisibleReached || !visible) return@LaunchedEffect
        delay(durations.settleMs.toLong())
        launch {
            overlayAlpha.animateTo(0f, tween(durationMillis = durations.exitMs, easing = FastOutSlowInEasing))
        }
        launch {
            logoAlpha.animateTo(0f, tween(durationMillis = durations.exitMs - 40, easing = FastOutSlowInEasing))
        }
        launch {
            gradientAlpha.animateTo(0.72f, tween(durationMillis = durations.exitMs, easing = FastOutSlowInEasing))
        }
        delay(durations.exitMs.toLong())
        visible = false
        if (completionGuard.completeOnce()) onSplashFinished()
    }

    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(splash.baseColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = gradientAlpha.value)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            splash.topGradientColor.copy(alpha = 0.72f + colorBloom.value * 0.28f),
                            splash.baseColor,
                            splash.bottomGradientColor.copy(alpha = 0.7f + colorBloom.value * 0.3f),
                        ),
                        startY = -320f + (backgroundShift.value * 360f),
                        endY = 1360f + (backgroundShift.value * 420f),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = gradientAlpha.value * plateBlurGlow.value * 0.96f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            splash.primaryGlowColor.copy(alpha = 0.42f + colorBloom.value * 0.3f),
                            Color.Transparent,
                        ),
                        center = Offset(
                            180f + backgroundShift.value * 380f,
                            390f + glowDrift.value * 120f,
                        ),
                        radius = 1180f + glowDrift.value * 110f,
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = gradientAlpha.value * plateBlurGlow.value * 0.88f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            splash.secondaryGlowColor.copy(alpha = 0.36f + colorBloom.value * 0.26f),
                            Color.Transparent,
                        ),
                        center = Offset(
                            1060f - backgroundShift.value * 340f,
                            260f + glowDrift.value * 170f,
                        ),
                        radius = 980f + glowDrift.value * 80f,
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = gradientAlpha.value * plateBlurGlow.value * 0.34f)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            splash.mistColor.copy(alpha = 0.18f + colorBloom.value * 0.16f),
                            Color.Transparent,
                        ),
                        start = Offset(-320f + backgroundShift.value * 240f, -80f + glowDrift.value * 70f),
                        end = Offset(1340f + backgroundShift.value * 340f, 1460f + glowDrift.value * 180f),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(splash.logoSize)
                .graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                },
            contentAlignment = Alignment.Center,
        ) {
            AuraLotusLogo(
                modifier = Modifier
                    .size(splash.logoSize)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = ((1f - logoScale.value) * 24f).roundToInt(),
                        )
                    },
                markColor = splash.logoMarkColor,
                showBackground = false,
                monochromeWhenNoBackground = true,
            )
        }
    }
}
