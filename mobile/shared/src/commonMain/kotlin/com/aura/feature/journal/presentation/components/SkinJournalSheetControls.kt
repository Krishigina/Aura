package com.aura.feature.journal.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraTokenAlpha
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.journal.visibleJournalSearchOptions

@Composable
fun SearchableSelect(
    label: String,
    selectedLabel: String?,
    placeholder: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(options, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) {
            options
        } else {
            options.filter { (value, text) ->
                value.lowercase().contains(normalized) || text.lowercase().contains(normalized)
            }
        }
    }
    val visible = remember(filtered) { visibleJournalSearchOptions(filtered) }

    Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
        Text(label, color = MaterialTheme.aura.journal.slate800, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(auraTokenDp(18f)))
                .clickable { expanded = !expanded }
                .semantics {
                    role = Role.Button
                    contentDescription = "$label: ${selectedLabel ?: placeholder}"
                },
            color = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
            border = BorderStroke(auraTokenDp(1f), Color.White.copy(alpha = auraTokenAlpha(0.72f))),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = auraTokenDp(14f), vertical = auraTokenDp(13f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedLabel ?: placeholder,
                    color = if (selectedLabel == null) MaterialTheme.aura.journal.slate500 else MaterialTheme.aura.journal.slate800,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(if (expanded) "⌃" else "⌄", color = MaterialTheme.aura.journal.slate500, fontWeight = FontWeight.Bold)
            }
        }
        if (expanded) {
            GlassInset {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Поиск") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = auraTextFieldColors(),
                )
                if (filtered.isEmpty()) {
                    EmptyText("Ничего не найдено")
                } else {
                    visible.items.forEach { (value, text) ->
                        Text(
                            text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(auraTokenDp(12f)))
                                .clickable {
                                    onSelected(value)
                                    expanded = false
                                    query = ""
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Выбрать $text"
                                }
                                .padding(auraTokenDp(12f)),
                            color = MaterialTheme.aura.journal.slate800,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (visible.hasMore) {
                        Text("Показаны первые 8 результатов, уточните поиск", color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(12f))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchableMultiSelect(
    label: String,
    placeholder: String,
    selectedValues: List<String>,
    options: List<Pair<String, String>>,
    onToggle: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedLabels = options.filter { it.first in selectedValues }.map { it.second }
    val filtered = remember(options, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) {
            options
        } else {
            options.filter { (value, text) ->
                value.lowercase().contains(normalized) || text.lowercase().contains(normalized)
            }
        }
    }
    val visible = remember(filtered) { visibleJournalSearchOptions(filtered) }

    Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
        Text(label, color = MaterialTheme.aura.journal.slate800, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(auraTokenDp(18f)))
                .clickable { expanded = !expanded }
                .semantics {
                    role = Role.Button
                    contentDescription = "$label: ${selectedLabels.joinToString().ifBlank { placeholder }}"
                },
            color = Color.White.copy(alpha = auraTokenAlpha(0.62f)),
            border = BorderStroke(auraTokenDp(1f), Color.White.copy(alpha = auraTokenAlpha(0.72f))),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = auraTokenDp(14f), vertical = auraTokenDp(13f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedLabels.takeIf { it.isNotEmpty() }?.joinToString() ?: placeholder,
                    color = if (selectedLabels.isEmpty()) MaterialTheme.aura.journal.slate500 else MaterialTheme.aura.journal.slate800,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(if (expanded) "⌃" else "⌄", color = MaterialTheme.aura.journal.slate500, fontWeight = FontWeight.Bold)
            }
        }
        if (expanded) {
            GlassInset {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Поиск") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = auraTextFieldColors(),
                )
                if (filtered.isEmpty()) {
                    EmptyText("Ничего не найдено")
                } else {
                    visible.items.forEach { (value, text) ->
                        val selected = value in selectedValues
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(auraTokenDp(12f)))
                                .background(if (selected) MaterialTheme.aura.journal.neutralSoft else Color.Transparent)
                                .clickable { onToggle(value) }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (selected) "Убрать $text" else "Выбрать $text"
                                }
                                .padding(auraTokenDp(12f)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text, color = MaterialTheme.aura.journal.slate800, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (selected) Text("✓", color = MaterialTheme.aura.journal.slate700, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (visible.hasMore) {
                        Text("Показаны первые 8 результатов, уточните поиск", color = MaterialTheme.aura.journal.slate500, fontSize = auraTokenSp(12f))
                    }
                }
            }
        }
    }
}

@Composable
fun DotMetric(label: String, value: Int, onValue: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(auraTokenDp(8f))) {
        Text(label, color = MaterialTheme.aura.journal.slate800, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(auraTokenDp(10f))) {
            (1..5).forEach { index ->
                val selected = index <= value
                Box(
                    modifier = Modifier
                        .size(auraTokenDp(34f))
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.aura.journal.neutral else Color.White.copy(alpha = auraTokenAlpha(0.62f)))
                        .border(auraTokenDp(1f), if (selected) MaterialTheme.aura.journal.neutral else MaterialTheme.aura.journal.neutral.copy(alpha = auraTokenAlpha(0.42f)), CircleShape)
                        .clickable { onValue(index) }
                        .semantics {
                            role = Role.Button
                            contentDescription = "$label: $index из 5"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.aura.journal.slate700, modifier = Modifier.size(auraTokenDp(16f)))
                }
            }
        }
    }
}
