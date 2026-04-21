package com.aura.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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
    val photos: List<ProductPhoto>? = null,
    val imageUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val category: String? = null,
    val desc: String? = null,
    @SerialName("has_video") val hasVideo: Boolean = false,
    val video: ProductVideo? = null,
    @SerialName("compatibility_percent") val compatibilityPercent: Int? = null,
    val decision: String? = null,
    val explanations: List<String> = emptyList(),
)

@Serializable
data class RoutineProductOption(
    val id: Int = 0,
    val brand: String? = null,
    val name: String? = null,
) {
    val displayLabel: String
        get() {
            val parts = listOf(brand, name)
                .map { it?.trim().orEmpty() }
                .filter { it.isNotEmpty() }
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "Без названия"
        }
}

@Serializable
data class ScoreBreakdown(
    val safety: Int = 0,
    val rules: Int = 0,
    @SerialName("skin_type") val skinType: Int = 0,
    @SerialName("skin_state") val skinState: Int = 0,
    val semantic: Int = 0,
    val routine: Int = 0,
    @SerialName("ingredient_function_fit") val ingredientFunctionFit: Int = 0,
    @SerialName("skin_state_fit") val skinStateFit: Int = 0,
    @SerialName("safety_fit") val safetyFit: Int = 0,
    @SerialName("evidence_quality") val evidenceQuality: Int = 0,
    @SerialName("metadata_confirmation") val metadataConfirmation: Int = 0,
)

@Serializable
data class ExtendedSkinProfile(
    @SerialName("global_skin_type") val globalSkinType: String = "",
    val concerns: List<String> = emptyList(),
    @SerialName("zone_concerns") val zoneConcerns: Map<String, List<String>> = emptyMap(),
    @SerialName("state_tags") val stateTags: List<String> = emptyList(),
)

@Serializable
data class EvidenceExplanation(
    val text: String = "",
    @SerialName("evidence_status") val evidenceStatus: String = "",
    @SerialName("source_ids") val sourceIds: List<Int> = emptyList(),
    val sources: List<JsonElement> = emptyList(),
)

@Serializable
data class ProductMatch(
    @SerialName("product_id") val productId: Int,
    @SerialName("compatibility_percent") val compatibilityPercent: Int = 0,
    val decision: String = "recommend",
    @SerialName("score_breakdown") val scoreBreakdown: ScoreBreakdown = ScoreBreakdown(),
    val explanations: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    @SerialName("matched_functions") val matchedFunctions: List<String> = emptyList(),
    @SerialName("evidence_explanations") val evidenceExplanations: List<EvidenceExplanation> = emptyList(),
)

@Serializable
data class ProductMatchesResponse(val items: List<ProductMatch> = emptyList())

@Serializable
data class ProductMatchingDetail(
    @SerialName("product_id") val productId: Int = 0,
    val decision: String = "recommend",
    @SerialName("final_score") val finalScore: Double = 0.0,
    @SerialName("compatibility_percent") val compatibilityPercent: Int = 0,
    @SerialName("score_breakdown") val scoreBreakdown: ScoreBreakdown = ScoreBreakdown(),
    val explanations: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val contraindications: List<String> = emptyList(),
    @SerialName("matched_goals") val matchedGoals: List<String> = emptyList(),
    @SerialName("matched_concerns") val matchedConcerns: List<String> = emptyList(),
    @SerialName("matched_functions") val matchedFunctions: List<String> = emptyList(),
    @SerialName("evidence_explanations") val evidenceExplanations: List<EvidenceExplanation> = emptyList(),
)

@Serializable
data class ProductDetailResponse(
    val product: Product = Product(),
    val matching: ProductMatchingDetail? = null,
    @SerialName("extended_skin_profile") val extendedSkinProfile: ExtendedSkinProfile = ExtendedSkinProfile(),
    @SerialName("assistant_context") val assistantContext: JsonObject? = null,
)

@Serializable data class ProductPhoto(
    val id: String = "",
    val filename: String = "",
    val data: String = "",
    val content_type: String = "",
    val url: String? = null,
)

@Serializable data class ProductIngredient(val id: String, val name: String, val position: Int, val safetyLevel: String)
