package com.aura.feature.auth

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.core.data.api.AuraApiClient
import com.aura.core.data.repository.TokenManager
import com.aura.core.i18n.StringsRu
import com.aura.core.ui.components.AuraLotusLogo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ─── Palette ──────────────────────────────────────────────
private val BgColor = Color(0xFFF4F7FE)
private val LavenderBlob = Color(0xFFE0C3FC)
private val MintBlob = Color(0xFFA7F3D0)
private val TextOnBg = Color(0xFF2D3648)
private val TextSubtitle = Color(0xFF2D3648).copy(alpha = 0.6f)
private val TextLabel = Color(0xFF434655).copy(alpha = 0.7f)
private val InputPlaceholder = Color(0xFF434655).copy(alpha = 0.3f)
private val PrimaryGreen = Color(0xFF059669)
private val BtnGradientEnd = Color(0xFFB4D9FF)
private val ErrorColor = Color(0xFFEF4444)

// ─── Regex ────────────────────────────────────────────────
private val EMAIL_REGEX = Regex("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)
private val PHONE_REGEX = Regex("^\\+?[0-9\\s()-]{7,15}$")
private val NICKNAME_REGEX = Regex("^[a-zA-Z0-9_]+$")

private fun normalizeNicknameInput(input: String): String {
    val clean = input.removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }.take(32)
    return "@$clean"
}

private fun normalizePhoneDigits(input: String): String =
    run {
        val digits = input.filter { it.isDigit() }
        val withoutCountry = when {
            digits.length >= 11 && (digits.startsWith("7") || digits.startsWith("8")) -> digits.drop(1)
            else -> digits
        }
        withoutCountry.take(10)
    }

private fun formatPhoneForDisplay(digitsInput: String): String {
    val rest = digitsInput.filter { it.isDigit() }.take(10)
    if (rest.isEmpty()) return "+7"

    val p1 = rest.take(3)
    val p2 = rest.drop(3).take(3)
    val p3 = rest.drop(6).take(2)
    val p4 = rest.drop(8).take(2)

    val out = StringBuilder().append("+7")
    if (p1.isNotEmpty()) out.append(" (").append(p1)
    if (p1.length == 3) out.append(")")
    if (p2.isNotEmpty()) out.append(" ").append(p2)
    if (p3.isNotEmpty()) out.append("-").append(p3)
    if (p4.isNotEmpty()) out.append("-").append(p4)

    return out.toString()
}

