package com.aura.feature.chat

import androidx.compose.ui.unit.dp
import com.aura.core.ui.theme.ChatHeaderGlowStyle
import com.aura.core.ui.theme.ChatHeaderLogoStyle
import com.aura.core.ui.theme.defaultAuraDesignTokens
import com.aura.feature.chat.presentation.logic.chatHeaderCenteringSpacerWidth
import com.aura.feature.chat.presentation.logic.chatInputBottomPadding
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatLayoutLogicTest {
    @Test
    fun chatInputDoesNotReserveBottomNavigationSpaceWhenKeyboardIsVisible() {
        assertEquals(0.dp, chatInputBottomPadding(keyboardVisible = true))
    }

    @Test
    fun chatInputReservesBottomNavigationSpaceWhenKeyboardIsHidden() {
        assertEquals(116.dp, chatInputBottomPadding(keyboardVisible = false))
    }

    @Test
    fun chatHeaderUsesLeftSpacerMatchingOptionsButtonToCenterLogo() {
        val tokens = defaultAuraDesignTokens()

        assertEquals(tokens.chat.headerOptionsSize, chatHeaderCenteringSpacerWidth(tokens.chat.headerOptionsSize))
    }

    @Test
    fun chatHeaderUsesAppColoredLogoWithoutBackground() {
        assertEquals(ChatHeaderLogoStyle.AppColorMarkOnly, defaultAuraDesignTokens().chat.headerLogoStyle)
    }

    @Test
    fun chatHeaderUsesLargerCenteredLogo() {
        assertEquals(42.dp, defaultAuraDesignTokens().chat.headerLogoSize)
    }

    @Test
    fun chatHeaderUsesAppColoredGlowBehindFrame() {
        assertEquals(ChatHeaderGlowStyle.AppColorFrameGlow, defaultAuraDesignTokens().chat.headerGlowStyle)
    }
}
