package com.aura.feature.chat

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.theme.aura
import com.aura.feature.chat.presentation.components.ChatArea
import com.aura.feature.chat.presentation.components.ChatAttachmentChip
import com.aura.feature.chat.presentation.components.ChatHeader
import com.aura.feature.chat.presentation.components.ChatInput
import com.aura.feature.chat.presentation.components.ChatInputDock
import com.aura.feature.chat.domain.model.ChatAttachmentPayload
import com.aura.feature.chat.presentation.ChatNavigationState
import com.aura.feature.chat.presentation.ChatViewModel
import com.aura.feature.chat.presentation.logic.chatInputBottomPadding
import org.koin.compose.koinInject

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    initialSessionId: Int? = null,
    onNavigateToSessions: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = koinInject(),
    navigationState: ChatNavigationState = koinInject(),
) {
    val rememberedSessionId = initialSessionId ?: navigationState.activeSessionId
    val productContextRequestKey = navigationState.productContextRequestKey
    val initialProductContext = remember(productContextRequestKey) { navigationState.consumeProductContext() }
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val chatTokens = MaterialTheme.aura.chat
    var rootHeightPx by remember { mutableIntStateOf(0) }
    var dockTopPx by remember { mutableIntStateOf(0) }
    val dynamicBottomInset = with(density) {
        val occupiedBottomPx = (rootHeightPx - dockTopPx).coerceAtLeast(0)
        maxOf(chatTokens.chatAreaBottomPadding.roundToPx(), occupiedBottomPx).toDp()
    }

    DisposableEffect(viewModel) {
        ChatAttachmentPickerBridge.onPicked = { picked ->
            viewModel.uploadAttachmentChip(
                ChatAttachmentPayload(
                    filename = picked.filename,
                    contentType = picked.contentType,
                    bytes = picked.bytes,
                ),
            )
        }
        onDispose {
            if (ChatAttachmentPickerBridge.onPicked != null) {
                ChatAttachmentPickerBridge.onPicked = null
            }
        }
    }

    LaunchedEffect(rememberedSessionId, productContextRequestKey) {
        viewModel.load(
            sessionId = rememberedSessionId,
            initialProductContext = initialProductContext,
            requestKey = productContextRequestKey,
        )
    }

    LaunchedEffect(uiState.activeSessionId) {
        navigationState.setActiveSession(uiState.activeSessionId)
    }

    val lastMessageLength = uiState.messages.lastOrNull()?.text?.length ?: 0
    val leadingItemsCount = 1 + if (uiState.productContextActive) 1 else 0
    val lastContentIndex = when {
        uiState.isLoading && uiState.messages.isNotEmpty() -> leadingItemsCount + uiState.messages.size
        uiState.messages.isNotEmpty() -> leadingItemsCount + uiState.messages.lastIndex
        else -> leadingItemsCount
    }

    LaunchedEffect(uiState.messages.size, uiState.isLoading, uiState.productContextActive) {
        listState.scrollToChatBottom(lastContentIndex, animated = true)
    }

    LaunchedEffect(lastMessageLength, uiState.isResponding, uiState.productContextActive) {
        if (uiState.isResponding && uiState.messages.isNotEmpty()) {
            listState.scrollToChatBottom(leadingItemsCount + uiState.messages.lastIndex, animated = false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rootHeightPx = it.height },
    ) {
        SoftPastelBackground(variant = SoftPastelVariant.Chat)

        ChatArea(
            messages = uiState.messages,
            listState = listState,
            isLoading = uiState.isLoading,
            productContextActive = uiState.productContextActive,
            bottomInset = dynamicBottomInset + 8.dp,
            modifier = Modifier.fillMaxSize(),
        )
        ChatHeader(
            onNavigateToSessions = onNavigateToSessions,
            onBack = onBack,
            showBackButton = uiState.productContextActive,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        ChatInputDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = chatTokens.screenHorizontalPadding)
                .imePadding()
                .padding(bottom = chatInputBottomPadding(keyboardVisible))
                .onGloballyPositioned { coordinates ->
                    dockTopPx = coordinates.boundsInRoot().top.toInt()
                },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(chatTokens.attachmentRowGap)) {
                if (uiState.attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(chatTokens.attachmentRowGap),
                    ) {
                        uiState.attachments.forEach { attachment ->
                            ChatAttachmentChip(attachment = attachment)
                        }
                    }
                }

                ChatInput(
                    value = uiState.draftMessage,
                    onValueChange = viewModel::updateDraftMessage,
                    isSending = uiState.isLoading || uiState.isResponding,
                    onAttachClick = {
                        ChatAttachmentPickerBridge.openPicker()
                    },
                    onSend = viewModel::sendDraftMessage,
                )
            }
        }
    }
}

private suspend fun LazyListState.scrollToChatBottom(targetIndex: Int, animated: Boolean) {
    if (targetIndex < 0) return

    if (animated) {
        animateScrollToItem(targetIndex)
    } else {
        scrollToItem(targetIndex)
    }

    val targetItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == targetIndex } ?: return
    val desiredBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val overflow = (targetItem.offset + targetItem.size) - desiredBottom
    if (overflow > 0) {
        scrollBy(overflow.toFloat())
    }
}
