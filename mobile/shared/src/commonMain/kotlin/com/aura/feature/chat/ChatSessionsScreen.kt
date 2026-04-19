package com.aura.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.aura.core.data.api.model.ChatSessionSummary
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.feature.chat.presentation.ChatSessionsViewModel
import org.koin.compose.koinInject

@Composable
fun ChatSessionsScreen(
    onBack: () -> Unit,
    onSessionClick: (Int) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatSessionsViewModel = koinInject(),
) {
    val chat = MaterialTheme.aura.chat
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(chat.backgroundColor),
    ) {
        SoftPastelBackground(variant = SoftPastelVariant.Chat)

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = chat.primaryMint)
            }

            uiState.errorText != null -> ChatSessionsState(text = uiState.errorText.orEmpty())

            uiState.sessions.isEmpty() -> ChatSessionsState(text = "История чатов пока пуста")

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = chat.sessionsListHorizontalPadding),
                contentPadding = PaddingValues(top = auraToolbarContentTopPadding(), bottom = chat.sessionsListBottomPadding),
                verticalArrangement = Arrangement.spacedBy(chat.sessionsListGap),
            ) {
                items(uiState.sessions, key = { it.id }) { session ->
                    ChatSessionRow(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                    )
                }
            }
        }

        AuraTopBar(
            title = "История чатов",
            onBack = onBack,
            titleColor = chat.textStrong,
            iconTint = chat.textMuted,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        ChatSessionsNewChatButton(
            onNewChat = onNewChat,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ChatSessionsNewChatButton(onNewChat: () -> Unit, modifier: Modifier = Modifier) {
    val chat = MaterialTheme.aura.chat

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = chat.sessionsNewButtonHorizontalPadding)
            .padding(bottom = chat.sessionsNewButtonBottomPadding)
            .clip(RoundedCornerShape(chat.sessionsNewButtonRadius))
            .clickable { onNewChat() },
        color = chat.primaryMint,
        shape = RoundedCornerShape(chat.sessionsNewButtonRadius),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = chat.sessionsNewButtonContentHorizontalPadding, vertical = chat.sessionsNewButtonContentVerticalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = chat.textStrong)
            Text(
                text = "Новый чат",
                modifier = Modifier.padding(start = chat.sessionsNewButtonIconGap),
                fontSize = chat.sessionsNewButtonFontSize,
                fontWeight = FontWeight.SemiBold,
                color = chat.textStrong,
            )
        }
    }
}

@Composable
private fun ChatSessionsState(text: String) {
    val chat = MaterialTheme.aura.chat

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = chat.sessionsStateHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = chat.textMuted, fontSize = chat.sessionsStateFontSize)
    }
}

@Composable
private fun ChatSessionRow(session: ChatSessionSummary, onClick: () -> Unit) {
    val chat = MaterialTheme.aura.chat

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(chat.sessionRowRadius))
            .background(chat.glassSurfaceColor.copy(alpha = chat.sessionRowSurfaceAlpha))
            .border(chat.glassBorderWidth, chat.glassBorderColor.copy(alpha = chat.sessionRowBorderAlpha), RoundedCornerShape(chat.sessionRowRadius))
            .clickable { onClick() }
            .padding(chat.sessionRowPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.title.ifBlank { "Чат #${session.id}" },
                modifier = Modifier.weight(1f),
                fontSize = chat.sessionTitleFontSize,
                fontWeight = FontWeight.SemiBold,
                color = chat.textStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${session.messageCount} сообщ.",
                modifier = Modifier
                    .clip(RoundedCornerShape(chat.sessionCountPillRadius))
                    .background(chat.primaryMint.copy(alpha = chat.sessionCountPillAlpha))
                    .padding(horizontal = chat.sessionCountHorizontalPadding, vertical = chat.sessionCountVerticalPadding),
                fontSize = chat.sessionCountFontSize,
                fontWeight = FontWeight.Medium,
                color = chat.textBody,
            )
        }

        session.lastMessage?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(chat.sessionPreviewTopGap))
            Text(
                text = it,
                fontSize = chat.sessionPreviewFontSize,
                color = chat.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(chat.sessionMetaTopGap))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = chat.textSubtle, modifier = Modifier.size(chat.sessionMetaIconSize))
            Text(
                text = session.updatedAt,
                modifier = Modifier.padding(start = chat.sessionMetaTextGap),
                fontSize = chat.sessionMetaFontSize,
                color = chat.textSubtle,
            )
        }
    }
}
