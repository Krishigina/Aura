package com.aura.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.ui.theme.*

data class RoutineItem(val title: String, val time: String, val isCompleted: Boolean = false, val icon: String = "✨")
data class SkinMetric(val label: String, val value: String, val color: Color, val icon: ImageVector)
data class TabItem(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val tabs = listOf(
        TabItem("Главная", Icons.Filled.Home, Icons.Outlined.Home),
        TabItem("Анализ", Icons.Filled.Analytics, Icons.Outlined.Analytics),
        TabItem("Для вас", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        TabItem("Чат", Icons.Filled.Chat, Icons.Outlined.Chat),
        TabItem("Профиль", Icons.Filled.Person, Icons.Outlined.Person)
    )
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ModernBottomNav(selectedTab = selectedTab, tabs = tabs, onTabSelected = { 
                selectedTab = it
                when (it) {
                    1 -> onNavigateToDiagnostics()
                    3 -> onNavigateToChat()
                    4 -> onNavigateToProfile()
                }
            })
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeTabContent(Modifier.padding(padding))
            1 -> AnalysisTabContent(Modifier.padding(padding))
            2 -> RecommendationsTabContent(Modifier.padding(padding))
            3 -> ChatTabContent(Modifier.padding(padding))
            4 -> ProfileTabContent(Modifier.padding(padding))
        }
    }
}

