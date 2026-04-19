package com.aura.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.aura.core.ui.components.AuraLotusLogo
import com.aura.core.ui.components.auraToolbarTopOffset
import com.aura.core.ui.theme.aura
import com.aura.feature.chat.presentation.logic.chatHeaderCenteringSpacerWidth

@Composable
fun ChatHeader(
    onNavigateToSessions: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.aura
    val chat = tokens.chat

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = auraToolbarTopOffset())
            .padding(horizontal = chat.headerHorizontalPadding, vertical = chat.headerVerticalPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .padding(horizontal = chat.headerGlowHorizontalInset, vertical = chat.headerGlowVerticalInset)
                .shadow(
                    elevation = chat.headerGlowElevation,
                    shape = RoundedCornerShape(chat.headerCornerRadius),
                    ambientColor = tokens.brand.chatHeaderGlowAmbient,
                    spotColor = tokens.brand.chatHeaderGlowSpot,
                )
                .clip(RoundedCornerShape(chat.headerCornerRadius))
                .background(
                    Brush.linearGradient(
                        tokens.brand.chatHeaderGlowGradient,
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(chat.headerCornerRadius))
                .background(chat.glassSurfaceColor.copy(alpha = chat.headerSurfaceAlpha))
                .border(
                    width = chat.glassBorderWidth,
                    color = chat.glassBorderColor.copy(alpha = chat.headerBorderAlpha),
                    shape = RoundedCornerShape(chat.headerCornerRadius),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = chat.headerContentHorizontalPadding,
                        vertical = chat.headerContentVerticalPadding,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(chat.headerOptionsSize),
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = chat.headerOptionsIconColor)
                    }
                } else {
                    Spacer(modifier = Modifier.width(chatHeaderCenteringSpacerWidth(chat.headerOptionsSize)))
                }

                AuraLotusLogo(
                    modifier = Modifier.size(chat.headerLogoSize),
                    colors = tokens.brand.logoGradient,
                    showBackground = false,
                )

                IconButton(
                    onClick = onNavigateToSessions,
                    modifier = Modifier
                        .size(chat.headerOptionsSize)
                        .clip(CircleShape)
                        .background(chat.headerOptionsBackgroundColor),
                ) {
                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "Options", tint = chat.headerOptionsIconColor)
                }
            }
        }
    }
}
