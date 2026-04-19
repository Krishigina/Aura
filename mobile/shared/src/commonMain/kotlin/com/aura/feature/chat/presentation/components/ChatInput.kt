package com.aura.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.aura.core.ui.components.AuraLotusLogo
import com.aura.core.ui.components.GlassSurface
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.AuraChatTokens
import com.aura.core.ui.theme.aura
import com.aura.feature.chat.presentation.logic.ChatMarkdownBlock
import com.aura.feature.chat.presentation.logic.ChatMarkdownSpan
import com.aura.feature.chat.presentation.logic.parseChatMarkdown
import com.aura.feature.chat.presentation.model.ChatAttachmentUi
import com.aura.feature.chat.presentation.model.ChatMessage

@Composable
fun ChatInputDock(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val chat = MaterialTheme.aura.chat

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(chat.dockCornerRadius))
            .background(chat.glassSurfaceColor.copy(alpha = chat.dockSurfaceAlpha))
            .border(
                chat.glassBorderWidth,
                chat.glassBorderColor.copy(alpha = chat.dockBorderAlpha),
                RoundedCornerShape(chat.dockCornerRadius),
            )
            .padding(chat.dockPadding),
    ) {
        content()
    }
}

fun aiMessageShape(chat: AuraChatTokens) = RoundedCornerShape(
    topStart = chat.aiBubbleCornerRadius,
    topEnd = chat.aiBubbleCornerRadius,
    bottomStart = chat.messageTailRadius,
    bottomEnd = chat.aiBubbleCornerRadius,
)

fun userMessageShape(chat: AuraChatTokens) = RoundedCornerShape(
    topStart = chat.aiBubbleCornerRadius,
    topEnd = chat.aiBubbleCornerRadius,
    bottomStart = chat.aiBubbleCornerRadius,
    bottomEnd = chat.messageTailRadius,
)

@Composable
fun ChatArea(
    messages: List<ChatMessage>,
    listState: LazyListState,
    isLoading: Boolean,
    productContextActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val chat = MaterialTheme.aura.chat

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = chat.chatAreaHorizontalPadding),
        contentPadding = PaddingValues(
            top = auraToolbarContentTopPadding(chat.chatAreaTopPaddingBase),
            bottom = chat.chatAreaBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(chat.chatAreaMessageGap),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = chat.dateBadgeTopPadding),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(chat.glassSurfaceColor.copy(alpha = chat.dateBadgeSurfaceAlpha))
                        .padding(horizontal = chat.dateBadgeHorizontalPadding, vertical = chat.dateBadgeVerticalPadding),
                ) {
                    Text(text = "Сегодня", fontSize = chat.dateBadgeFontSize, fontWeight = FontWeight.Medium, color = chat.textSubtle)
                }
            }
        }

        if (productContextActive) {
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ассистент учитывает выбранный продукт, состав и индекс совместимости.",
                        color = chat.textBody,
                        fontSize = chat.contextTextFontSize,
                        lineHeight = chat.contextTextLineHeight,
                        modifier = Modifier.padding(chat.contextTextPadding),
                    )
                }
            }
        }

        items(messages) { message ->
            when {
                message.isFromUser -> UserMessage(text = message.text, time = message.timestamp)
                message.text.contains("Можете загрузить") -> AiMessageWithAction(text = message.text, time = message.timestamp)
                else -> AiMessage(text = message.text, time = message.timestamp)
            }
        }

        if (isLoading && messages.isNotEmpty()) {
            item {
                AiTypingMessage()
            }
        }

        if (messages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = chat.emptyStateTopPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isLoading) "Загружаем историю диалога..." else "История сообщений пока пуста",
                        color = chat.textSubtle,
                        fontSize = chat.emptyStateFontSize,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun AiMessage(text: String, time: String) {
    val chat = MaterialTheme.aura.chat
    val shape = aiMessageShape(chat)

    Row(
        modifier = Modifier.fillMaxWidth(chat.messageMaxWidthFraction),
        verticalAlignment = Alignment.Bottom,
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(chat.messageAvatarGap))

        Column {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(chat.glassSurfaceColor.copy(alpha = chat.messageSurfaceAlpha))
                    .border(chat.glassBorderWidth, chat.accentViolet.copy(alpha = chat.aiBubbleBorderAlpha), shape)
                    .shadow(elevation = chat.messageShadowElevation, shape = shape, spotColor = chat.accentViolet.copy(alpha = chat.aiBubbleShadowAlpha))
                    .padding(chat.messagePadding),
            ) {
                MarkdownText(text = text)
            }
            Spacer(modifier = Modifier.height(chat.messageTimeTopGap))
            Text(text = time, fontSize = chat.messageTimeFontSize, color = chat.textSubtle, modifier = Modifier.padding(start = chat.messageTimeHorizontalPadding))
        }
    }
}

