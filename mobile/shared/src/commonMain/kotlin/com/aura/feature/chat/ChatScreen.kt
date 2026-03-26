package com.aura.feature.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.ui.theme.*

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String = "12:30"
)

@Composable
fun ChatScreen(onBack: () -> Unit = {}) {
    ChatScreenContent(onBack = onBack)
}

@Composable
fun ChatScreenContent(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Привет! Я ваш AI-ассистент по уходу за кожей. Чем могу помочь?", false, "10:00"),
            ChatMessage("У меня сухая кожа, что выбрать?", true, "10:01"),
            ChatMessage("Для сухой кожи рекомендую увлажняющую сыворотку с гиалуроновой кислотой и плотный крем с керамидами. Хотите, чтобы я подобрал продукты?", false, "10:02")
        )
    }
    
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size - 1)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        ChatHeader(onBack)
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                QuickReplies()
            }
            
            items(messages) { message ->
                ChatBubble(message = message)
            }
            
            item {
                TypingIndicator()
            }
        }
        
        ChatInput(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                if (messageText.isNotBlank()) {
                    messages.add(ChatMessage(messageText, true, "12:30"))
                    messageText = ""
                }
            }
        )
    }
}

@Composable
private fun QuickReplies() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Быстрые вопросы",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickReplyChip("Что мне подходит?", Modifier.weight(1f))
            QuickReplyChip("Мой тип кожи?", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        QuickReplyChip("Лучший SPF?", Modifier.fillMaxWidth(0.6f))
    }
}

@Composable
private fun QuickReplyChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MintGreen.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MintGreen,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ChatHeader(onBack: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MintGreen, Lavender)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Ассистент",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MintGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "В сети",
                        style = MaterialTheme.typography.bodySmall,
                        color = MintGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Меню",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MintGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MintGreen)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isFromUser) 20.dp else 6.dp,
                bottomEnd = if (message.isFromUser) 6.dp else 20.dp
            ),
            color = if (message.isFromUser) MintGreen else MaterialTheme.colorScheme.surface,
            shadowElevation = if (message.isFromUser) 4.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isFromUser) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(colors = listOf(Lavender, PinkAccent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Е", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "alpha1")
    val alpha2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400, delayMillis = 150), RepeatMode.Reverse), label = "alpha2")
    val alpha3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400, delayMillis = 300), RepeatMode.Reverse), label = "alpha3")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MintGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MintGreen)
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MintGreen.copy(alpha = alpha1)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MintGreen.copy(alpha = alpha2)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MintGreen.copy(alpha = alpha3)))
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircle,
                        contentDescription = "Добавить",
                        tint = MintGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Камера",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = {
                        Text(
                            "Введите сообщение...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(if (value.isNotBlank()) 6.dp else 0.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank()) {
                                Brush.linearGradient(colors = listOf(MintGreen, MintGreen.copy(alpha = 0.85f)))
                            } else {
                                Brush.linearGradient(colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                ))
                            }
                        )
                        .clickable(enabled = value.isNotBlank()) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (value.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                        contentDescription = if (value.isNotBlank()) "Отправить" else "Микрофон",
                        tint = if (value.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
