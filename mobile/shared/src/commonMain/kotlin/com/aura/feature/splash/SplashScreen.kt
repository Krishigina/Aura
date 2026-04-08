package com.aura.feature.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.AuraLotusLogo

private val BgColor = Color(0xFFF4F7FE)
private val OnSurface = Color(0xFF2D3648)
private val BlobPurple = Color(0xFFE0C3FC)
private val BlobMint = Color(0xFFA7F3D0)
private val BlobPink = Color(0xFFFBCFE8)

@Composable
fun AuraSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentAlignment = Alignment.Center
    ) {
        MeshBackgroundBlobs()
        DecorativeCorners()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoSection()
            Spacer(modifier = Modifier.height(32.dp))
            TextSection()
        }
    }
}

@Composable
private fun MeshBackgroundBlobs() {
    Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
        val width = size.width
        val height = size.height

        val radius1 = width * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobPurple.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = radius1
            ),
            radius = radius1,
            center = Offset(0f, 0f)
        )

        val radius2 = width * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobMint.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(width, height * 0.9f),
                radius = radius2
            ),
            radius = radius2,
            center = Offset(width, height * 0.9f)
        )

        val radius3 = width * 0.35f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BlobPink.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(width * 0.2f, height * 0.5f),
                radius = radius3
            ),
            radius = radius3,
            center = Offset(width * 0.2f, height * 0.5f)
        )
    }
}

@Composable
private fun LogoSection() {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(BlobPurple.copy(alpha = 0.2f), CircleShape)
                .blur(40.dp)
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFF1F2687).copy(alpha = 0.15f),
                    ambientColor = Color(0xFF1F2687).copy(alpha = 0.1f)
                )
                .glassmorphism(shape = CircleShape, alpha = 0.4f, borderAlpha = 0.5f),
            contentAlignment = Alignment.Center
        ) {
            AuraLotusLogo(modifier = Modifier.size(72.dp))
        }
    }
}

@Composable
private fun TextSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = StringsRu.Common.appName,
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface,
            letterSpacing = 8.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = StringsRu.Splash.subtitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface.copy(alpha = 0.6f),
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun BoxScope.DecorativeCorners() {
    Icon(
        imageVector = Icons.Rounded.BubbleChart,
        contentDescription = null,
        tint = BlobPurple.copy(alpha = 0.2f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(48.dp)
            .size(36.dp)
    )
}

private fun Modifier.glassmorphism(
    shape: Shape,
    alpha: Float,
    borderAlpha: Float
): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = alpha))
    .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
