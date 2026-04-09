package com.aura.feature.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.i18n.I18n
import com.aura.core.i18n.StringsRu
import com.aura.core.data.repository.SkinPassportManager
import com.aura.core.data.repository.TokenManager
import com.aura.core.ui.theme.*

// ─── Palette ──────────────────────────────────────────────
private val IceBlue = Color(0xFFF4F7FE)
private val AuraMint = Color(0xFFA7F3D0)
private val AuraLavender = Color(0xFFE9D5FF)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)

// ─── Screen ───────────────────────────────────────────────
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToSurvey: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(3) }
    AuraProfileScreen(selectedTab = selectedTab, onTabSelected = {
        selectedTab = it
        when (it) {
            0 -> onNavigateToHome()
            1 -> onNavigateToDiagnostics()
            2 -> onNavigateToChat()
        }
    }, onBack = onBack, onNavigateToSurvey = onNavigateToSurvey)
}

@Composable
private fun AuraProfileScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToSurvey: () -> Unit
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val bg = if (dark) Color(0xFF0A0A0A) else IceBlue
    val glassAlpha = if (dark) 0.08f else 0.45f
    val glassBorderAlpha = if (dark) 0.15f else 0.6f
    val textPrimary = if (dark) Color(0xFFF1F5F9) else Slate800
    val textBody = if (dark) Color(0xFFCBD5E1) else Slate700
    val textSecondary = if (dark) Color(0xFF94A3B8) else Slate500
    val textMuted = if (dark) Color(0xFF64748B) else Slate400
    val cardBg = if (dark) Color.White.copy(alpha = 0.06f) else Color.White
    val cardBorder = if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f)
    val iconBoxBg = if (dark) Color.White.copy(alpha = 0.1f) else Color.White

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        MeshBackground(dark = dark)

        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection(onBack = onBack, textSecondary = textSecondary, dark = dark)
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)
            ) {
                ProfileSection(textPrimary = textPrimary, textSecondary = textSecondary, textBody = textBody, dark = dark, glassAlpha = glassAlpha, cardBorder = cardBorder)
                Spacer(modifier = Modifier.height(32.dp))
                SkinPassportSection(
                    textBody = textBody,
                    textSecondary = textSecondary,
                    glassAlpha = glassAlpha,
                    cardBorder = cardBorder,
                    dark = dark,
                    onNavigateToSurvey = onNavigateToSurvey
                )
                Spacer(modifier = Modifier.height(24.dp))
                HydrationJourneySection(textPrimary = textPrimary, textSecondary = textSecondary, textBody = textBody, glassAlpha = glassAlpha, cardBorder = cardBorder, dark = dark)
                Spacer(modifier = Modifier.height(24.dp))
                MenuListSection(textBody = textBody, textSecondary = textSecondary, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, dark = dark)
            }
        }

        BottomNavigationBar(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), selectedTab = selectedTab, onTabSelected = onTabSelected, glassAlpha = glassAlpha, cardBorder = cardBorder, textSecondary = textSecondary, textBody = textBody, dark = dark)
    }
}

// ─── Mesh Background ─────────────────────────────────────
@Composable
private fun MeshBackground(dark: Boolean) {
    val mintA = if (dark) 0.15f else 0.6f
    val lavA = if (dark) 0.1f else 0.7f
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
            drawCircle(color = AuraMint.copy(alpha = mintA), radius = 400f, center = Offset(size.width * 0.1f, size.height * 0.1f))
            drawCircle(color = AuraLavender.copy(alpha = lavA), radius = 500f, center = Offset(size.width * 0.9f, size.height * 0.1f))
            drawCircle(color = if (dark) Color(0xFF1E293B) else IceBlue, radius = 600f, center = Offset(size.width * 0.5f, size.height * 0.5f))
            drawCircle(color = AuraMint.copy(alpha = mintA * 0.8f), radius = 450f, center = Offset(size.width * 0.8f, size.height * 0.9f))
            drawCircle(color = AuraLavender.copy(alpha = lavA * 0.8f), radius = 500f, center = Offset(size.width * 0.1f, size.height * 0.9f))
        }
    }
}

