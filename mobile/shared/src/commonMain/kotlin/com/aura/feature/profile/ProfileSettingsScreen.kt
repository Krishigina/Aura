package com.aura.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.data.api.AuraApiClient
import com.aura.core.data.repository.TokenManager
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.core.domain.model.ProfileRoutineUpdateRequest
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ReminderPreference
import com.aura.core.domain.model.ReminderFrequency
import com.aura.core.ui.components.GlassSurface
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class SettingsTab { ROOT, NAME, LOGIN, PASSWORD, ROUTINE, NOTIFICATIONS, DELETE }

@Composable
fun ProfileSettingsScreen(apiClient: AuraApiClient, onBack: () -> Unit, onAccountDeleted: () -> Unit) {
    val uiScope = rememberCoroutineScope()
    val user = TokenManager.getUser()
    var tab by remember { mutableStateOf(SettingsTab.ROOT) }

    var name by remember { mutableStateOf(user?.name ?: "") }
    var nickname by remember { mutableStateOf(user?.nickname ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var routineSteps by remember { mutableStateOf<List<ProfileRoutineStep>>(emptyList()) }
    var routineLoading by remember { mutableStateOf(false) }
    var routineSaving by remember { mutableStateOf(false) }
    var routineDirty by remember { mutableStateOf(false) }
    var notificationSettings by remember { mutableStateOf(ProfileNotificationSettings()) }
    var notificationSaving by remember { mutableStateOf(false) }

    fun loadRoutine() {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            error = "Сессия истекла, войдите снова"
            return
        }
        routineLoading = true
        uiScope.launch {
            runCatching {
                apiClient.getProfileRoutine(token)
            }.onSuccess { response ->
                routineSteps = normalizeRoutineStepOrder(response.steps)
                routineDirty = false
            }.onFailure {
                error = it.message ?: "Не удалось загрузить рутину"
            }
            routineLoading = false
        }
    }

    fun loadNotificationSettings() {
        val token = TokenManager.getToken()
        if (token.isNullOrBlank()) {
            error = "Сессия истекла, войдите снова"
            return
        }
        uiScope.launch {
            runCatching {
                apiClient.getProfileNotificationSettings(token)
            }.onSuccess { response ->
                notificationSettings = response
            }.onFailure {
                error = it.message ?: "Не удалось загрузить настройки уведомлений"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRoutine()
        loadNotificationSettings()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7FE))) {
        SoftPastelBackground(dark = false, variant = SoftPastelVariant.Default)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 112.dp, bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                success?.let { Text(it, color = Color(0xFF059669), fontSize = 13.sp) }
                error?.let { Text(it, color = Color(0xFFDC2626), fontSize = 13.sp) }

                when (tab) {
                    SettingsTab.ROOT -> {
                        SettingsEntryCard("Имя", name.ifBlank { "Не указано" }, Icons.Rounded.Person) { tab = SettingsTab.NAME }
                        SettingsEntryCard("Логин", nickname.ifBlank { "Не указан" }, Icons.Rounded.Person) { tab = SettingsTab.LOGIN }
                        SettingsEntryCard("Пароль", "Изменить пароль", Icons.Rounded.Lock) { tab = SettingsTab.PASSWORD }
                        SettingsEntryCard(
                            "Моя рутина",
                            if (routineSteps.isEmpty()) "Не настроена" else "${routineSteps.size} шаг(ов)",
                            Icons.Rounded.Person
                        ) {
                            tab = SettingsTab.ROUTINE
                            error = null
                            success = null
                            if (!routineDirty) {
                                loadRoutine()
                            }
                        }
                        SettingsEntryCard("Уведомления", "Рутина и журнал", Icons.Rounded.Lock) {
                            tab = SettingsTab.NOTIFICATIONS
                            error = null
                            success = null
                        }
                        DangerEntryCard("Удалить аккаунт", "Это действие необратимо", Icons.Rounded.Delete) { tab = SettingsTab.DELETE }
                    }

                    SettingsTab.NAME -> {
                        GlassField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Имя",
                            placeholder = "Ваше имя"
                        )
                        SaveButton(isSaving = isSaving) {
                            saveAccount(apiClient, name, nickname, onError = { error = it }, onSuccess = {
                                success = "Имя обновлено"
                            }, onSaving = { isSaving = it }, scope = uiScope)
                        }
                    }

                    SettingsTab.LOGIN -> {
                        GlassField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            label = "Логин",
                            placeholder = "@username"
                        )
                        SaveButton(isSaving = isSaving) {
                            saveAccount(apiClient, name, nickname, onError = { error = it }, onSuccess = {
                                success = "Логин обновлен"
                            }, onSaving = { isSaving = it }, scope = uiScope)
                        }
                    }

                    SettingsTab.PASSWORD -> {
                        GlassPasswordField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = "Текущий пароль",
                            placeholder = "Введите текущий пароль"
                        )
                        GlassPasswordField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "Новый пароль",
                            placeholder = "Введите новый пароль"
                        )
                        GlassPasswordField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Повторите пароль",
                            placeholder = "Повторите новый пароль"
                        )
                        SaveButton(isSaving = isSaving) {
                            savePassword(
                                apiClient = apiClient,
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword,
                                onError = { error = it },
                                onSuccess = {
                                    success = "Пароль обновлен"
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                },
                                onSaving = { isSaving = it },
                                scope = uiScope
                            )
                        }
                    }

                    SettingsTab.ROUTINE -> {
                        if (routineLoading) {
                            Text("Загрузка рутины...", color = Color(0xFF475569), fontSize = 13.sp)
                        } else {
                            routineSteps.forEachIndexed { index, step ->
                                RoutineStepEditor(
                                    step = step,
                                    index = index,
                                    total = routineSteps.size,
                                    onProductLabelChange = { value ->
                                        routineSteps = routineSteps.toMutableList().also {
                                            it[index] = it[index].copy(product_label = value)
                                        }
                                        routineDirty = true
                                    },
                                    onOrderChange = { value ->
                                        val parsed = value.toIntOrNull() ?: step.order
                                        routineSteps = routineSteps.toMutableList().also {
                                            it[index] = it[index].copy(order = parsed)
                                        }
                                        routineDirty = true
                                    },
                                    onFrequencyChange = { value ->
                                        routineSteps = routineSteps.toMutableList().also {
                                            it[index] = it[index].copy(
                                                frequency = value,
                                                reminder_time = if (value == ReminderFrequency.NONE) null else it[index].reminder_time
                                            )
                                        }
                                        routineDirty = true
                                    },
                                    onReminderTimeChange = { value ->
                                        routineSteps = routineSteps.toMutableList().also {
                                            it[index] = it[index].copy(reminder_time = value.ifBlank { null })
                                        }
                                        routineDirty = true
                                    },
                                    onMoveUp = {
                                        if (index == 0) return@RoutineStepEditor
                                        val updated = routineSteps.toMutableList()
                                        val current = updated[index]
                                        updated[index] = updated[index - 1]
                                        updated[index - 1] = current
                                        routineSteps = normalizeRoutineStepOrder(updated)
                                        routineDirty = true
                                    },
                                    onMoveDown = {
                                        if (index >= routineSteps.lastIndex) return@RoutineStepEditor
                                        val updated = routineSteps.toMutableList()
                                        val current = updated[index]
                                        updated[index] = updated[index + 1]
                                        updated[index + 1] = current
                                        routineSteps = normalizeRoutineStepOrder(updated)
                                        routineDirty = true
                                    },
                                    onRemove = {
                                        routineSteps = normalizeRoutineStepOrder(
                                            routineSteps.toMutableList().also { it.removeAt(index) }
                                        )
                                        routineDirty = true
                                    }
                                )
                            }

                            Button(
                                onClick = {
                                    routineSteps = normalizeRoutineStepOrder(
                                        routineSteps + ProfileRoutineStep(
                                            id = "",
                                            product_label = "",
                                            order = routineSteps.size + 1,
                                            frequency = ReminderFrequency.NONE,
                                            reminder_time = null
                                        )
                                    )
                                    routineDirty = true
                                },
                                enabled = !routineSaving,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF334155))
                            ) {
                                Text("Добавить шаг", fontWeight = FontWeight.SemiBold)
                            }

                            SaveButton(isSaving = routineSaving) {
                                error = null
                                success = null
                                val token = TokenManager.getToken()
                                if (token.isNullOrBlank()) {
                                    error = "Сессия истекла, войдите снова"
                                    return@SaveButton
                                }
                                val normalized = normalizeRoutineStepOrder(routineSteps)
                                for (step in normalized) {
                                    val validationError = validateRoutineStep(step)
                                    if (validationError != null) {
                                        error = validationError
                                        return@SaveButton
                                    }
                                }
                                routineSaving = true
                                uiScope.launch {
                                    runCatching {
                                        apiClient.saveProfileRoutine(token, ProfileRoutineUpdateRequest(normalized))
                                    }.onSuccess { response ->
                                        routineSteps = normalizeRoutineStepOrder(response.steps)
                                        success = "Рутина сохранена"
                                        routineDirty = false
                                    }.onFailure {
                                        error = it.message ?: "Не удалось сохранить рутину"
                                    }
                                    routineSaving = false
                                }
                            }
                        }
                    }

                    SettingsTab.NOTIFICATIONS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Отключить все уведомления", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.55f))
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Все уведомления", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(if (notificationSettings.disable_all) "Отключены" else "Включены", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                                Switch(
                                    checked = notificationSettings.disable_all,
                                    onCheckedChange = { notificationSettings = notificationSettings.copy(disable_all = it) }
                                )
                            }
                        }

                        NotificationDomainEditor(
                            title = "Рутина",
                            preference = notificationSettings.routine,
                            enabled = !notificationSettings.disable_all,
                            onFrequencyChange = { value ->
                                notificationSettings = notificationSettings.copy(
                                    routine = notificationSettings.routine.copy(
                                        frequency = value,
                                        reminder_time = if (value == ReminderFrequency.NONE) null else notificationSettings.routine.reminder_time
                                    )
                                )
                            },
                            onReminderTimeChange = { value ->
                                notificationSettings = notificationSettings.copy(
                                    routine = notificationSettings.routine.copy(reminder_time = value.ifBlank { null })
                                )
                            }
                        )

                        NotificationDomainEditor(
                            title = "Журнал",
                            preference = notificationSettings.journal,
                            enabled = !notificationSettings.disable_all,
                            onFrequencyChange = { value ->
                                notificationSettings = notificationSettings.copy(
                                    journal = notificationSettings.journal.copy(
                                        frequency = value,
                                        reminder_time = if (value == ReminderFrequency.NONE) null else notificationSettings.journal.reminder_time
                                    )
                                )
                            },
                            onReminderTimeChange = { value ->
                                notificationSettings = notificationSettings.copy(
                                    journal = notificationSettings.journal.copy(reminder_time = value.ifBlank { null })
                                )
                            }
                        )

                        SaveButton(isSaving = notificationSaving) {
                            error = null
                            success = null
                            val token = TokenManager.getToken()
                            if (token.isNullOrBlank()) {
                                error = "Сессия истекла, войдите снова"
                                return@SaveButton
                            }
                            val validationError = validateNotificationSettingsBeforeSave(notificationSettings, routineSteps.size)
                            if (validationError != null) {
                                error = validationError
                                return@SaveButton
                            }
                            notificationSaving = true
                            uiScope.launch {
                                runCatching {
                                    apiClient.saveProfileNotificationSettings(token, notificationSettings)
                                }.onSuccess { response ->
                                    notificationSettings = response
                                    success = "Настройки уведомлений сохранены"
                                }.onFailure {
                                    error = it.message ?: "Не удалось сохранить настройки уведомлений"
                                }
                                notificationSaving = false
                            }
                        }
                    }

                    SettingsTab.DELETE -> {
                        Text(
                            "Вы уверены, что хотите удалить аккаунт? Это действие необратимо.",
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp
                        )
                        GlassPasswordField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it },
                            label = "Текущий пароль",
                            placeholder = "Введите текущий пароль"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    tab = SettingsTab.ROOT
                                    error = null
                                },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF475569))
                            ) {
                                Text("Отмена", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    error = null
                                    success = null
                                    val token = TokenManager.getToken()
                                    if (token.isNullOrBlank()) {
                                        error = "Сессия истекла, войдите снова"
                                        return@Button
                                    }
                                    if (deletePassword.isBlank()) {
                                        error = "Введите текущий пароль"
                                        return@Button
                                    }
                                    isSaving = true
                                    uiScope.launch {
                                        runCatching {
                                            apiClient.deleteProfileAccount(token, deletePassword)
                                        }.onSuccess {
                                            TokenManager.clearToken()
                                            onAccountDeleted()
                                        }.onFailure {
                                            error = it.message ?: "Не удалось удалить аккаунт"
                                        }
                                        isSaving = false
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                            ) {
                                Text(if (isSaving) "Удаление..." else "Удалить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        SettingsHeader(
            title = if (tab == SettingsTab.ROOT) "Настройки" else "Изменение",
            onBack = {
                if (tab == SettingsTab.ROOT) onBack() else {
                    tab = SettingsTab.ROOT
                    error = null
                    success = null
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun RoutineStepEditor(
    step: ProfileRoutineStep,
    index: Int,
    total: Int,
    onProductLabelChange: (String) -> Unit,
    onOrderChange: (String) -> Unit,
    onFrequencyChange: (ReminderFrequency) -> Unit,
    onReminderTimeChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Шаг ${index + 1}", color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        GlassField(
            value = step.product_label,
            onValueChange = onProductLabelChange,
            label = "Продукт",
            placeholder = "Например, очищающий гель"
        )
        GlassField(
            value = step.order.toString(),
            onValueChange = onOrderChange,
            label = "Порядок",
            placeholder = "1",
            keyboardType = KeyboardType.Number
        )
        Text("Частота", color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FrequencyButton("Никогда", step.frequency == ReminderFrequency.NONE) { onFrequencyChange(ReminderFrequency.NONE) }
            FrequencyButton("Ежедневно", step.frequency == ReminderFrequency.DAILY) { onFrequencyChange(ReminderFrequency.DAILY) }
            FrequencyButton("Еженедельно", step.frequency == ReminderFrequency.WEEKLY) { onFrequencyChange(ReminderFrequency.WEEKLY) }
            FrequencyButton("Ежемесячно", step.frequency == ReminderFrequency.MONTHLY) { onFrequencyChange(ReminderFrequency.MONTHLY) }
        }
        GlassField(
            value = step.reminder_time.orEmpty(),
            onValueChange = onReminderTimeChange,
            label = "Время напоминания",
            placeholder = if (step.frequency == ReminderFrequency.NONE) "Можно оставить пустым" else "Например, 08:30"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Вверх", tint = Color(0xFF64748B))
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Вниз", tint = Color(0xFF64748B))
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = "Удалить шаг", tint = Color(0xFFDC2626))
            }
        }
    }
}

@Composable
private fun RowScope.FrequencyButton(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF0EA5E9) else Color.White,
            contentColor = if (selected) Color.White else Color(0xFF475569)
        )
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NotificationDomainEditor(
    title: String,
    preference: ReminderPreference,
    enabled: Boolean,
    onFrequencyChange: (ReminderFrequency) -> Unit,
    onReminderTimeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("Частота", color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FrequencyButton("Никогда", preference.frequency == ReminderFrequency.NONE, enabled) { onFrequencyChange(ReminderFrequency.NONE) }
            FrequencyButton("Ежедневно", preference.frequency == ReminderFrequency.DAILY, enabled) { onFrequencyChange(ReminderFrequency.DAILY) }
            FrequencyButton("Еженедельно", preference.frequency == ReminderFrequency.WEEKLY, enabled) { onFrequencyChange(ReminderFrequency.WEEKLY) }
            FrequencyButton("Ежемесячно", preference.frequency == ReminderFrequency.MONTHLY, enabled) { onFrequencyChange(ReminderFrequency.MONTHLY) }
        }
        if (preference.frequency != ReminderFrequency.NONE) {
            GlassField(
                value = preference.reminder_time.orEmpty(),
                onValueChange = onReminderTimeChange,
                label = "Время напоминания",
                placeholder = "Например, 08:30",
                enabled = enabled
            )
        } else {
            Text("Напоминание отключено", color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DangerEntryCard(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFDC2626))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Color(0xFF991B1B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = Color(0xFFB91C1C), fontSize = 12.sp)
            }
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFB91C1C))
    }
}

