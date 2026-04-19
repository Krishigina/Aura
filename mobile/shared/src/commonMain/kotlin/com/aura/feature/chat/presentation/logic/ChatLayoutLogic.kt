package com.aura.feature.chat.presentation.logic

import androidx.compose.ui.unit.Dp
import com.aura.core.ui.theme.defaultAuraDesignTokens

fun chatInputBottomPadding(keyboardVisible: Boolean) =
    defaultAuraDesignTokens().chat.let { chat ->
        if (keyboardVisible) chat.inputBottomPaddingVisible else chat.inputBottomPaddingHidden
    }

fun chatHeaderCenteringSpacerWidth(optionsButtonWidth: Dp) = optionsButtonWidth