// ─── Header ───────────────────────────────────────────────
@Composable
private fun HeaderSection(onBack: () -> Unit, textSecondary: Color, dark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = StringsRu.Common.back, tint = textSecondary)
        }
        Text(text = StringsRu.Profile.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 2.sp)
        IconButton(onClick = { I18n.toggleLanguage() }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Settings, contentDescription = StringsRu.Common.settings, tint = textSecondary)
        }
    }
}

// ─── Profile Section ──────────────────────────────────────
@Composable
private fun ProfileSection(textPrimary: Color, textSecondary: Color, textBody: Color, dark: Boolean, glassAlpha: Float, cardBorder: Color) {
    val userName = TokenManager.getUser()?.name?.takeIf { it.isNotBlank() } ?: StringsRu.Common.userFallback

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(120.dp).background(Brush.linearGradient(listOf(AuraMint.copy(0.4f), AuraLavender.copy(0.4f))), CircleShape).blur(20.dp))
            Box(modifier = Modifier.size(112.dp).clip(CircleShape).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, CircleShape).padding(4.dp).background(if (dark) Color.White.copy(alpha = 0.06f) else Slate400.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = textSecondary, modifier = Modifier.size(48.dp))
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).size(32.dp).background(if (dark) Color(0xFF1E293B) else Color.White, CircleShape).border(1.dp, AuraMint, CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Edit, contentDescription = StringsRu.Common.edit, tint = textBody, modifier = Modifier.size(16.dp))
            }
        }
        Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            PulsingDot()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "\u0412 Aura \u0441 2023", fontSize = 14.sp, color = textSecondary)
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse))
    Box(modifier = Modifier.size(8.dp).background(AuraMint.copy(alpha = alpha), CircleShape))
}

// ─── Skin Passport ────────────────────────────────────────
@Composable
private fun SkinPassportSection(textBody: Color, textSecondary: Color, glassAlpha: Float, cardBorder: Color, dark: Boolean) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\u041F\u0430\u0441\u043F\u043E\u0440\u0442 \u043A\u043E\u0436\u0438", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textBody)
            Text(text = "\u0412\u0441\u0435 \u043F\u0430\u0440\u0430\u043C\u0435\u0442\u0440\u044B", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PassportCard(Icons.Rounded.WaterDrop, Color(0xFF60A5FA), Color(0xFFDBEAFE), "\u041A\u043E\u043C\u0431\u0438\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u0430\u044F", "\u0422\u0418\u041F \u041A\u041E\u0416\u0418", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.WbSunny, Color(0xFFFB923C), Color(0xFFFFEDD5), "\u0427\u0443\u0432\u0441\u0442\u0432\u0438\u0442\u0435\u043B\u044C\u043D\u0430\u044F", "\u0420\u0415\u0410\u041A\u0426\u0418\u042F \u041D\u0410 \u0421\u041E\u041B\u041D\u0426\u0415", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.BubbleChart, Color(0xFFF87171), Color(0xFFFEE2E2), "\u0410\u043A\u043D\u0435", "\u041F\u0420\u041E\u0411\u041B\u0415\u041C\u0410", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.Face, Color(0xFFA855F7), Color(0xFFF3E8FF), "\u0425\u043E\u0440\u043E\u0448\u0430\u044F", "\u042D\u041B\u0410\u0421\u0422\u0418\u0427\u041D\u041E\u0421\u0422\u042C", glassAlpha, cardBorder, dark)
        }
    }
}

