package com.aura.feature.auth

import androidx.compose.foundation.layout.*, androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons, androidx.compose.material.icons.filled.Email, androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*, androidx.compose.runtime.*, androidx.compose.ui.Alignment, androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType, androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Aura", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Text(if (isLogin) "С возвращением!" else "Создайте аккаунт", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

        Button(onClick = { isLoading = true; onAuthSuccess() }, enabled = !isLoading && email.isNotBlank() && password.length >= 8,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(if (isLogin) "Войти" else "Зарегистрироваться")
        }

        TextButton(onClick = { isLogin = !isLogin }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (isLogin) "Нет аккаунта? Зарегистрируйтесь" else "Уже есть аккаунт? Войдите")
        }
    }
}