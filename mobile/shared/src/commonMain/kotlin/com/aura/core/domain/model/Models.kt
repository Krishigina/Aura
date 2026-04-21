package com.aura.core.domain.model

import kotlinx.serialization.Serializable

@Serializable data class User(val id: String, val email: String, val name: String?, val avatarUrl: String? = null)

@Serializable data class SkinProfile(val id: String, val userId: String, val skinType: String, val ageRange: String? = null, 
    val concerns: List<String> = emptyList(), val allergies: List<String> = emptyList(), val goals: List<String> = emptyList(), val isCompleted: Boolean = false)

@Serializable data class Product(
    val id: Int = 0,
    val name: String? = null,
    val brand: String? = null,
    val what_is_it: String? = null,
    val product_type: String? = null,
    val for_whom: String? = null,
    val purpose: List<String>? = null,
    val skin_type: List<String>? = null,
    val application_time: String? = null,
    val area: String? = null,
    val active_ingredient: String? = null,
    val volume: String? = null,
    val segment: String? = null,
    val composition: String? = null,
    val application_info: String? = null,
    val country: String? = null,
    val country_origin: String? = null,
    val manufacturer: String? = null,
    val description: String? = null,
    val images: List<String>? = null,
    val video: String? = null,
    val has_video: Boolean? = false,
    val created_at: String? = null,
    val photos: List<ProductPhoto>? = null,
    val imageUrl: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val category: String? = null,
    val desc: String? = null
)

@Serializable data class ProductPhoto(
    val id: String = "",
    val filename: String = "",
    val data: String = "",
    val content_type: String = ""
)

@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class WeatherSnapshot(
    val temperatureCelsius: Double? = null,
    val uvIndex: Double? = null,
    val isDay: Boolean? = null
)

@Serializable data class ProductIngredient(val id: String, val name: String, val position: Int, val safetyLevel: String)

@Serializable data class Recommendation(val id: String, val productId: String, val score: Double, val reason: String, val type: String, val isViewed: Boolean = false, val isSaved: Boolean = false)

@Serializable data class TrackerLog(val id: String, val productId: String?, val action: String, val loggedAt: String, val notes: String? = null, val rating: Int? = null)

@Serializable data class Reminder(val id: String, val title: String, val message: String? = null, val scheduledAt: String, val repeatType: String, val isActive: Boolean = true)

@Serializable data class ChatMessage(val id: String, val content: String, val isFromUser: Boolean, val timestamp: Long = System.currentTimeMillis())