@Composable
private fun PassportCard(icon: ImageVector, color: Color, bgColor: Color, title: String, subtitle: String, glassAlpha: Float, cardBorder: Color, dark: Boolean) {
    Column(modifier = Modifier.width(144.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, RoundedCornerShape(16.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(40.dp).background(bgColor.copy(alpha = 0.5f), CircleShape).border(1.dp, bgColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (dark) Color(0xFFCBD5E1) else Slate700)
            Text(text = subtitle, fontSize = 10.sp, color = if (dark) Color(0xFF94A3B8) else Slate500, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SkinPassportSection(
    textBody: Color,
    textSecondary: Color,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    onNavigateToSurvey: () -> Unit
) {
    val passport = SkinPassportManager.passport
    val skinType = passport?.answers?.get("skin_type")?.firstOrNull() ?: "Не заполнен"
    val mainIssue = passport?.answers?.get("skin_issues")?.firstOrNull() ?: "Не определено"
    val sensitivity = passport?.answers?.get("new_products_reaction")?.firstOrNull() ?: "Не указано"
    val goal = passport?.answers?.get("goals")?.firstOrNull() ?: "Не выбрана"

    val actionTitle = if (passport == null) "Пройти анкету" else "Обновить анкету"
    val actionSubtitle = if (passport == null) "ПАСПОРТ КОЖИ" else "АКТУАЛИЗАЦИЯ"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Паспорт кожи", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textBody)
            Text(text = "Все параметры", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PassportCard(Icons.Rounded.WaterDrop, Color(0xFF60A5FA), Color(0xFFDBEAFE), skinType, "ТИП КОЖИ", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.BubbleChart, Color(0xFFF87171), Color(0xFFFEE2E2), mainIssue, "КЛЮЧЕВАЯ ПРОБЛЕМА", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.WbSunny, Color(0xFFFB923C), Color(0xFFFFEDD5), sensitivity, "ЧУВСТВИТЕЛЬНОСТЬ", glassAlpha, cardBorder, dark)
            PassportCard(Icons.Rounded.Face, Color(0xFFA855F7), Color(0xFFF3E8FF), goal, "ЦЕЛЬ УХОДА", glassAlpha, cardBorder, dark)
            PassportCard(
                icon = Icons.Rounded.Assignment,
                color = Color(0xFF059669),
                bgColor = Color(0xFFD1FAE5),
                title = actionTitle,
                subtitle = actionSubtitle,
                glassAlpha = glassAlpha,
                cardBorder = cardBorder,
                dark = dark,
                onClick = onNavigateToSurvey
            )
        }
    }
}

@Composable
private fun PassportCard(
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    title: String,
    subtitle: String,
    glassAlpha: Float,
    cardBorder: Color,
    dark: Boolean,
    onClick: (() -> Unit)?
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Column(
        modifier = Modifier
            .width(144.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = glassAlpha))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .then(clickableModifier)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(40.dp).background(bgColor.copy(alpha = 0.5f), CircleShape).border(1.dp, bgColor, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (dark) Color(0xFFCBD5E1) else Slate700)
            Text(text = subtitle, fontSize = 10.sp, color = if (dark) Color(0xFF94A3B8) else Slate500, letterSpacing = 1.sp)
        }
    }
}

// ─── Hydration Journey ────────────────────────────────────
@Composable
private fun HydrationJourneySection(textPrimary: Color, textSecondary: Color, textBody: Color, glassAlpha: Float, cardBorder: Color, dark: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, RoundedCornerShape(24.dp)).padding(24.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "\u041F\u0443\u0442\u044C \u043A \u0443\u0432\u043B\u0430\u0436\u043D\u0435\u043D\u0438\u044E", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(text = "\u0421\u0440\u0435\u0434\u043D\u0435\u0435 \u0437\u0430 7 \u0434\u043D\u0435\u0439", fontSize = 12.sp, color = textSecondary)
                }
                Column(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp), horizontalAlignment = Alignment.End) {
                    Text(text = "72%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+4%", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF16A34A))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HydrationChart(textSecondary = textSecondary, textMuted = if (dark) Color(0xFF64748B) else Slate400, dark = dark)
        }
    }
}

@Composable
private fun HydrationChart(textSecondary: Color, textMuted: Color, dark: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Divider(color = textSecondary.copy(alpha = 0.2f))
            Divider(color = textSecondary.copy(alpha = 0.2f), modifier = Modifier.alpha(0.5f))
            Divider(color = textSecondary.copy(alpha = 0.2f), modifier = Modifier.alpha(0.5f))
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartPath = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(size.width * 0.16f, size.height * 0.7f, size.width * 0.26f, size.height * 0.4f, size.width * 0.4f, size.height * 0.45f)
                cubicTo(size.width * 0.53f, size.height * 0.5f, size.width * 0.66f, size.height * 0.2f, size.width * 0.8f, size.height * 0.3f)
                cubicTo(size.width * 0.9f, size.height * 0.38f, size.width, size.height * 0.25f, size.width, size.height * 0.25f)
            }
            val fillPath = Path().apply { addPath(chartPath); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            drawPath(path = fillPath, brush = Brush.verticalGradient(colors = listOf(Color(0xFF34D399).copy(alpha = 0.5f), Color(0xFF34D399).copy(alpha = 0.0f))))
            drawPath(path = chartPath, color = Color(0xFF34D399), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(size.width * 0.4f, size.height * 0.45f))
            drawCircle(color = Color(0xFF34D399), radius = 4.dp.toPx(), center = Offset(size.width * 0.4f, size.height * 0.45f), style = Stroke(2.dp.toPx()))
            drawCircle(color = Color(0xFF34D399), radius = 5.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.3f))
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.3f), style = Stroke(2.dp.toPx()))
        }
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).offset(y = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("\u041F\u043D", "\u0421\u0440", "\u041F\u0442", "\u0412\u0441").forEach { day ->
                Text(text = day, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textMuted, letterSpacing = 1.sp)
            }
        }
    }
}

