package com.aura.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.i18n.StringsRu
import com.aura.core.data.repository.TokenManager
import com.aura.core.ui.theme.*

// ─── Palette ──────────────────────────────────────────────
val PrimaryBlue = Color(0xFF197FE6)
val AuraLavender = Color(0xFFE0C3FC)
val AuraMint = Color(0xFFA7F3D0)
val AuraIce = Color(0xFFF4F7FE)
val SlateBlue = Color(0xFF2D3748)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)

// ─── Screen ───────────────────────────────────────────────
@Composable
fun HomeScreen(
    onNavigateToProduct: (String) -> Unit = {}
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val bg = if (dark) Color(0xFF0A0A0A) else AuraIce
    val glassAlpha = if (dark) 0.08f else 0.45f
    val glassBorderAlpha = if (dark) 0.15f else 0.6f
    val textPrimary = if (dark) Color(0xFFF1F5F9) else Slate800
    val textSecondary = if (dark) Color(0xFF94A3B8) else Slate500
    val textBody = if (dark) Color(0xFFCBD5E1) else Slate700
    val userName = TokenManager.getUser()?.name?.takeIf { it.isNotBlank() } ?: StringsRu.Common.userFallback

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LiquidMeshBackground(dark = dark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 100.dp)
        ) {
            HeaderSection(userName = userName, textPrimary = textPrimary, textSecondary = textSecondary, dark = dark)
            Spacer(modifier = Modifier.height(32.dp))
            TopWidgetSection(textPrimary = textPrimary, textSecondary = textSecondary, textBody = textBody, glassAlpha = glassAlpha, glassBorderAlpha = glassBorderAlpha, dark = dark)
            Spacer(modifier = Modifier.height(32.dp))
            MorningRitualSection(textPrimary = textPrimary, textSecondary = textSecondary, textBody = textBody, glassAlpha = glassAlpha, glassBorderAlpha = glassBorderAlpha, dark = dark)
            Spacer(modifier = Modifier.height(32.dp))
            AiInsightsSection(textPrimary = textPrimary, textSecondary = textSecondary, textBody = textBody, glassAlpha = glassAlpha, glassBorderAlpha = glassBorderAlpha, dark = dark)
        }
    }
}

// ─── Background ───────────────────────────────────────────
@Composable
private fun LiquidMeshBackground(dark: Boolean) {
    val lavenderAlpha = if (dark) 0.15f else 0.4f
    val mintAlpha = if (dark) 0.1f else 0.4f
    val iceAlpha = if (dark) 0.08f else 0.5f

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.size(400.dp).offset(x = (-50).dp, y = (-50).dp).blur(80.dp)) {
            drawCircle(color = AuraLavender.copy(alpha = lavenderAlpha))
        }
        Canvas(modifier = Modifier.size(350.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = 100.dp).blur(80.dp)) {
            drawCircle(color = AuraMint.copy(alpha = mintAlpha))
        }
        Canvas(modifier = Modifier.size(450.dp).align(Alignment.BottomStart).offset(x = (-50).dp, y = 100.dp).blur(80.dp)) {
            drawCircle(color = Color(0xFFDBEAFE).copy(alpha = iceAlpha))
        }
    }
}

// ─── Header ───────────────────────────────────────────────
@Composable
private fun HeaderSection(userName: String, textPrimary: Color, textSecondary: Color, dark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = StringsRu.Home.today, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textSecondary, modifier = Modifier.alpha(0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${StringsRu.Home.goodMorningPrefix},\n$userName", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = textPrimary, lineHeight = 36.sp)
        }
        Column(
            modifier = Modifier
                .size(width = 80.dp, height = 96.dp)
                .glassPanel(RoundedCornerShape(16.dp), dark = dark)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Rounded.WbSunny, contentDescription = StringsRu.Home.weather, tint = Color(0xFFEAB308), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "22\u00B0C", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dark) Color(0xFFCBD5E1) else Slate700)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.background(Color(0xFFFEE2E2), RoundedCornerShape(percent = 50)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(text = "UV 6.0", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
            }
        }
    }
}

// ─── Top Widget ───────────────────────────────────────────
@Composable
private fun TopWidgetSection(textPrimary: Color, textSecondary: Color, textBody: Color, glassAlpha: Float, glassBorderAlpha: Float, dark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(24.dp), dark = dark).padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.background(Color(0xFFEFF6FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                Icon(imageVector = Icons.Rounded.WaterDrop, contentDescription = null, tint = PrimaryBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = StringsRu.Home.humidity, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textBody)
                Text(text = StringsRu.Home.humiditySubtitle, fontSize = 12.sp, color = textSecondary)
            }
        }
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE2E8F0).copy(alpha = 0.5f)))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = StringsRu.Home.airQuality, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = StringsRu.Home.airGood, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
            }
        }
    }
}

