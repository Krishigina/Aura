package com.aura.feature.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.SoftPastelBackground
import com.aura.core.ui.components.SoftPastelVariant
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.defaultAuraDesignTokens
import com.aura.feature.auth.presentation.AuthViewModel
import org.koin.compose.koinInject

@Composable
fun AuthScreen(
    onAuthSuccess: (Boolean) -> Unit,
    viewModel: AuthViewModel = koinInject(),
) {
    val auth = MaterialTheme.aura.auth
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated, uiState.isNewUser) {
        if (uiState.isAuthenticated) {
            onAuthSuccess(uiState.isNewUser)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(auth.backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        SoftPastelBackground(variant = SoftPastelVariant.Auth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = auth.screenHorizontalPadding, vertical = auth.screenVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AuthGlassCard(
                isLogin = uiState.isLogin,
                name = uiState.name, onNameChange = viewModel::updateName,
                nickname = uiState.nickname, onNicknameChange = viewModel::updateNickname,
                email = uiState.email, onEmailChange = viewModel::updateEmail,
                password = uiState.password, onPasswordChange = viewModel::updatePassword,
                confirmPassword = uiState.confirmPassword, onConfirmPasswordChange = viewModel::updateConfirmPassword,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onToggleMode = viewModel::toggleMode,
                onSubmit = viewModel::submitCurrentForm,
            )
            Spacer(modifier = Modifier.height(auth.bottomSpacer))
        }
    }
}

@Composable
private fun AuthGlassCard(
    isLogin: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
) {
    val auth = MaterialTheme.aura.auth

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = auth.cardOuterHorizontalPadding)
            .animateContentSize()
            .shadow(elevation = auth.cardShadowElevation, shape = RoundedCornerShape(auth.cardRadius), spotColor = auth.cardShadowColor.copy(alpha = auth.cardShadowAlpha))
            .clip(RoundedCornerShape(auth.cardRadius))
            .background(auth.cardSurfaceColor)
            .padding(horizontal = auth.cardHorizontalPadding, vertical = auth.cardVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isLogin) StringsRu.Auth.loginTitle else StringsRu.Auth.registerTitle,
            fontSize = auth.titleFontSize,
            fontWeight = FontWeight.Bold,
            color = auth.textColor,
        )

        Spacer(modifier = Modifier.height(if (isLogin) auth.titleBottomGapLogin else auth.titleBottomGapRegister))

        if (!isLogin) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(auth.nameRowGap)) {
                GlassTextField(modifier = Modifier.weight(1f), value = name, onValueChange = onNameChange, label = StringsRu.Auth.nameLabel, placeholder = "Александра")
                GlassTextField(modifier = Modifier.weight(1f), value = nickname, onValueChange = onNicknameChange, label = StringsRu.Auth.loginLabel, placeholder = "@username")
            }
            Spacer(modifier = Modifier.height(auth.nameRowBottomGap))
        }

        GlassTextField(modifier = Modifier.fillMaxWidth(), value = email, onValueChange = onEmailChange, label = StringsRu.Auth.emailLabel, placeholder = "email@example.com", keyboardType = KeyboardType.Email)
        Spacer(modifier = Modifier.height(if (isLogin) auth.fieldGapLogin else auth.fieldGapRegister))

        GlassPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = StringsRu.Auth.passwordLabel,
            placeholder = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022",
        )

        if (!isLogin) {
            Spacer(modifier = Modifier.height(auth.fieldGapRegister))
            GlassPasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = StringsRu.Auth.confirmLabel,
                placeholder = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022",
            )
        }

        if (isLogin) {
            Spacer(modifier = Modifier.height(auth.forgotTopGap))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(text = StringsRu.Auth.forgotPassword, fontSize = auth.forgotFontSize, fontWeight = FontWeight.SemiBold, color = auth.textColor.copy(alpha = auth.forgotAlpha), modifier = Modifier.clickable { })
            }
        }

        Spacer(modifier = Modifier.height(auth.submitTopGap))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(auth.submitHeight)
                .clip(RoundedCornerShape(auth.submitRadius))
                .background(Brush.horizontalGradient(listOf(auth.primaryColor, auth.buttonGradientEnd)), RoundedCornerShape(auth.submitRadius))
                .clickable(enabled = !isLoading, onClick = onSubmit)
                .shadow(elevation = auth.submitShadowElevation, spotColor = auth.primaryColor.copy(alpha = auth.submitShadowAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(auth.submitProgressSize), color = auth.glassSurfaceColor, strokeWidth = auth.submitProgressStroke)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (isLogin) StringsRu.Auth.loginAction else StringsRu.Auth.registerAction, fontSize = auth.submitTextFontSize, fontWeight = FontWeight.Bold, color = auth.glassSurfaceColor, letterSpacing = auth.submitTextLetterSpacing)
                    Spacer(modifier = Modifier.width(auth.submitIconGap))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = auth.glassSurfaceColor, modifier = Modifier.size(auth.submitIconSize))
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(auth.errorTopGap))
            Text(text = errorMessage, color = auth.errorColor, fontSize = auth.errorFontSize, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(auth.modeToggleTopGap))

        Row {
            Text(text = if (isLogin) StringsRu.Auth.noAccount else StringsRu.Auth.hasAccount, fontSize = auth.modeToggleFontSize, color = auth.labelColor)
            Text(
                text = if (isLogin) StringsRu.Auth.registerAction else StringsRu.Auth.loginAction,
                fontSize = auth.modeToggleFontSize,
                fontWeight = FontWeight.Bold,
                color = auth.primaryColor,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onToggleMode() },
            )
        }
    }
}

