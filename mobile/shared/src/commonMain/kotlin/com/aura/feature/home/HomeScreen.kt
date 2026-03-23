package com.aura.feature.home

import androidx.compose.foundation.layout.*, androidx.compose.material.icons.Icons, androidx.compose.material.icons.filled.*
import androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Главная", "Рекомендации", "Трекер", "Чат")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (selectedTab) {
                0 -> HomeTab()
                1 -> RecommendationsTab()
                2 -> TrackerTab()
                3 -> ChatTab()
            }
        }
    }
}

@Composable private fun HomeTab() {
    Column {
        Text("Добро пожаловать в Aura!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Персонализированный подбор косметики", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable private fun RecommendationsTab() {
    Text("Рекомендации", style = MaterialTheme.typography.headlineMedium)
}

@Composable private fun TrackerTab() {
    Text("Трекер использования", style = MaterialTheme.typography.headlineMedium)
}

@Composable private fun ChatTab() {
    Text("AI Ассистент", style = MaterialTheme.typography.headlineMedium)
}