@Composable
fun AiTypingMessage() {
    val chat = MaterialTheme.aura.chat
    val shape = aiMessageShape(chat)

    Row(
        modifier = Modifier.fillMaxWidth(chat.messageMaxWidthFraction),
        verticalAlignment = Alignment.Bottom,
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(chat.messageAvatarGap))

        Row(
            modifier = Modifier
                .clip(shape)
                .background(chat.glassSurfaceColor.copy(alpha = chat.messageSurfaceAlpha))
                .border(chat.glassBorderWidth, chat.accentViolet.copy(alpha = chat.aiBubbleBorderAlpha), shape)
                .padding(horizontal = chat.typingHorizontalPadding, vertical = chat.typingVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(chat.typingIndicatorSize),
                strokeWidth = chat.typingIndicatorStroke,
                color = chat.primaryMint,
            )
            Spacer(modifier = Modifier.width(chat.typingContentGap))
            Text(text = "Aura анализирует ответ...", fontSize = chat.typingTextFontSize, color = chat.textMuted)
        }
    }
}

@Composable
fun MarkdownText(text: String) {
    val chat = MaterialTheme.aura.chat

    Column(verticalArrangement = Arrangement.spacedBy(chat.markdownLineGap)) {
        parseChatMarkdown(text).forEach { block ->
            when (block) {
                ChatMarkdownBlock.Blank -> Spacer(modifier = Modifier.height(chat.markdownBlankLineHeight))
                is ChatMarkdownBlock.Heading -> Text(
                    text = markdownAnnotated(block.spans),
                    fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        else -> 16.sp
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = chat.textStrong,
                    lineHeight = chat.markdownLineHeight,
                )
                is ChatMarkdownBlock.Bullet -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "•", fontSize = chat.markdownTextFontSize, color = chat.textBody, lineHeight = chat.markdownLineHeight)
                        Spacer(modifier = Modifier.width(chat.markdownBulletGap))
                        Text(
                            text = markdownAnnotated(block.spans),
                            fontSize = chat.markdownTextFontSize,
                            color = chat.textBody,
                            lineHeight = chat.markdownLineHeight,
                        )
                    }
                }
                is ChatMarkdownBlock.Numbered -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "${block.number}.", fontSize = chat.markdownTextFontSize, color = chat.textBody, lineHeight = chat.markdownLineHeight)
                        Spacer(modifier = Modifier.width(chat.markdownBulletGap))
                        Text(
                            text = markdownAnnotated(block.spans),
                            fontSize = chat.markdownTextFontSize,
                            color = chat.textBody,
                            lineHeight = chat.markdownLineHeight,
                        )
                    }
                }
                is ChatMarkdownBlock.Paragraph -> Text(
                    text = markdownAnnotated(block.spans),
                    fontSize = chat.markdownTextFontSize,
                    color = chat.textBody,
                    lineHeight = chat.markdownLineHeight,
                )
            }
        }
    }
}

fun markdownAnnotated(spans: List<ChatMarkdownSpan>) = buildAnnotatedString {
    spans.forEach { span ->
        when (span) {
            is ChatMarkdownSpan.Text -> append(span.value)
            is ChatMarkdownSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(span.value)
            }
            is ChatMarkdownSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(span.value)
            }
            is ChatMarkdownSpan.Code -> withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                append(span.value)
            }
        }
    }
}

@Composable
fun AiAvatar() {
    val chat = MaterialTheme.aura.chat

    Box(
        modifier = Modifier.size(chat.avatarSize),
        contentAlignment = Alignment.Center,
    ) {
        AuraLotusLogo(modifier = Modifier.size(chat.avatarSize))
    }
}

@Composable
fun AiMessageWithAction(text: String, time: String) {
    val chat = MaterialTheme.aura.chat
    val shape = aiMessageShape(chat)

    Row(
        modifier = Modifier.fillMaxWidth(chat.messageMaxWidthFraction),
        verticalAlignment = Alignment.Bottom,
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(chat.messageAvatarGap))

        Column {
            Column(
                modifier = Modifier
                    .clip(shape)
                    .background(chat.glassSurfaceColor.copy(alpha = chat.messageSurfaceAlpha))
                    .border(chat.glassBorderWidth, chat.accentViolet.copy(alpha = chat.aiBubbleBorderAlpha), shape)
                    .shadow(elevation = chat.messageShadowElevation, shape = shape, spotColor = chat.accentViolet.copy(alpha = chat.aiBubbleShadowAlpha))
                    .padding(chat.messagePadding),
            ) {
                Text(text = text, fontSize = chat.markdownTextFontSize, color = chat.textBody, lineHeight = chat.markdownLineHeight)
                Spacer(modifier = Modifier.height(chat.actionContentGap))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(chat.actionButtonRadius))
                        .background(chat.glassSurfaceColor)
                        .border(chat.glassBorderWidth, chat.primaryMint.copy(alpha = chat.actionButtonBorderAlpha), RoundedCornerShape(chat.actionButtonRadius))
                        .clickable { }
                        .padding(vertical = chat.actionButtonVerticalPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = chat.primaryMint, modifier = Modifier.size(chat.actionButtonIconSize))
                    Spacer(modifier = Modifier.width(chat.actionButtonGap))
                    Text(text = "Открыть камеру", fontSize = chat.actionButtonTextFontSize, fontWeight = FontWeight.Medium, color = chat.primaryMint)
                }
            }
            Spacer(modifier = Modifier.height(chat.messageTimeTopGap))
            Text(text = time, fontSize = chat.messageTimeFontSize, color = chat.textSubtle, modifier = Modifier.padding(start = chat.messageTimeHorizontalPadding))
        }
    }
}

