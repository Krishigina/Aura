package com.aura.feature.journal.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.journal.JournalZone

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.White.copy(alpha = auraTokenAlpha(0.48f)))
            .border(auraTokenDp(1f), Color.White.copy(alpha = auraTokenAlpha(0.62f)), RoundedCornerShape(cornerRadius.dp))
            .padding(auraTokenDp(20f)),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(12f)),
        content = content,
    )
}

@Composable
fun GlassInset(
    background: Color = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
    border: Color = Color.White.copy(alpha = auraTokenAlpha(0.72f)),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(auraTokenDp(20f)))
            .background(background)
            .border(auraTokenDp(1f), border, RoundedCornerShape(auraTokenDp(20f)))
            .padding(auraTokenDp(10f)),
        verticalArrangement = Arrangement.spacedBy(auraTokenDp(6f)),
        content = content,
    )
}

@Composable
fun StatPill(value: String, label: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(auraTokenDp(20f)))
            .background(background.copy(alpha = auraTokenAlpha(0.82f)))
            .border(auraTokenDp(1f), color.copy(alpha = auraTokenAlpha(0.26f)), RoundedCornerShape(auraTokenDp(20f)))
            .padding(auraTokenDp(14f)),
    ) {
        Text(value, color = MaterialTheme.aura.journal.slate800, fontSize = auraTokenSp(15f), fontWeight = FontWeight.ExtraBold)
        Text(label, color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(12f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = MaterialTheme.aura.journal.slate800, fontSize = auraTokenSp(18f), fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(13f))
    }
}

@Composable
fun SoftRow(title: String, subtitle: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(auraTokenDp(18f)))
            .background(Color.White.copy(alpha = auraTokenAlpha(0.50f)))
            .border(auraTokenDp(1f), Color.White.copy(alpha = auraTokenAlpha(0.64f)), RoundedCornerShape(auraTokenDp(18f)))
            .padding(auraTokenDp(12f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(auraTokenDp(40f))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = auraTokenAlpha(0.72f)), MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.66f))))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Spa, contentDescription = null, tint = Color.White, modifier = Modifier.size(auraTokenDp(18f)))
        }
        Spacer(Modifier.width(auraTokenDp(12f)))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.aura.journal.slate800, fontWeight = FontWeight.Bold, fontSize = auraTokenSp(15f))
            Text(subtitle, color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(12f), lineHeight = auraTokenSp(16f))
        }
    }
}

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(auraTokenDp(999f)))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        color = if (selected) MaterialTheme.aura.journal.neutralSoft else Color.White.copy(alpha = auraTokenAlpha(0.68f)),
        border = BorderStroke(auraTokenDp(1f), if (selected) MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.52f)) else Color.White.copy(alpha = auraTokenAlpha(0.72f))),
    ) {
        Text(
            label,
            color = MaterialTheme.aura.journal.slate700,
            modifier = Modifier.padding(horizontal = auraTokenDp(14f), vertical = auraTokenDp(9f)),
            fontWeight = FontWeight.SemiBold,
            fontSize = auraTokenSp(13f),
        )
    }
}

@Composable
fun FlowRows(items: List<JournalZone>, content: @Composable (JournalZone) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
                rowItems.forEach { content(it) }
            }
        }
    }
}

@Composable
fun EventDot(color: Color) {
    Box(modifier = Modifier.size(auraTokenDp(5f)).clip(CircleShape).background(color))
}

@Composable
fun EventMark(text: String, background: Color, border: Color) {
    Box(
        modifier = Modifier
            .size(auraTokenDp(13f))
            .clip(CircleShape)
            .background(background)
            .border(auraTokenDp(1f), border.copy(alpha = auraTokenAlpha(0.45f)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.aura.journal.slate700, fontSize = auraTokenSp(7f), fontWeight = FontWeight.Black)
    }
}

@Composable
fun EmptyText(text: String) {
    Text(text, color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(13f), lineHeight = auraTokenSp(18f))
}

@Composable
fun InlineError(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(auraTokenDp(16f)))
            .background(auraHex(0xFFFEE2E2).copy(alpha = auraTokenAlpha(0.82f)))
            .padding(auraTokenDp(12f)),
        color = MaterialTheme.aura.journal.errorRed,
        fontSize = auraTokenSp(13f),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun auraTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.86f)),
    unfocusedBorderColor = Color.White.copy(alpha = auraTokenAlpha(0.72f)),
    focusedLabelColor = MaterialTheme.aura.journal.slate700,
    unfocusedLabelColor = MaterialTheme.aura.journal.slate500,
    cursorColor = MaterialTheme.aura.journal.neutral,
    focusedTextColor = MaterialTheme.aura.journal.slate800,
    unfocusedTextColor = MaterialTheme.aura.journal.slate800,
    focusedContainerColor = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
    unfocusedContainerColor = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
)
