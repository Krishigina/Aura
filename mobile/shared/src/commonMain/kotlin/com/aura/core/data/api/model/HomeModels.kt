package com.aura.core.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class HomeRitualItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val checked: Boolean = false,
    val is_active: Boolean = false,
    val is_warning: Boolean = false,
)

@Serializable
data class HomeInsightItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: String = "profile",
)

@Serializable
data class HomeFeedResponse(
    val ritual_items: List<HomeRitualItem> = emptyList(),
    val insights: List<HomeInsightItem> = emptyList(),
)

@Serializable
data class WeatherCoordinates(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class HomeWeather(
    val temperature: String = "",
    val uv_index: String = "",
)

@Serializable
data class HomeTopWidget(
    val humidity_value: String = "",
    val humidity_subtitle: String = "",
    val air_quality: String = "",
    val air_status: String = "",
    val weather_advice: String = "",
)

@Serializable
data class HomeStatusResponse(
    val weather: HomeWeather = HomeWeather(),
    val top_widget: HomeTopWidget = HomeTopWidget(),
)
