package com.aura.feature.product

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProductDetailGalleryStateTest {
    @Test
    fun openAtClampsToBounds() {
        val state = ProductDetailGalleryState(totalItems = 3)

        state.openAt(99)

        assertEquals(2, state.currentIndex)
    }

    @Test
    fun closeResetsFullscreenFlag() {
        val state = ProductDetailGalleryState(totalItems = 2)

        state.openAt(1)
        state.close()

        assertFalse(state.isFullscreen)
    }
}