// ─── Morning Ritual ───────────────────────────────────────
@Composable
private fun MorningRitualSection(textPrimary: Color, textSecondary: Color, textBody: Color, glassAlpha: Float, glassBorderAlpha: Float, dark: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = StringsRu.Home.ritual, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Box(modifier = Modifier.background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(text = StringsRu.Home.ritualStepsLeft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(32.dp), dark = dark)) {
            Canvas(modifier = Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).blur(40.dp)) {
                drawCircle(Brush.radialGradient(listOf(AuraLavender.copy(alpha = 0.3f), AuraMint.copy(alpha = 0.3f))))
            }
            Column(modifier = Modifier.padding(24.dp)) {
                RitualItem(checked = true, title = StringsRu.Home.ritualCleanser, subtitle = StringsRu.Home.ritualCleanserSubtitle, textBody = textBody, textSecondary = textSecondary, dark = dark)
                Divider(modifier = Modifier.padding(start = 40.dp, top = 16.dp, bottom = 16.dp), color = if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f))
                RitualItem(checked = true, title = StringsRu.Home.ritualVitaminC, subtitle = StringsRu.Home.ritualVitaminCSubtitle, textBody = textBody, textSecondary = textSecondary, dark = dark)
                Divider(modifier = Modifier.padding(start = 40.dp, top = 16.dp, bottom = 16.dp), color = if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f))
                RitualItem(checked = false, title = StringsRu.Home.ritualGel, subtitle = StringsRu.Home.ritualGelSubtitle, isActive = true, textBody = textBody, textSecondary = textSecondary, dark = dark)
                Divider(modifier = Modifier.padding(start = 40.dp, top = 16.dp, bottom = 16.dp), color = if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f))
                RitualItem(checked = false, title = StringsRu.Home.ritualSpf, subtitle = StringsRu.Home.ritualSpfSubtitle, isWarning = true, textBody = textBody, textSecondary = textSecondary, dark = dark)
            }
        }
    }
}

@Composable
private fun RitualItem(checked: Boolean, title: String, subtitle: String, isActive: Boolean = false, isWarning: Boolean = false, textBody: Color, textSecondary: Color, dark: Boolean) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isChecked = !isChecked }) {
        CustomCheckbox(checked = isChecked, dark = dark)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).alpha(if (isChecked) 0.5f else 1f)) {
            Text(text = title, fontSize = if (isActive) 16.sp else 14.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold, color = textBody, textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(text = subtitle, fontSize = 12.sp, fontWeight = if (isActive || isWarning) FontWeight.Medium else FontWeight.Normal, color = when {
                    isWarning -> Color(0xFFF87171)
                    isActive -> PrimaryBlue
                    else -> textSecondary
                })
            }
        }
        if (isActive) {
            Box(modifier = Modifier.size(32.dp).background(if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CustomCheckbox(checked: Boolean, dark: Boolean) {
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(if (checked) PrimaryBlue else if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.5f)).border(1.dp, if (checked) PrimaryBlue else PrimaryBlue.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(imageVector = Icons.Rounded.Check, contentDescription = StringsRu.Home.checked, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

// ─── AI Insights ──────────────────────────────────────────
@Composable
private fun AiInsightsSection(textPrimary: Color, textSecondary: Color, textBody: Color, glassAlpha: Float, glassBorderAlpha: Float, dark: Boolean) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(text = StringsRu.Home.aiInsights, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Text(text = StringsRu.Home.viewAll, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InsightCard(icon = Icons.Rounded.Face, iconTint = PrimaryBlue, bgTint = PrimaryBlue.copy(alpha = 0.1f), title = StringsRu.Home.weeklyScan, subtitle = StringsRu.Home.weeklyScanSubtitle, textBody = textBody, textSecondary = textSecondary, dark = dark)
            InsightCard(icon = Icons.Rounded.Star, iconTint = Color(0xFF16A34A), bgTint = Color(0xFF22C55E).copy(alpha = 0.1f), title = StringsRu.Home.refresh, subtitle = StringsRu.Home.refreshSubtitle, textBody = textBody, textSecondary = textSecondary, dark = dark)
            InsightCard(icon = Icons.Rounded.WaterDrop, iconTint = Color(0xFF9333EA), bgTint = Color(0xFFA855F7).copy(alpha = 0.1f), title = StringsRu.Home.hydrationAlert, subtitle = StringsRu.Home.hydrationAlertSubtitle, textBody = textBody, textSecondary = textSecondary, dark = dark)
        }
    }
}

@Composable
private fun InsightCard(icon: ImageVector, iconTint: Color, bgTint: Color, title: String, subtitle: String, textBody: Color, textSecondary: Color, dark: Boolean) {
    Column(
        modifier = Modifier.width(200.dp).height(128.dp).glassPanel(RoundedCornerShape(16.dp), dark = dark).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.size(32.dp).background(bgTint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textBody, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 10.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── Glass Modifier ───────────────────────────────────────
fun Modifier.glassPanel(shape: androidx.compose.ui.graphics.Shape, dark: Boolean): Modifier {
    val bgAlpha = if (dark) 0.08f else 0.45f
    val borderAlpha = if (dark) 0.15f else 0.6f
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = bgAlpha))
        .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
}
