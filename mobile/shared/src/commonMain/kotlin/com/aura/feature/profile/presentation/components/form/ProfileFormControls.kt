package com.aura.feature.profile.presentation.components.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.aura.core.ui.theme.aura
import com.aura.core.ui.theme.auraHex
import com.aura.core.ui.theme.auraTokenDp
import com.aura.core.ui.theme.auraTokenSp
import com.aura.feature.profile.logic.formatMaskedTime

@Composable
fun FrequencyButtonInline(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.aura.profile.sky500,
    accentBorder: Color = MaterialTheme.aura.profile.sky200,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.height(auraTokenDp(40f)), shape = RoundedCornerShape(auraTokenDp(10f)), border = BorderStroke(auraTokenDp(1f), if (selected) accent else accentBorder), colors = ButtonDefaults.buttonColors(containerColor = if (selected) accent else Color.White, contentColor = if (selected) Color.White else accent)) {
        Text(label, fontSize = auraTokenSp(12f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RoutineTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, accent: Color = MaterialTheme.aura.profile.sky500) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    fun buildMaskedTimeFieldValue(raw: TextFieldValue): TextFieldValue {
        val digits = raw.text.filter { it.isDigit() }.take(4)
        val masked = formatMaskedTime(digits)
        val digitsBeforeCursor = raw.text.take(raw.selection.start).count { it.isDigit() }.coerceIn(0, digits.length)
        val cursor = when {
            digitsBeforeCursor <= 2 -> digitsBeforeCursor
            else -> (digitsBeforeCursor + 1).coerceAtMost(masked.length)
        }
        return TextFieldValue(masked, TextRange(cursor))
    }

    Column {
        Text(label, fontSize = auraTokenSp(12f), fontWeight = FontWeight.SemiBold, color = MaterialTheme.aura.profile.slate700)
        Spacer(modifier = Modifier.height(auraTokenDp(6f)))
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                if (keyboardType == KeyboardType.Number) {
                    val masked = buildMaskedTimeFieldValue(newValue)
                    fieldValue = masked
                    onValueChange(masked.text)
                } else {
                    fieldValue = newValue
                    onValueChange(newValue.text)
                }
            },
            singleLine = true,
            textStyle = TextStyle(color = auraHex(0xFF1E293B), fontSize = auraTokenSp(14f)),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(accent),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth().height(auraTokenDp(48f)).clip(RoundedCornerShape(auraTokenDp(12f))).background(Color.White).border(auraTokenDp(1f), MaterialTheme.aura.profile.sky200, RoundedCornerShape(auraTokenDp(12f))).padding(horizontal = auraTokenDp(12f)), contentAlignment = Alignment.CenterStart) {
                    if (fieldValue.text.isEmpty()) Text(placeholder, color = auraHex(0xFF94A3B8), fontSize = auraTokenSp(13f))
                    innerTextField()
                }
            },
        )
    }
}
