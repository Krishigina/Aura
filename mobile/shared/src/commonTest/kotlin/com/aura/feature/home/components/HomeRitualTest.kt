package com.aura.feature.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeRitualTest {
    @Test
    fun ritualGlowOnlyShowsWhenRitualHasItems() {
        assertFalse(shouldShowRitualGlow(hasRitualItems = false))
        assertTrue(shouldShowRitualGlow(hasRitualItems = true))
    }
}
