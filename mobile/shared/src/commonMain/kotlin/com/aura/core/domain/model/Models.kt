package com.aura.core.domain.model

import kotlinx.serialization.Serializable

@Serializable data class User(val id: String, val email: String, val name: String?, val avatarUrl: String? = null)

@Serializable data class SkinProfile(val id: String, val userId: String, val skinType: String, val ageRange: String? = null, 
    val concerns: List<String> = emptyList(), val allergies: List<String> = emptyList(), val goals: List<String> = emptyList(), val isCompleted: Boolean = false)

@Serializable data class Product(val id: String, val name: String, val brand: String, val categoryId: String? = null, 
    val description: String? = null, val imageUrl: String? = null, val price: String? = null, val currency: String = "USD")

@Serializable data class ProductIngredient(val id: String, val name: String, val position: Int, val safetyLevel: String)

@Serializable data class Recommendation(val id: String, val productId: String, val score: Double, val reason: String, val type: String, val isViewed: Boolean = false, val isSaved: Boolean = false)

@Serializable data class TrackerLog(val id: String, val productId: String?, val action: String, val loggedAt: String, val notes: String? = null, val rating: Int? = null)

@Serializable data class Reminder(val id: String, val title: String, val message: String? = null, val scheduledAt: String, val repeatType: String, val isActive: Boolean = true)

@Serializable data class ChatMessage(val id: String, val content: String, val isFromUser: Boolean, val timestamp: Long = System.currentTimeMillis())