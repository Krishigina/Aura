package com.aura.feature.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.data.api.AuraApiClient
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.AuraLotusLogo
import com.aura.core.ui.theme.AppState
import com.aura.core.ui.theme.AuraPalette
import com.aura.core.ui.theme.auraThemeColors
import com.aura.feature.auth.mvi.AuthEffect
import com.aura.feature.auth.mvi.AuthIntent
import com.aura.feature.auth.mvi.AuthStore
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: (Boolean) -> Unit,
    apiClient: AuraApiClient
) {
    val dark = isSystemInDarkTheme() || AppState.isDarkMode
    val colors = auraThemeColors(dark)
    val store = remember(apiClient) { AuthStore(apiClient) }
    var uiState by remember { mutableStateOf(store.currentState()) }
    val scope = rememberCoroutineScope()

    fun dispatch(intent: AuthIntent) {
        uiState = store.dispatch(intent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        MeshBackgroundBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HeaderSection(textPrimary = colors.textBody, textSecondary = colors.textSecondary)
            Spacer(modifier = Modifier.height(24.dp))

            AuthGlassCard(
                uiState = uiState,
                dark = dark,
                textPrimary = colors.textBody,
                textSecondary = colors.textSecondary,
                onNameChange = { dispatch(AuthIntent.SetName(it)) },
                onNicknameChange = { dispatch(AuthIntent.SetNickname(it)) },
                onEmailChange = { dispatch(AuthIntent.SetEmail(it)) },
                onPasswordChange = { dispatch(AuthIntent.SetPassword(it)) },
                onConfirmPasswordChange = { dispatch(AuthIntent.SetConfirmPassword(it)) },
                onToggleMode = { dispatch(AuthIntent.ToggleMode) },
                onSubmit = {
                    scope.launch {
                        val effect = store.submit()
                        uiState = store.currentState()
                        if (effect is AuthEffect.AuthSucceeded) {
                            onAuthSuccess(effect.isNewUser)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MeshBackgroundBlobs() {
    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        drawCircle(color = AuraPalette.BrandLavender.copy(alpha = 0.5f), radius = size.width * 0.5f, center = Offset(0f, 0f))
        drawCircle(color = AuraPalette.BrandMint.copy(alpha = 0.5f), radius = size.width * 0.5f, center = Offset(size.width, size.height))
        drawCircle(color = AuraPalette.BlobBlue.copy(alpha = 0.4f), radius = size.width * 0.3f, center = Offset(size.width * 0.9f, size.height * 0.2f))
    }
}

@Composable
private fun HeaderSection(textPrimary: Color, textSecondary: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AuraLotusLogo(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AURA",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = textPrimary,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = StringsRu.Auth.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun AuthGlassCard(
    uiState: com.aura.feature.auth.mvi.AuthUiState,
    dark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .animateContentSize()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = AuraPalette.BrandPink.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(if (dark) AuraPalette.SurfaceCardDark else AuraPalette.SurfaceCardLight)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.isLogin) StringsRu.Auth.loginTitle else StringsRu.Auth.registerTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(if (uiState.isLogin) 24.dp else 20.dp))

        if (!uiState.isLogin) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassTextField(
                    modifier = Modifier.weight(1f),
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = StringsRu.Auth.nameLabel,
                    placeholder = "Александра",
                    dark = dark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                GlassTextField(
                    modifier = Modifier.weight(1f),
                    value = uiState.nickname,
                    onValueChange = onNicknameChange,
                    label = StringsRu.Auth.loginLabel,
                    placeholder = "@username",
                    dark = dark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        GlassTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.email,
            onValueChange = onEmailChange,
            label = StringsRu.Auth.emailLabel,
            placeholder = "email@example.com",
            keyboardType = KeyboardType.Email,
            dark = dark,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
        Spacer(modifier = Modifier.height(if (uiState.isLogin) 16.dp else 12.dp))

        GlassPasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = StringsRu.Auth.passwordLabel,
            placeholder = "••••••••",
            dark = dark,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        if (!uiState.isLogin) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassPasswordField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = StringsRu.Auth.confirmLabel,
                placeholder = "••••••••",
                dark = dark,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        if (uiState.isLogin) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = StringsRu.Auth.forgotPassword,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    modifier = Modifier.clickable { }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(AuraPalette.BrandRose, AuraPalette.BrandPink)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .clickable(enabled = !uiState.isLoading, onClick = onSubmit)
                .shadow(elevation = 8.dp, spotColor = AuraPalette.BrandPink.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.isLogin) StringsRu.Auth.loginAction else StringsRu.Auth.registerAction,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = uiState.errorMessage, color = AuraPalette.Error, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row {
            Text(
                text = if (uiState.isLogin) StringsRu.Auth.noAccount else StringsRu.Auth.hasAccount,
                fontSize = 14.sp,
                color = textSecondary
            )
            Text(
                text = if (uiState.isLogin) StringsRu.Auth.registerAction else StringsRu.Auth.loginAction,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AuraPalette.BrandPink,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onToggleMode() }
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
    dark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val borderColor = if (dark) AuraPalette.GlassBorderDark else AuraPalette.GlassBorderLight
    val placeholderColor = textSecondary.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(AuraPalette.BrandPink),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) Text(text = placeholder, color = placeholderColor, fontSize = 14.sp)
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun GlassPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    dark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    var isVisible by remember { mutableStateOf(false) }
    val borderColor = if (dark) AuraPalette.GlassBorderDark else AuraPalette.GlassBorderLight

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            cursorBrush = SolidColor(AuraPalette.BrandPink),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(text = placeholder, color = textSecondary.copy(alpha = 0.5f), fontSize = 14.sp)
                        innerTextField()
                    }
                    Icon(
                        imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isVisible = !isVisible }
                    )
                }
            }
        )
    }
}

fun Modifier.glassmorphism(shape: Shape, alpha: Float, borderAlpha: Float): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = alpha))
    .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