// ─── Menu List ────────────────────────────────────────────
@Composable
private fun MenuListSection(textBody: Color, textSecondary: Color, textMuted: Color, glassAlpha: Float, cardBorder: Color, iconBoxBg: Color, dark: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MenuItem(icon = Icons.Rounded.Spa, title = "\u041C\u043E\u044F \u0440\u0443\u0442\u0438\u043D\u0430", textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, dark = dark)
        MenuItem(icon = Icons.Rounded.History, title = "\u0418\u0441\u0442\u043E\u0440\u0438\u044F \u0430\u043D\u0430\u043B\u0438\u0437\u043E\u0432", textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, dark = dark)
        MenuItem(icon = Icons.Rounded.Notifications, title = "\u0423\u0432\u0435\u0434\u043E\u043C\u043B\u0435\u043D\u0438\u044F", hasNotification = true, textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, dark = dark)
        MenuItem(icon = Icons.Rounded.Star, title = "\u041F\u043E\u0434\u043F\u0438\u0441\u043A\u0430", isPro = true, textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, dark = dark)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFF87171)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFEE2E2))) {
            Text("\u0412\u044B\u0439\u0442\u0438", fontSize = 14.sp)
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, hasNotification: Boolean = false, isPro: Boolean = false, textBody: Color, textMuted: Color, glassAlpha: Float, cardBorder: Color, iconBoxBg: Color, dark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, RoundedCornerShape(16.dp)).clickable { }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(iconBoxBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = textMuted, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textBody)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasNotification) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF87171), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isPro) {
                Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(AuraMint, Color(0xFF93C5FD))), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("PRO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = textMuted)
            }
        }
    }
}

// ─── Bottom Nav ───────────────────────────────────────────
@Composable
private fun BottomNavigationBar(modifier: Modifier = Modifier, selectedTab: Int, onTabSelected: (Int) -> Unit, glassAlpha: Float, cardBorder: Color, textSecondary: Color, textBody: Color, dark: Boolean) {
    Row(modifier = modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = glassAlpha)).border(1.dp, cardBorder, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BottomNavItem(icon = Icons.Rounded.Home, title = "\u0413\u043B\u0430\u0432\u043D\u0430\u044F", isActive = selectedTab == 0, onTabSelected = { onTabSelected(0) }, textBody = textBody, textMuted = textSecondary, dark = dark)
        BottomNavItem(icon = Icons.Rounded.Spa, title = "\u0423\u0445\u043E\u0434", isActive = selectedTab == 1, onTabSelected = { onTabSelected(1) }, textBody = textBody, textMuted = textSecondary, dark = dark)
        BottomNavItem(icon = Icons.Rounded.CenterFocusStrong, title = "\u0410\u043D\u0430\u043B\u0438\u0437", isActive = selectedTab == 2, onTabSelected = { onTabSelected(2) }, textBody = textBody, textMuted = textSecondary, dark = dark)
        BottomNavItem(icon = Icons.Rounded.Person, title = "\u041F\u0440\u043E\u0444\u0438\u043B\u044C", isActive = selectedTab == 3, onTabSelected = { onTabSelected(3) }, textBody = textBody, textMuted = textSecondary, dark = dark)
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, title: String, isActive: Boolean, onTabSelected: () -> Unit, textBody: Color, textMuted: Color, dark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTabSelected() }.padding(4.dp)) {
        if (isActive) {
            Box(modifier = Modifier.background(if (dark) Color.White.copy(alpha = 0.12f) else AuraMint.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(6.dp)) {
                Icon(icon, contentDescription = title, tint = textBody, modifier = Modifier.size(24.dp))
            }
        } else {
            Icon(icon, contentDescription = title, tint = textMuted, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium, color = if (isActive) textBody else textMuted)
    }
}
