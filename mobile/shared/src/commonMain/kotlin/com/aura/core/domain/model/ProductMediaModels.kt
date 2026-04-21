package com.aura.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductVideo(
    val filename: String = "",
    val data: String = "",
    val content_type: String = "video/mp4",
)
