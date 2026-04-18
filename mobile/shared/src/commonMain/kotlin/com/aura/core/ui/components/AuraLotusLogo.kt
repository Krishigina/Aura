package com.aura.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aura.core.ui.theme.defaultAuraDesignTokens

@Composable
fun AuraLotusLogo(
    modifier: Modifier = Modifier,
    colors: List<Color> = defaultAuraDesignTokens().brand.logoGradient,
    markColor: Color = Color.White,
    showBackground: Boolean = true,
    monochromeWhenNoBackground: Boolean = false,
) {
    Canvas(modifier = modifier) {
        drawAuraLotusLogo(
            colors = colors,
            markColor = markColor,
            showBackground = showBackground,
            monochromeWhenNoBackground = monochromeWhenNoBackground,
        )
    }
}

private fun DrawScope.drawAuraLotusLogo(
    colors: List<Color>,
    markColor: Color,
    showBackground: Boolean,
    monochromeWhenNoBackground: Boolean,
) {
    val width = size.width
    val height = size.height
    val logoGradient = Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(width, height),
    )

    if (showBackground) {
        drawRoundRect(
            brush = logoGradient,
            cornerRadius = CornerRadius(width * 0.2f, height * 0.2f),
        )
    }

    val path = Path().apply {
        moveTo(width * 0.25f, height * 0.72f)
        cubicTo(
            width * 0.34f,
            height * 0.58f,
            width * 0.43f,
            height * 0.38f,
            width * 0.50f,
            height * 0.27f,
        )
        cubicTo(
            width * 0.57f,
            height * 0.38f,
            width * 0.66f,
            height * 0.58f,
            width * 0.75f,
            height * 0.72f,
        )
    }

    val stroke = Stroke(width = width * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (showBackground) {
        drawPath(path = path, color = markColor, style = stroke, alpha = 0.96f)
        drawCircle(
            color = markColor,
            radius = width * 0.055f,
            center = Offset(width * 0.5f, height * 0.76f),
            alpha = 0.96f,
        )
    } else if (monochromeWhenNoBackground) {
        drawPath(path = path, color = markColor, style = stroke, alpha = 0.96f)
        drawCircle(
            color = markColor,
            radius = width * 0.055f,
            center = Offset(width * 0.5f, height * 0.76f),
            alpha = 0.96f,
        )
    } else {
        drawPath(path = path, brush = logoGradient, style = stroke, alpha = 0.96f)
        drawCircle(
            brush = logoGradient,
            radius = width * 0.055f,
            center = Offset(width * 0.5f, height * 0.76f),
            alpha = 0.96f,
        )
    }
}