@Composable
private fun ModernBottomNav(selectedTab: Int, tabs: List<TabItem>, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .shadow(8.dp, RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val scale by animateFloatAsState(if (selected) 1.1f else 1f, label = "scale")
                
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) MintGreen.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (selected) MintGreen else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatTabContent(modifier: Modifier = Modifier) {
    com.aura.feature.chat.ChatScreenContent(modifier = modifier)
}

@Composable
private fun ProfileTabContent(modifier: Modifier = Modifier) {
    com.aura.feature.profile.ProfileScreenContent(modifier = modifier)
}

@Composable
private fun HomeTabContent(modifier: Modifier = Modifier) {
    val userName = "Елена"
    val focusTitle = "Увлажнение и SPF"
    val focusReason = "Ваш кожный барьер восстанавливается"
    val progressPercent = 0.75f
    
    val skinMetrics = listOf(
        SkinMetric("Влага", "72%", MintGreen, Icons.Outlined.WaterDrop),
        SkinMetric("Жирность", "35%", PinkAccent, Icons.Outlined.OilBarrel),
        SkinMetric("Барьер", "68%", Lavender, Icons.Outlined.Shield)
    )
    
    val morningRoutine = listOf(
        RoutineItem("Очищение", "07:00", true, "🧴"),
        RoutineItem("Тоник", "07:05", true, "💧"),
        RoutineItem("Сыворотка C", "07:10", false, "✨"),
        RoutineItem("Увлажнение", "07:15", false, "🌸"),
        RoutineItem("SPF 50+", "07:20", false, "☀️")
    )
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AnimatedBackground()
                Column(modifier = Modifier.padding(20.dp)) {
                    GreetingSection(userName)
                    Spacer(modifier = Modifier.height(24.dp))
                    FocusCard(title = focusTitle, reason = focusReason)
                    Spacer(modifier = Modifier.height(24.dp))
                    ProgressSection(progress = progressPercent, metrics = skinMetrics)
                }
            }
        }
        
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Утренняя рутина", "Сегодня 5 шагов")
                Spacer(modifier = Modifier.height(16.dp))
                morningRoutine.forEach { item ->
                    ModernRoutineItem(item)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "offset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-50 + offset * 30).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MintGreen.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (offset * 50).dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PinkAccent.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MintGreen,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GreetingSection(userName: String) {
    Column {
        Spacer(modifier = Modifier.height(60.dp))
        Text(
            text = "Доброе утро 👋",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Как ваша кожа сегодня?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun FocusCard(title: String, reason: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MintGreen.copy(alpha = 0.9f),
                            MintGreen.copy(alpha = 0.6f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Фокус дня",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(progress: Float, metrics: List<SkinMetric>) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val animatedValue by animateFloatAsState(targetValue = animatedProgress, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "progress")
    
    LaunchedEffect(Unit) { animatedProgress = progress }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Состояние кожи",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MintGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Обновлено сегодня",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                    CircularProgressIndicator(
                        progress = animatedValue,
                        modifier = Modifier.size(110.dp),
                        strokeWidth = 10.dp,
                        trackColor = Color.Gray.copy(alpha = 0.1f),
                        color = MintGreen
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedValue * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MintGreen
                        )
                        Text(
                            text = "здоровье",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    metrics.forEach { metric ->
                        MetricItem(metric)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(metric: SkinMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(metric.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = metric.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Text(
            text = metric.value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = metric.color
        )
    }
}

@Composable
private fun ModernRoutineItem(item: RoutineItem) {
    val scale by animateFloatAsState(
        targetValue = if (item.isCompleted) 1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (item.isCompleted) 
                    MaterialTheme.colorScheme.surface 
                else 
                    MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (item.isCompleted) MintGreen.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (item.isCompleted) MintGreen.copy(alpha = 0.15f)
                            else Lavender.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.icon, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isCompleted) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isCompleted) MintGreen else Color.Gray.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.Check else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (item.isCompleted) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AnalysisTabContent(modifier: Modifier = Modifier) {
    val skinMetrics = listOf(
        SkinMetric("Влага", "72%", MintGreen, Icons.Outlined.WaterDrop),
        SkinMetric("Жирность", "35%", PinkAccent, Icons.Outlined.OilBarrel),
        SkinMetric("Барьер", "68%", Lavender, Icons.Outlined.Shield),
        SkinMetric("pH", "5.5", Color(0xFFFFAB40), Icons.Outlined.Science)
    )
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Анализ кожи",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Детальный анализ состояния вашей кожи",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MintGreen.copy(alpha = 0.15f), Lavender.copy(alpha = 0.1f))
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = 0.72f,
                            modifier = Modifier.size(180.dp),
                            strokeWidth = 16.dp,
                            trackColor = Color.Gray.copy(alpha = 0.1f),
                            color = MintGreen
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("72%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MintGreen)
                            Text("Общее здоровье", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                skinMetrics.take(2).forEach { metric ->
                    MetricCard(metric, Modifier.weight(1f))
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                skinMetrics.drop(2).forEach { metric ->
                    MetricCard(metric, Modifier.weight(1f))
                }
            }
        }
        
        item {
            Text(
                text = "Рекомендации",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        items(3) { index ->
            RecommendationCard(index)
        }
    }
}

@Composable
private fun MetricCard(metric: SkinMetric, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(metric.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = metric.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = metric.color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun RecommendationCard(index: Int) {
    val recommendations = listOf(
        Triple("Увеличить увлажнение", "Добавьте сыворотку с гиалуроновой кислотой", MintGreen),
        Triple("Защита от солнца", "Используйте SPF 50 каждый день", PinkAccent),
        Triple("Восстановление барьера", "Нанесите ceramide крем на ночь", Lavender)
    )
    val (title, desc, color) = recommendations[index]
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun RecommendationsTabContent(modifier: Modifier = Modifier) {
    val products = listOf(
        Triple("Hydrating Serum", "Увлажняющая сыворотка", 98f),
        Triple("Vitamin C Cream", "Антиоксидантный крем", 95f),
        Triple("Barrier Repair", "Восстановление барьера", 92f),
        Triple("SPF 50+ Protection", "Защита от солнца", 99f),
        Triple("Retinol Night", "Ночной ретинол", 88f)
    )
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Рекомендации",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Персонализированные продукты для вас",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MintGreen, MintGreen.copy(alpha = 0.7f))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Индекс совместимости",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "98%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Отлично",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Text(
                text = "Лучшие для вас",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        items(products.size) { index ->
            val (name, desc, match) = products[index]
            val color = when {
                match >= 95 -> MintGreen
                match >= 90 -> PinkAccent
                else -> Lavender
            }
            
            ProductCardModern(name = name, desc = desc, match = match, color = color)
        }
        
        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun ProductCardModern(name: String, desc: String, match: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable { }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧴", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${match.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Text(
                    text = "совм.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
