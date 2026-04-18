package com.aura.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

const val AuraGlassBarAlpha = 0.90f
const val AuraGlassBarBorderAlpha = 0.95f

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = AuraGlassBarAlpha))
            .border(1.dp, Color.White.copy(alpha = AuraGlassBarBorderAlpha), shape),
    ) {
        content()
    }
}
