package com.aura.core.navigation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.aura.core.ui.components.GlassSurface
import com.aura.core.ui.theme.aura

private data class BottomNavTabInfo(val label: String, val icon: ImageVector, val route: String)

private val bottomTabs = listOf(
    BottomNavTabInfo("Главная", Icons.Rounded.Home, Routes.HOME),
    BottomNavTabInfo("Уход", Icons.Rounded.Spa, Routes.CATALOG),
    BottomNavTabInfo("Анализ", Icons.Rounded.CenterFocusStrong, Routes.CHAT),
    BottomNavTabInfo("Профиль", Icons.Rounded.Person, Routes.PROFILE),
)

@Composable
internal fun AuraBottomNavigationBar(
    selectedTab: Int,
    dark: Boolean,
    textBody: Color,
    textMuted: Color,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navTokens = MaterialTheme.aura.navigation

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = navTokens.bottomBarHorizontalPadding,
                end = navTokens.bottomBarHorizontalPadding,
                bottom = navTokens.bottomBarBottomPadding,
            ),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = navTokens.bottomBarContentHorizontalPadding,
                    vertical = navTokens.bottomBarContentVerticalPadding,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomTabs.forEachIndexed { index, tab ->
                val isActive = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(tab.route) }
                        .padding(navTokens.tabPadding),
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .background(
                                    navTokens.selectedMenuColor.copy(
                                        alpha = if (dark) navTokens.activeIndicatorDarkAlpha else navTokens.activeIndicatorLightAlpha,
                                    ),
                                    RoundedCornerShape(navTokens.activeIndicatorRadius),
                                )
                                .padding(navTokens.activeIndicatorPadding),
                        ) {
                            Icon(tab.icon, contentDescription = tab.label, tint = textBody, modifier = Modifier.size(navTokens.tabIconSize))
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label, tint = textMuted, modifier = Modifier.size(navTokens.tabIconSize))
                    }
                    Spacer(modifier = Modifier.height(navTokens.tabLabelTopGap))
                    Text(
                        text = tab.label,
                        fontSize = navTokens.tabLabelFontSize,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) textBody else textMuted,
                    )
                }
            }
        }
    }
}
