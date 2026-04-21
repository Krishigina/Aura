package com.aura.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.aura.core.ui.theme.AuraPalette

@Composable
fun AuraLotusLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawAuraLotusLogo()
    }
}

private fun DrawScope.drawAuraLotusLogo() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val petalColors = listOf(
        AuraPalette.BrandRose,
        AuraPalette.BrandPink,
        AuraPalette.BlobPink,
        AuraPalette.Warning,
        AuraPalette.SurfaceSoftLavender
    )
    val angles = listOf(0f, -30f, 30f, -55f, 55f)
    val petalWidths = listOf(0.35f, 0.45f, 0.45f, 0.55f, 0.55f)
    val petalHeights = listOf(0.9f, 0.75f, 0.75f, 0.6f, 0.6f)

    for (i in angles.indices) {
        rotate(degrees = angles[i]) {
            val gradient = Brush.verticalGradient(
                colors = listOf(petalColors[i], petalColors[i].copy(alpha = 0.6f)),
                startY = 0f,
                endY = size.height
            )
            drawPetal(
                cx = cx,
                cy = cy + size.height * 0.15f,
                width = size.width * petalWidths[i],
                height = size.height * petalHeights[i],
                brush = gradient
            )
        }
    }
}

private fun DrawScope.drawPetal(cx: Float, cy: Float, width: Float, height: Float, brush: Brush) {
    val path = Path().apply {
        moveTo(cx, cy + height * 0.5f)
        cubicTo(
            cx - width * 0.5f, cy + height * 0.1f,
            cx - width * 0.5f, cy - height * 0.4f,
            cx, cy - height * 0.5f
        )
        cubicTo(
            cx + width * 0.5f, cy - height * 0.4f,
            cx + width * 0.5f, cy + height * 0.1f,
            cx, cy + height * 0.5f
        )
        close()
    }
    drawPath(path = path, brush = brush, alpha = 0.85f)
}
