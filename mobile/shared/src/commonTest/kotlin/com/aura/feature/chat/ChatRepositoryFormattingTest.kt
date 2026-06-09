package com.aura.feature.chat

import com.aura.feature.chat.data.repository.normalizeAssistantMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatRepositoryFormattingTest {
    @Test
    fun removesEscapedQuotesFromAssistantText() {
        assertEquals(
            "\"Clarifying Lotion Twice A Day Exfoliator\" \u043e\u0442 Clinique",
            normalizeAssistantMessage("\\\"Clarifying Lotion Twice A Day Exfoliator\\\" \u043e\u0442 Clinique"),
        )
    }

    @Test
    fun restoresEscapedLineBreaksFromAssistantText() {
        assertEquals(
            "\u0423\u0442\u0440\u043e\u043c\n\u0412\u0435\u0447\u0435\u0440\u043e\u043c",
            normalizeAssistantMessage("\u0423\u0442\u0440\u043e\u043c\\n\u0412\u0435\u0447\u0435\u0440\u043e\u043c"),
        )
    }
}
