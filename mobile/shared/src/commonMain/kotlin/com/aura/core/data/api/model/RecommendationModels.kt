package com.aura.core.data.api.model

import com.aura.core.domain.model.ExtendedSkinProfile
import com.aura.core.domain.model.ScoreBreakdown
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendationSummary(
    val title: String = "",
    val description: String = "",
    val confidence: String = "medium",
)

@Serializable
data class RecommendationInputQuality(
    @SerialName("skin_passport") val skinPassport: String = "missing",
    @SerialName("sensor_readings") val sensorReadings: String = "missing",
    val procedures: String = "none",
    val notes: List<String> = emptyList(),
)

@Serializable
data class RecommendationStep(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val productName: String = "",
    val brand: String = "",
    val step: String = "",
    @SerialName("compatibility_percent") val compatibilityPercent: Int = 0,
    val reason: String = "",
    val instruction: String = "",
    val frequency: String = "",
    val warnings: List<String> = emptyList(),
    @SerialName("reason_codes") val reasonCodes: List<String> = emptyList(),
    val sequence: Int = 0,
    @SerialName("score_breakdown") val scoreBreakdown: ScoreBreakdown = ScoreBreakdown(),
    val explanations: List<String> = emptyList(),
    val decision: String = "recommend",
)

@Serializable
data class RecommendationRoutine(
    val morning: List<RecommendationStep> = emptyList(),
    val evening: List<RecommendationStep> = emptyList(),
    val weekly: List<RecommendationStep> = emptyList(),
)

@Serializable
data class RecommendationLine(
    val key: String = "budget",
    val title: String = "Бюджетная",
    val positioning: String = "",
    val routine: RecommendationRoutine = RecommendationRoutine(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RecommendationResponse(
    val id: String? = null,
    @SerialName("generated_at") val generatedAt: String = "",
    val summary: RecommendationSummary = RecommendationSummary(),
    @SerialName("input_quality") val inputQuality: RecommendationInputQuality = RecommendationInputQuality(),
    @SerialName("extended_skin_profile") val extendedSkinProfile: ExtendedSkinProfile = ExtendedSkinProfile(),
    val lines: List<RecommendationLine> = emptyList(),
    @SerialName("procedure_context") val procedureContext: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RecommendationFavorite(
    @SerialName("favorite_id") val favoriteId: String = "",
    val id: String? = null,
    @SerialName("saved_at") val savedAt: String = "",
    @SerialName("generated_at") val generatedAt: String = "",
    val summary: RecommendationSummary = RecommendationSummary(),
    @SerialName("input_quality") val inputQuality: RecommendationInputQuality = RecommendationInputQuality(),
    @SerialName("extended_skin_profile") val extendedSkinProfile: ExtendedSkinProfile = ExtendedSkinProfile(),
    val lines: List<RecommendationLine> = emptyList(),
    @SerialName("procedure_context") val procedureContext: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    fun asRecommendationResponse(): RecommendationResponse = RecommendationResponse(
        id = id,
        generatedAt = generatedAt,
        summary = summary,
        inputQuality = inputQuality,
        extendedSkinProfile = extendedSkinProfile,
        lines = lines,
        procedureContext = procedureContext,
        warnings = warnings,
    )
}

@Serializable
data class RecommendationFavoritesResponse(
    val items: List<RecommendationFavorite> = emptyList(),
)

@Serializable
data class RecommendationFavoriteRequest(
    val recommendation: RecommendationResponse,
)

@Serializable
data class RecommendationFavoriteResponse(
    val success: Boolean = false,
    val favorite: RecommendationResponse? = null,
)
