package com.aura.feature.chat

import com.aura.feature.chat.presentation.logic.ChatMarkdownBlock
import com.aura.feature.chat.presentation.logic.ChatMarkdownSpan
import com.aura.feature.chat.presentation.logic.parseChatMarkdown
import com.aura.feature.chat.presentation.logic.parseChatMarkdownSpans
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMarkdownParserTest {
    @Test
    fun parsesBasicMarkdownBlocksAndInlineSpans() {
        val blocks = parseChatMarkdown(
            """
            ## План ухода
            Текст с **важным** словом, *курсивом* и `кодом`.
            - первый пункт
            1. второй пункт
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                ChatMarkdownBlock.Heading(
                    level = 2,
                    spans = listOf(ChatMarkdownSpan.Text("План ухода")),
                ),
                ChatMarkdownBlock.Paragraph(
                    spans = listOf(
                        ChatMarkdownSpan.Text("Текст с "),
                        ChatMarkdownSpan.Bold("важным"),
                        ChatMarkdownSpan.Text(" словом, "),
                        ChatMarkdownSpan.Italic("курсивом"),
                        ChatMarkdownSpan.Text(" и "),
                        ChatMarkdownSpan.Code("кодом"),
                        ChatMarkdownSpan.Text("."),
                    ),
                ),
                ChatMarkdownBlock.Bullet(
                    spans = listOf(ChatMarkdownSpan.Text("первый пункт")),
                ),
                ChatMarkdownBlock.Numbered(
                    number = 1,
                    spans = listOf(ChatMarkdownSpan.Text("второй пункт")),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun leavesUnclosedBoldMarkerAsText() {
        assertEquals(
            listOf(ChatMarkdownSpan.Text("** вот такое")),
            parseChatMarkdownSpans("** вот такое"),
        )
    }
}
