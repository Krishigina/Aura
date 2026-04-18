package com.aura.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun auraToolbarContentTopPadding(extra: Dp = 156.dp): Dp {
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + extra
}

@Composable
fun auraToolbarTopOffset(extra: Dp = 58.dp): Dp {
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + extra
}

@Composable
fun AuraTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = Color(0xFF1E293B),
    iconTint: Color = Color(0xFF64748B),
    centerContent: (@Composable () -> Unit)? = null,
    rightContent: @Composable () -> Unit = { Spacer(modifier = Modifier.size(40.dp)) },
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = auraToolbarTopOffset())
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = iconTint)
            }
            if (centerContent != null) {
                centerContent()
            } else {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
            }
            rightContent()
        }
    }
}
