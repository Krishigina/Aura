package com.aura.feature.chat.presentation.logic

sealed class ChatMarkdownBlock {
    data class Heading(val level: Int, val spans: List<ChatMarkdownSpan>) : ChatMarkdownBlock()
    data class Paragraph(val spans: List<ChatMarkdownSpan>) : ChatMarkdownBlock()
    data class Bullet(val spans: List<ChatMarkdownSpan>) : ChatMarkdownBlock()
    data class Numbered(val number: Int, val spans: List<ChatMarkdownSpan>) : ChatMarkdownBlock()
    data object Blank : ChatMarkdownBlock()
}

sealed class ChatMarkdownSpan {
    data class Text(val value: String) : ChatMarkdownSpan()
    data class Bold(val value: String) : ChatMarkdownSpan()
    data class Italic(val value: String) : ChatMarkdownSpan()
    data class Code(val value: String) : ChatMarkdownSpan()
}

fun parseChatMarkdown(text: String): List<ChatMarkdownBlock> = text.lines().map { rawLine ->
    val line = rawLine.trimEnd()
    val trimmed = line.trimStart()
    when {
        line.isBlank() -> ChatMarkdownBlock.Blank
        isHeading(trimmed) -> {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
            ChatMarkdownBlock.Heading(level, parseChatMarkdownSpans(trimmed.drop(level).trimStart()))
        }
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
            ChatMarkdownBlock.Bullet(parseChatMarkdownSpans(trimmed.drop(2)))
        }
        numberedPrefixLength(trimmed) > 0 -> {
            val prefixLength = numberedPrefixLength(trimmed)
            ChatMarkdownBlock.Numbered(
                number = trimmed.take(prefixLength - 2).toIntOrNull() ?: 1,
                spans = parseChatMarkdownSpans(trimmed.drop(prefixLength)),
            )
        }
        else -> ChatMarkdownBlock.Paragraph(parseChatMarkdownSpans(line))
    }
}

fun parseChatMarkdownSpans(text: String): List<ChatMarkdownSpan> {
    val spans = mutableListOf<ChatMarkdownSpan>()
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", startIndex = index + 2)
                if (end < 0) {
                    spans.add(ChatMarkdownSpan.Text(text.substring(index)))
                    break
                }
                spans.add(ChatMarkdownSpan.Bold(text.substring(index + 2, end)))
                index = end + 2
            }
            text[index] == '`' -> {
                val end = text.indexOf('`', startIndex = index + 1)
                if (end < 0) {
                    spans.add(ChatMarkdownSpan.Text(text.substring(index)))
                    break
                }
                spans.add(ChatMarkdownSpan.Code(text.substring(index + 1, end)))
                index = end + 1
            }
            text[index] == '*' -> {
                val end = text.indexOf('*', startIndex = index + 1)
                if (end < 0) {
                    spans.add(ChatMarkdownSpan.Text(text.substring(index)))
                    break
                }
                spans.add(ChatMarkdownSpan.Italic(text.substring(index + 1, end)))
                index = end + 1
            }
            else -> {
                val next = listOf(
                    text.indexOf("**", startIndex = index),
                    text.indexOf('`', startIndex = index),
                    text.indexOf('*', startIndex = index),
                ).filter { it >= 0 }.minOrNull() ?: text.length
                spans.add(ChatMarkdownSpan.Text(text.substring(index, next)))
                index = next
            }
        }
    }
    return spans.mergeAdjacentText()
}

private fun isHeading(trimmed: String): Boolean {
    val level = trimmed.takeWhile { it == '#' }.length
    return level in 1..3 && trimmed.getOrNull(level) == ' '
}

private fun numberedPrefixLength(trimmed: String): Int {
    val digits = trimmed.takeWhile { it.isDigit() }
    if (digits.isEmpty()) return 0
    val dotIndex = digits.length
    return if (trimmed.getOrNull(dotIndex) == '.' && trimmed.getOrNull(dotIndex + 1) == ' ') dotIndex + 2 else 0
}

private fun List<ChatMarkdownSpan>.mergeAdjacentText(): List<ChatMarkdownSpan> {
    val merged = mutableListOf<ChatMarkdownSpan>()
    forEach { span ->
        val previous = merged.lastOrNull()
        if (span is ChatMarkdownSpan.Text && previous is ChatMarkdownSpan.Text) {
            merged[merged.lastIndex] = ChatMarkdownSpan.Text(previous.value + span.value)
        } else {
            merged.add(span)
        }
    }
    return merged
}
