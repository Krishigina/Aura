package com.aura.feature.profile.presentation.components.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp

@Composable
fun MenuListSection(
    textBody: Color,
    textMuted: Color,
    glassAlpha: Float,
    cardBorder: Color,
    iconBoxBg: Color,
    onLogout: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = auraTokenDp(24f)), verticalArrangement = Arrangement.spacedBy(auraTokenDp(12f))) {
        MenuItem(icon = Icons.Rounded.AutoAwesome, title = "Моя рутина", textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, onClick = onNavigateToRoutine)
        MenuItem(icon = Icons.Rounded.Warning, title = "Уведомления", textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, onClick = onNavigateToNotifications)

        MenuItem(icon = Icons.Rounded.History, title = "Журнал кожи", textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, onClick = onNavigateToJournal)
        MenuItem(icon = Icons.Rounded.Settings, title = "Настройки", textBody = textBody, textMuted = textMuted, glassAlpha = glassAlpha, cardBorder = cardBorder, iconBoxBg = iconBoxBg, onClick = onNavigateToSettings)
        Spacer(modifier = Modifier.height(auraTokenDp(8f)))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(auraTokenDp(48f)), colors = ButtonDefaults.buttonColors(containerColor = auraHex(0xFFF87171), contentColor = Color.White), shape = RoundedCornerShape(auraTokenDp(16f)), border = BorderStroke(auraTokenDp(1f), auraHex(0xFFF87171))) {
            Text("\u0412\u044B\u0439\u0442\u0438", fontSize = auraTokenSp(14f))
        }
    }
}

@Composable
fun MenuItem(icon: ImageVector, title: String, hasNotification: Boolean = false, isPro: Boolean = false, textBody: Color, textMuted: Color, glassAlpha: Float, cardBorder: Color, iconBoxBg: Color, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(auraTokenDp(16f))).background(Color.White.copy(alpha = glassAlpha)).border(auraTokenDp(1f), cardBorder, RoundedCornerShape(auraTokenDp(16f))).clickable { onClick() }.padding(auraTokenDp(16f)), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(auraTokenDp(40f)).background(iconBoxBg, RoundedCornerShape(auraTokenDp(12f))), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = textMuted, modifier = Modifier.size(auraTokenDp(20f)))
            }
            Spacer(modifier = Modifier.width(auraTokenDp(16f)))
            Text(text = title, fontSize = auraTokenSp(14f), fontWeight = FontWeight.SemiBold, color = textBody)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasNotification) {
                Box(modifier = Modifier.size(auraTokenDp(8f)).background(auraHex(0xFFF87171), CircleShape))
                Spacer(modifier = Modifier.width(auraTokenDp(8f)))
            }
            if (isPro) {
                Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(MaterialTheme.aura.profile.auraMint, auraHex(0xFF93C5FD))), RoundedCornerShape(auraTokenDp(4f))).padding(horizontal = auraTokenDp(8f), vertical = auraTokenDp(2f))) {
                    Text("PRO", color = Color.White, fontSize = auraTokenSp(10f), fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = textMuted)
            }
        }
    }
}
