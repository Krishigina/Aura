package com.aura.feature.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.theme.*

// Цветовая палитра
private val BgLight = Color(0xFFF6F8F7)
private val PrimaryMint = Color(0xFFA5F3CF)
private val CoolGrey = Color(0xFF4B5563)
private val TextGray900 = Color(0xFF111827)
private val TextGray800 = Color(0xFF1F2937)
private val TextGray700 = Color(0xFF374151)
private val TextGray500 = Color(0xFF6B7280)
private val TextGray400 = Color(0xFF9CA3AF)
private val BlobBlue = Color(0xFFDBEAFE)
private val BlobPurple = Color(0xFFF3E8FF)
private val BlobIndigo = Color(0xFFE0E7FF)

@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Привет! Я заметила, что вы обновили свой утренний уход. Как ваша кожа чувствует себя сегодня?", false, "10:23"),
            ChatMessage("На самом деле, немного суховата. Особенно в области щек.", true, "10:24"),
            ChatMessage("Понятно. Давайте проверим уровень увлажненности. Можете загрузить фото щеки крупным планом?", false, "10:25")
        )
    }
    
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size - 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // 1. Анимированные фоновые пятна (Blobs)
        BackgroundBlobs()

        // 2. Основной контент
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader()
            ChatArea(
                messages = messages,
                listState = listState,
                modifier = Modifier.weight(1f)
            )
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
}

@Composable
private fun BackgroundBlobs() {
    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        val width = size.width
        val height = size.height
        drawCircle(color = BlobBlue.copy(alpha = 0.4f), radius = width * 0.4f, center = Offset(0f, 0f))
        drawCircle(color = BlobPurple.copy(alpha = 0.4f), radius = width * 0.4f, center = Offset(width, 0f))
        drawCircle(color = BlobIndigo.copy(alpha = 0.4f), radius = width * 0.4f, center = Offset(width * 0.2f, height * 0.8f))
    }
}

@Composable
private fun ChatHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.3f))
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Аватарка с онлайн-индикатором
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(listOf(BlobBlue, PrimaryMint.copy(alpha = 0.3f))), CircleShape)
                        .padding(2.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryMint
                    )
                }
                // Зеленая точка онлайн
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF4ADE80), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(text = "Aura AI - Помощник", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextGray900)
                Text(text = "Здоровье кожи", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryMint)
            }
        }

        IconButton(
            onClick = {},
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "Options", tint = TextGray500)
        }
    }
}

@Composable
private fun ChatArea(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Badge "Сегодня"
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(text = "Сегодня", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextGray400)
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
    }
}

@Composable
private fun AiMessage(text: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(0.85f),
        verticalAlignment = Alignment.Bottom
    ) {
        // AI Icon
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(PrimaryMint.copy(alpha = 0.1f), CircleShape)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryMint
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF743DF5).copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp), spotColor = Color(0xFF743DF5).copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Text(text = text, fontSize = 15.sp, color = TextGray700, lineHeight = 22.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = time, fontSize = 10.sp, color = TextGray400, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun AiMessageWithAction(text: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(0.85f),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(PrimaryMint.copy(alpha = 0.1f), CircleShape)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryMint
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF743DF5).copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp), spotColor = Color(0xFF743DF5).copy(alpha = 0.05f))
                    .padding(16.dp)
            ) {
                Text(text = text, fontSize = 15.sp, color = TextGray700, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Button "Открыть камеру"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, PrimaryMint.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = PrimaryMint, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Открыть камеру", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PrimaryMint)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = time, fontSize = 10.sp, color = TextGray400, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun UserMessage(text: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp))
                    .background(CoolGrey)
                    .padding(16.dp)
            ) {
                Text(text = text, fontSize = 15.sp, color = Color.White, lineHeight = 22.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = time, fontSize = 10.sp, color = TextGray400, modifier = Modifier.padding(end = 4.dp))
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
            .padding(start = 12.dp, end = 5.dp, top = 6.dp, bottom = 6.dp)
            .padding(bottom = 120.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attach Button
        Icon(
            imageVector = Icons.Rounded.AttachFile,
            contentDescription = "Attach",
            tint = TextGray400,
            modifier = Modifier
                .rotate(45f)
                .clickable { }
                .padding(8.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Input Field
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = TextGray700, fontSize = 15.sp),
            cursorBrush = SolidColor(PrimaryMint),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = "Спросите о вашей коже...", color = TextGray400, fontSize = 15.sp)
                }
                innerTextField()
            }
        )

        // Send Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(4.dp, CircleShape, spotColor = PrimaryMint.copy(alpha = 0.3f))
                .background(PrimaryMint, CircleShape)
                .clickable(enabled = value.isNotBlank()) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String = "12:30"
)