private fun saveAccount(
    apiClient: AuraApiClient,
    name: String,
    nickname: String,
    onError: (String) -> Unit,
    onSuccess: () -> Unit,
    onSaving: (Boolean) -> Unit,
    scope: CoroutineScope
) {
    val token = TokenManager.getToken()
    if (token.isNullOrBlank()) {
        onError("Сессия истекла, войдите снова")
        return
    }
    if (name.trim().isEmpty()) {
        onError("Имя не может быть пустым")
        return
    }
    onSaving(true)
    scope.launch {
        runCatching {
            val updated = apiClient.updateProfileAccount(token, name.trim(), nickname.trim().ifBlank { null })
            TokenManager.setUser(updated)
        }.onSuccess {
            onSuccess()
        }.onFailure {
            onError(it.message ?: "Не удалось сохранить")
        }
        onSaving(false)
    }
}

private fun savePassword(
    apiClient: AuraApiClient,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    onError: (String) -> Unit,
    onSuccess: () -> Unit,
    onSaving: (Boolean) -> Unit,
    scope: CoroutineScope
) {
    val token = TokenManager.getToken()
    if (token.isNullOrBlank()) {
        onError("Сессия истекла, войдите снова")
        return
    }
    val validationError = validatePasswordChange(currentPassword, newPassword, confirmPassword)
    if (validationError != null) {
        onError(validationError)
        return
    }
    onSaving(true)
    scope.launch {
        runCatching {
            apiClient.updateProfilePassword(token, currentPassword, newPassword)
        }.onSuccess {
            onSuccess()
        }.onFailure {
            onError(it.message ?: "Не удалось обновить пароль")
        }
        onSaving(false)
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = Color(0xFF64748B))
            }
            Text(title, color = Color(0xFF1E293B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun SettingsEntryCard(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF64748B))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF64748B))
    }
}

@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 15.sp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(Color(0xFF0EA5E9)),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.6f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) Text(placeholder, color = Color(0xFF94A3B8), fontSize = 14.sp)
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    var visible by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 15.sp),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            cursorBrush = SolidColor(Color(0xFF0EA5E9)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.6f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(placeholder, color = Color(0xFF94A3B8), fontSize = 14.sp)
                        innerTextField()
                    }
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp).clickable { visible = !visible }
                    )
                }
            }
        )
    }
}

@Composable
private fun SaveButton(isSaving: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF0284C7)),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9), contentColor = Color.White)
    ) {
        Text(if (isSaving) "Сохранение..." else "Сохранить", fontWeight = FontWeight.Bold)
    }
}