// ─── Screen ───────────────────────────────────────────────
@Composable
fun AuthScreen(
    onAuthSuccess: (Boolean) -> Unit,
    apiClient: AuraApiClient
) {
    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("@") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        if (isLogin) {
            if (email.isBlank()) return "Введите email"
            if (!EMAIL_REGEX.matches(email)) return "Некорректный email"
            if (password.length < 6) return "Пароль минимум 6 символов"
        } else {
            if (name.isBlank()) return "Введите имя"
            if (name.length < 2) return "Имя минимум 2 символа"
            if (nickname.isBlank() || nickname == "@") return "Введите логин"
            val cleanNickname = nickname.removePrefix("@")
            if (!NICKNAME_REGEX.matches(cleanNickname)) return "Логин: только латиница, цифры, _"
            if (phone.isNotBlank() && !PHONE_REGEX.matches(phone)) return "Некорректный телефон"
            if (email.isBlank()) return "Введите email"
            if (!EMAIL_REGEX.matches(email)) return "Некорректный email"
            if (password.length < 6) return "Пароль минимум 6 символов"
            if (password != confirmPassword) return "Пароли не совпадают"
        }
        return null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
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
            HeaderSection()
            Spacer(modifier = Modifier.height(24.dp))
            AuthGlassCard(
                isLogin = isLogin,
                name = name, onNameChange = { name = it },
                nickname = nickname, onNicknameChange = { nickname = normalizeNicknameInput(it) },
                phone = phone, onPhoneChange = { phone = normalizePhoneDigits(it) },
                email = email, onEmailChange = { email = it },
                password = password, onPasswordChange = { password = it },
                confirmPassword = confirmPassword, onConfirmPasswordChange = { confirmPassword = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                onToggleMode = {
                    isLogin = !isLogin
                    errorMessage = null
                    if (!isLogin && nickname.isBlank()) nickname = "@"
                },
                onSubmit = {
                    val err = validate()
                    if (err != null) {
                        println("[AUTH] VALIDATION FAILED: $err")
                        errorMessage = err
                        return@AuthGlassCard
                    }
                    isLoading = true
                    errorMessage = null
                    println("[AUTH] SUBMIT ${if (isLogin) "LOGIN" else "REGISTER"}")
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            println("[AUTH] CALLING API ${if (isLogin) "login" else "register"}...")
                            val response = if (isLogin) {
                                apiClient.login(email, password)
                            } else {
                                val cleanNickname = nickname.removePrefix("@").trim()
                                val cleanPhone = phone.takeIf { it.isNotBlank() }?.trim()
                                println("[AUTH] register: name=$name, email=$email, nickname=$cleanNickname, phone=$cleanPhone")
                                apiClient.register(
                                    name = name.trim(), email = email.trim(), password = password,
                                    nickname = cleanNickname, phone = cleanPhone
                                )
                            }
                            println("[AUTH] SUCCESS: token=${response.access_token.take(20)}..., user=${response.user.name}")
                            TokenManager.setToken(response.access_token)
                            TokenManager.setUser(response.user)
                            onAuthSuccess(!isLogin)
                        } catch (e: Exception) {
                            val msg = e.message ?: "Unknown error"
                            println("[AUTH] ERROR: $msg")
                            e.printStackTrace()
                            errorMessage = msg
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Background ───────────────────────────────────────────
@Composable
private fun MeshBackgroundBlobs() {
    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        drawCircle(color = LavenderBlob.copy(alpha = 0.5f), radius = size.width * 0.5f, center = Offset(0f, 0f))
        drawCircle(color = MintBlob.copy(alpha = 0.5f), radius = size.width * 0.5f, center = Offset(size.width, size.height))
        drawCircle(color = MintBlob.copy(alpha = 0.4f), radius = size.width * 0.3f, center = Offset(size.width * 0.9f, size.height * 0.2f))
    }
}

// ─── Header ───────────────────────────────────────────────
@Composable
private fun HeaderSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AuraLotusLogo(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AURA",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = TextOnBg,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = StringsRu.Auth.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSubtitle,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Auth Card ────────────────────────────────────────────
@Composable
private fun AuthGlassCard(
    isLogin: Boolean,
    name: String, onNameChange: (String) -> Unit,
    nickname: String, onNicknameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .animateContentSize()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0xFF1F2687).copy(alpha = 0.05f))
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF2F5FA))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLogin) StringsRu.Auth.loginTitle else StringsRu.Auth.registerTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextOnBg
        )

        Spacer(modifier = Modifier.height(if (isLogin) 24.dp else 20.dp))

        if (!isLogin) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassTextField(modifier = Modifier.weight(1f), value = name, onValueChange = onNameChange, label = StringsRu.Auth.nameLabel, placeholder = "Александра")
                GlassTextField(modifier = Modifier.weight(1f), value = nickname, onValueChange = onNicknameChange, label = StringsRu.Auth.loginLabel, placeholder = "@username")
            }
            Spacer(modifier = Modifier.height(16.dp))
            GlassTextField(
                modifier = Modifier.fillMaxWidth(),
                value = formatPhoneForDisplay(phone),
                onValueChange = onPhoneChange,
                label = StringsRu.Auth.phoneLabel,
                placeholder = "+7 (900) 000-00-00",
                keyboardType = KeyboardType.Phone
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        GlassTextField(modifier = Modifier.fillMaxWidth(), value = email, onValueChange = onEmailChange, label = StringsRu.Auth.emailLabel, placeholder = "email@example.com", keyboardType = KeyboardType.Email)
        Spacer(modifier = Modifier.height(if (isLogin) 16.dp else 12.dp))

        GlassPasswordField(
            value = password, onValueChange = onPasswordChange,
            label = StringsRu.Auth.passwordLabel, placeholder = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
        )

        if (!isLogin) {
            Spacer(modifier = Modifier.height(12.dp))
            GlassPasswordField(
                value = confirmPassword, onValueChange = onConfirmPasswordChange,
                label = StringsRu.Auth.confirmLabel, placeholder = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
            )
        }

        if (isLogin) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(text = StringsRu.Auth.forgotPassword, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextOnBg.copy(alpha = 0.7f), modifier = Modifier.clickable { })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(PrimaryGreen, BtnGradientEnd)), RoundedCornerShape(16.dp))
                .clickable(enabled = !isLoading, onClick = onSubmit)
                .shadow(elevation = 8.dp, spotColor = PrimaryGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (isLogin) StringsRu.Auth.loginAction else StringsRu.Auth.registerAction, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage, color = ErrorColor, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row {
            Text(text = if (isLogin) StringsRu.Auth.noAccount else StringsRu.Auth.hasAccount, fontSize = 14.sp, color = TextLabel)
            Text(
                text = if (isLogin) StringsRu.Auth.registerAction else StringsRu.Auth.loginAction,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onToggleMode() }
            )
        }
    }
}

// ─── Glass Text Field ─────────────────────────────────────
@Composable
private fun GlassTextField(
    modifier: Modifier = Modifier,
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLabel, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = TextStyle(color = TextOnBg, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(PrimaryGreen),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) Text(text = placeholder, color = InputPlaceholder, fontSize = 14.sp)
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun GlassPasswordField(
    value: String, onValueChange: (String) -> Unit,
    label: String, placeholder: String
) {
    var isVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLabel, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = TextStyle(color = TextOnBg, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            cursorBrush = SolidColor(PrimaryGreen),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(text = placeholder, color = InputPlaceholder, fontSize = 14.sp)
                        innerTextField()
                    }
                    Icon(
                        imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = TextLabel,
                        modifier = Modifier.size(20.dp).clickable { isVisible = !isVisible }
                    )
                }
            }
        )
    }
}

// ─── Glass Modifier ───────────────────────────────────────
fun Modifier.glassmorphism(shape: Shape, alpha: Float, borderAlpha: Float): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = alpha))
    .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