@Composable
fun UserMessage(text: String, time: String) {
    val chat = MaterialTheme.aura.chat
    val shape = userMessageShape(chat)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(chat.messageMaxWidthFraction),
            horizontalAlignment = Alignment.End,
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(chat.userBubbleColor)
                    .border(chat.glassBorderWidth, chat.primaryMint.copy(alpha = chat.userBubbleBorderAlpha), shape)
                    .padding(chat.messagePadding),
            ) {
                Text(text = text, fontSize = chat.markdownTextFontSize, color = chat.textStrong, lineHeight = chat.markdownLineHeight)
            }
            Spacer(modifier = Modifier.height(chat.messageTimeTopGap))
            Text(text = time, fontSize = chat.messageTimeFontSize, color = chat.textSubtle, modifier = Modifier.padding(end = chat.messageTimeHorizontalPadding))
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onAttachClick: () -> Unit,
    onSend: () -> Unit,
) {
    val chat = MaterialTheme.aura.chat

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(chat.inputShadowElevation, RoundedCornerShape(chat.inputRadius), spotColor = chat.inputShadowColor.copy(alpha = chat.inputShadowAlpha))
            .clip(RoundedCornerShape(chat.inputRadius))
            .background(chat.glassSurfaceColor.copy(alpha = chat.inputSurfaceAlpha))
            .border(chat.glassBorderWidth, chat.glassBorderColor.copy(alpha = chat.inputBorderAlpha), RoundedCornerShape(chat.inputRadius))
            .padding(start = chat.inputStartPadding, end = chat.inputEndPadding, top = chat.inputVerticalPadding, bottom = chat.inputVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AttachFile,
            contentDescription = "Attach",
            tint = chat.textSubtle,
            modifier = Modifier
                .rotate(chat.inputAttachRotation)
                .clickable(enabled = !isSending) { onAttachClick() }
                .padding(chat.inputAttachPadding),
        )

        Spacer(modifier = Modifier.width(chat.inputFieldGap))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = chat.textBody, fontSize = chat.inputTextFontSize),
            cursorBrush = SolidColor(chat.primaryMint),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = "Спросите о вашей коже...", color = chat.textSubtle, fontSize = chat.inputTextFontSize)
                }
                innerTextField()
            },
        )

        Box(
            modifier = Modifier
                .size(chat.sendButtonSize)
                .shadow(chat.sendButtonShadowElevation, CircleShape, spotColor = chat.primaryMint.copy(alpha = chat.sendButtonShadowAlpha))
                .background(chat.primaryMint.copy(alpha = if (isSending) chat.sendButtonDisabledAlpha else chat.sendIconEnabledAlpha), CircleShape)
                .clickable(enabled = value.isNotBlank() && !isSending) { onSend() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = "Send",
                tint = chat.glassSurfaceColor.copy(alpha = if (isSending) chat.sendIconDisabledAlpha else chat.sendIconEnabledAlpha),
                modifier = Modifier.size(chat.sendIconSize),
            )
        }
    }
}

@Composable
fun ChatAttachmentChip(attachment: ChatAttachmentUi) {
    val chat = MaterialTheme.aura.chat

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(chat.attachmentChipRadius))
            .background(chat.primaryMint.copy(alpha = chat.attachmentChipSurfaceAlpha))
            .border(chat.glassBorderWidth, chat.primaryMint.copy(alpha = chat.attachmentChipBorderAlpha), RoundedCornerShape(chat.attachmentChipRadius))
            .padding(horizontal = chat.attachmentChipHorizontalPadding, vertical = chat.attachmentChipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = chat.textMuted, modifier = Modifier.size(chat.attachmentChipIconSize))
        Spacer(modifier = Modifier.width(chat.attachmentChipGap))
        Column {
            Text(text = attachment.filename, color = chat.textStrong, fontSize = chat.attachmentFilenameFontSize, fontWeight = FontWeight.SemiBold)
            Text(text = attachment.status, color = chat.textMuted, fontSize = chat.attachmentStatusFontSize)
        }
    }
}
