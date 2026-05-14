package com.aura.feature.product

class ProductDetailGalleryState(private val totalItems: Int) {
    var isFullscreen: Boolean = false
        private set

    var currentIndex: Int = 0
        private set

    fun openAt(index: Int) {
        if (totalItems <= 0) return
        currentIndex = index.coerceIn(0, totalItems - 1)
        isFullscreen = true
    }

    fun close() {
        isFullscreen = false
    }
}
