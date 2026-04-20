package com.aura.feature.profile.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.components.auraToolbarContentTopPadding
import com.aura.core.ui.theme.aura
import com.aura.feature.profile.presentation.components.settings.DangerEntryCard
import com.aura.feature.profile.presentation.components.settings.GlassField
import com.aura.feature.profile.presentation.components.settings.GlassPasswordField
import com.aura.feature.profile.presentation.components.settings.SaveButton
import com.aura.feature.profile.presentation.components.settings.SettingsEntryCard
import com.aura.feature.profile.presentation.components.settings.SettingsHeader
import com.aura.feature.profile.presentation.settings.ProfileSettingsTab
import com.aura.feature.profile.presentation.settings.ProfileSettingsViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: ProfileSettingsViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsTokens = MaterialTheme.aura.profileSettings

    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) onAccountDeleted()
    }

    Box(modifier = Modifier.fillMaxSize().background(settingsTokens.backgroundColor)) {
        SoftPastelBackground(dark = false, variant = SoftPastelVariant.Default)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = auraToolbarContentTopPadding(), bottom = settingsTokens.screenBottomPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = settingsTokens.contentHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(settingsTokens.contentGap),
            ) {
                uiState.success?.let { Text(it, color = settingsTokens.successColor, fontSize = settingsTokens.statusFontSize) }
                uiState.error?.let { Text(it, color = settingsTokens.errorColor, fontSize = settingsTokens.statusFontSize) }

                when (uiState.tab) {
                    ProfileSettingsTab.ROOT -> {
                        SettingsEntryCard("Имя", uiState.name.ifBlank { "Не указано" }, Icons.Rounded.Person) { viewModel.setTab(ProfileSettingsTab.NAME) }
                        SettingsEntryCard("Логин", uiState.nickname.ifBlank { "Не указан" }, Icons.Rounded.Person) { viewModel.setTab(ProfileSettingsTab.LOGIN) }
                        SettingsEntryCard("Пароль", "Изменить пароль", Icons.Rounded.Lock) { viewModel.setTab(ProfileSettingsTab.PASSWORD) }
                        DangerEntryCard("Удалить аккаунт", "Это действие необратимо", Icons.Rounded.Delete) { viewModel.setTab(ProfileSettingsTab.DELETE) }
                    }

                    ProfileSettingsTab.NAME -> {
                        GlassField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = "Имя",
                            placeholder = "Ваше имя",
                        )
                        SaveButton(isSaving = uiState.isSaving) {
                            viewModel.saveName()
                        }
                    }

                    ProfileSettingsTab.LOGIN -> {
                        GlassField(
                            value = uiState.nickname,
                            onValueChange = viewModel::updateNickname,
                            label = "Логин",
                            placeholder = "@username",
                        )
                        SaveButton(isSaving = uiState.isSaving) {
                            viewModel.saveLogin()
                        }
                    }

                    ProfileSettingsTab.PASSWORD -> {
                        GlassPasswordField(
                            value = uiState.currentPassword,
                            onValueChange = viewModel::updateCurrentPassword,
                            label = "Текущий пароль",
                            placeholder = "Введите текущий пароль",
                        )
                        GlassPasswordField(
                            value = uiState.newPassword,
                            onValueChange = viewModel::updateNewPassword,
                            label = "Новый пароль",
                            placeholder = "Введите новый пароль",
                        )
                        GlassPasswordField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::updateConfirmPassword,
                            label = "Повторите пароль",
                            placeholder = "Повторите новый пароль",
                        )
                        SaveButton(isSaving = uiState.isSaving) {
                            viewModel.updatePassword()
                        }
                    }

                    ProfileSettingsTab.DELETE -> {
                        Text(
                            "Вы уверены, что хотите удалить аккаунт? Это действие необратимо.",
                            color = settingsTokens.dangerTextColor,
                            fontSize = settingsTokens.statusFontSize,
                        )
                        GlassPasswordField(
                            value = uiState.deletePassword,
                            onValueChange = viewModel::updateDeletePassword,
                            label = "Текущий пароль",
                            placeholder = "Введите текущий пароль",
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(settingsTokens.destructiveActionsGap), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    viewModel.setTab(ProfileSettingsTab.ROOT)
                                },
                                enabled = !uiState.isSaving,
                                modifier = Modifier.weight(1f).height(settingsTokens.buttonHeight),
                                shape = RoundedCornerShape(settingsTokens.buttonRadius),
                                border = BorderStroke(settingsTokens.buttonBorderWidth, settingsTokens.secondaryButtonBorderColor),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = settingsTokens.secondaryButtonContentColor),
                            ) {
                                Text("Отмена", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteAccount()
                                },
                                enabled = !uiState.isSaving,
                                modifier = Modifier.weight(1f).height(settingsTokens.buttonHeight),
                                shape = RoundedCornerShape(settingsTokens.buttonRadius),
                                border = BorderStroke(settingsTokens.buttonBorderWidth, settingsTokens.dangerButtonBorderColor),
                                colors = ButtonDefaults.buttonColors(containerColor = settingsTokens.dangerButtonContainerColor, contentColor = Color.White),
                            ) {
                                Text(if (uiState.isSaving) "Удаление..." else "Удалить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        SettingsHeader(
            title = if (uiState.tab == ProfileSettingsTab.ROOT) "Настройки" else "Изменение",
            onBack = {
                if (uiState.tab == ProfileSettingsTab.ROOT) {
                    onBack()
                } else {
                    viewModel.setTab(ProfileSettingsTab.ROOT)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