@Composable
private fun GlassTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val auth = MaterialTheme.aura.auth

    Column(modifier = modifier) {
        Text(text = label, fontSize = auth.fieldLabelFontSize, fontWeight = FontWeight.Bold, color = auth.labelColor, letterSpacing = auth.fieldLabelLetterSpacing)
        Spacer(modifier = Modifier.height(auth.fieldLabelBottomGap))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = auth.textColor, fontSize = auth.fieldTextFontSize),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(auth.primaryColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(auth.fieldHeight)
                        .border(BorderStroke(auth.glassBorderWidth, auth.glassBorderColor.copy(alpha = auth.fieldBorderAlpha)), RoundedCornerShape(auth.fieldRadius))
                        .padding(start = auth.fieldStartPadding, end = auth.fieldEndPadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) Text(text = placeholder, color = auth.placeholderColor, fontSize = auth.fieldTextFontSize)
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun GlassPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
) {
    val auth = MaterialTheme.aura.auth
    var isVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = auth.fieldLabelFontSize, fontWeight = FontWeight.Bold, color = auth.labelColor, letterSpacing = auth.fieldLabelLetterSpacing)
        Spacer(modifier = Modifier.height(auth.fieldLabelBottomGap))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = auth.textColor, fontSize = auth.fieldTextFontSize),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            cursorBrush = SolidColor(auth.primaryColor),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(auth.fieldHeight)
                        .border(BorderStroke(auth.glassBorderWidth, auth.glassBorderColor.copy(alpha = auth.fieldBorderAlpha)), RoundedCornerShape(auth.fieldRadius))
                        .padding(start = auth.fieldStartPadding, end = auth.fieldEndPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(text = placeholder, color = auth.placeholderColor, fontSize = auth.fieldTextFontSize)
                        innerTextField()
                    }
                    Icon(
                        imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = auth.labelColor,
                        modifier = Modifier.size(auth.passwordIconSize).clickable { isVisible = !isVisible },
                    )
                }
            },
        )
    }
}
fun Modifier.glassmorphism(shape: Shape, alpha: Float, borderAlpha: Float): Modifier = this
    .clip(shape)
    .background(defaultAuraDesignTokens().auth.glassSurfaceColor.copy(alpha = alpha))
    .border(defaultAuraDesignTokens().auth.glassBorderWidth, defaultAuraDesignTokens().auth.glassBorderColor.copy(alpha = borderAlpha), shape)
