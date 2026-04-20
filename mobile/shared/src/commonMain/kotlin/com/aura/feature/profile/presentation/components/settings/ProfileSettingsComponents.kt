package com.aura.feature.profile.presentation.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aura.core.ui.components.AuraTopBar
import com.aura.core.ui.theme.aura

@Composable
internal fun DangerEntryCard(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    val settingsTokens = MaterialTheme.aura.profileSettings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(settingsTokens.dangerCardRadius))
            .background(settingsTokens.dangerCardSurfaceColor)
            .border(settingsTokens.buttonBorderWidth, settingsTokens.dangerCardBorderColor, RoundedCornerShape(settingsTokens.dangerCardRadius))
            .clickable { onClick() }
            .padding(settingsTokens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(settingsTokens.cardIconSize)
                    .background(settingsTokens.cardIconSurfaceColor, RoundedCornerShape(settingsTokens.cardIconRadius)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = settingsTokens.dangerIconTint)
            }
            Spacer(modifier = Modifier.width(settingsTokens.cardTextGap))
            Column {
                Text(title, color = settingsTokens.dangerTitleColor, fontSize = settingsTokens.dangerTitleFontSize, fontWeight = FontWeight.SemiBold)
                Text(value, color = settingsTokens.dangerValueColor, fontSize = settingsTokens.dangerValueFontSize)
            }
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = settingsTokens.dangerValueColor)
    }
}

@Composable
internal fun SettingsHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val settingsTokens = MaterialTheme.aura.profileSettings
    AuraTopBar(
        title = title,
        onBack = onBack,
        titleColor = settingsTokens.topBarTitleColor,
        iconTint = settingsTokens.topBarIconTint,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsEntryCard(title: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    val settingsTokens = MaterialTheme.aura.profileSettings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(settingsTokens.entryCardRadius))
            .background(settingsTokens.entryCardSurfaceColor.copy(alpha = settingsTokens.entryCardSurfaceAlpha))
            .border(settingsTokens.buttonBorderWidth, settingsTokens.entryCardBorderColor.copy(alpha = settingsTokens.entryCardBorderAlpha), RoundedCornerShape(settingsTokens.entryCardRadius))
            .clickable { onClick() }
            .padding(settingsTokens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(settingsTokens.cardIconSize)
                    .background(settingsTokens.cardIconSurfaceColor, RoundedCornerShape(settingsTokens.cardIconRadius)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = settingsTokens.entryIconTint)
            }
            Spacer(modifier = Modifier.width(settingsTokens.cardTextGap))
            Column {
                Text(title, color = settingsTokens.entryTitleColor, fontSize = settingsTokens.entryTitleFontSize, fontWeight = FontWeight.SemiBold)
                Text(value, color = settingsTokens.entryValueColor, fontSize = settingsTokens.entryValueFontSize)
            }
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = settingsTokens.entryIconTint)
    }
}

@Composable
internal fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    val settingsTokens = MaterialTheme.aura.profileSettings
    Column {
        Text(label, fontSize = settingsTokens.fieldLabelFontSize, fontWeight = FontWeight.SemiBold, color = settingsTokens.fieldLabelColor)
        Spacer(modifier = Modifier.height(settingsTokens.fieldLabelBottomGap))
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = settingsTokens.fieldTextColor, fontSize = settingsTokens.fieldTextFontSize),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(settingsTokens.fieldCursorColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(settingsTokens.fieldHeight)
                        .clip(RoundedCornerShape(settingsTokens.fieldRadius))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = settingsTokens.fieldTopSurfaceAlpha), Color.White.copy(alpha = settingsTokens.fieldBottomSurfaceAlpha))))
                        .border(settingsTokens.buttonBorderWidth, Color.White.copy(alpha = settingsTokens.fieldBorderAlpha), RoundedCornerShape(settingsTokens.fieldRadius))
                        .padding(horizontal = settingsTokens.fieldHorizontalPadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) Text(placeholder, color = settingsTokens.fieldPlaceholderColor, fontSize = settingsTokens.fieldPlaceholderFontSize)
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun GlassPasswordField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String) {
    var visible by remember { mutableStateOf(false) }
    val settingsTokens = MaterialTheme.aura.profileSettings
    Column {
        Text(label, fontSize = settingsTokens.fieldLabelFontSize, fontWeight = FontWeight.SemiBold, color = settingsTokens.fieldLabelColor)
        Spacer(modifier = Modifier.height(settingsTokens.fieldLabelBottomGap))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = settingsTokens.fieldTextColor, fontSize = settingsTokens.fieldTextFontSize),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            cursorBrush = SolidColor(settingsTokens.fieldCursorColor),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(settingsTokens.fieldHeight)
                        .clip(RoundedCornerShape(settingsTokens.fieldRadius))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = settingsTokens.fieldTopSurfaceAlpha), Color.White.copy(alpha = settingsTokens.fieldBottomSurfaceAlpha))))
                        .border(settingsTokens.buttonBorderWidth, Color.White.copy(alpha = settingsTokens.fieldBorderAlpha), RoundedCornerShape(settingsTokens.fieldRadius))
                        .padding(horizontal = settingsTokens.fieldHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(placeholder, color = settingsTokens.fieldPlaceholderColor, fontSize = settingsTokens.fieldPlaceholderFontSize)
                        innerTextField()
                    }
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = settingsTokens.visibilityIconTint,
                        modifier = Modifier.size(settingsTokens.visibilityIconSize).clickable { visible = !visible },
                    )
                }
            },
        )
    }
}

@Composable
internal fun SaveButton(isSaving: Boolean, onClick: () -> Unit) {
    val settingsTokens = MaterialTheme.aura.profileSettings
    Button(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth().height(settingsTokens.buttonHeight),
        shape = RoundedCornerShape(settingsTokens.buttonRadius),
        border = BorderStroke(settingsTokens.buttonBorderWidth, settingsTokens.saveButtonBorderColor),
        colors = ButtonDefaults.buttonColors(containerColor = settingsTokens.saveButtonContainerColor, contentColor = Color.White),
    ) {
        Text(if (isSaving) "Сохранение..." else "Сохранить", fontWeight = FontWeight.Bold)
    }
}
