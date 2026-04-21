package com.aura.feature.splash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplashTimelineTest {

    @Test
    fun coldStart_usesExpectedDurations() {
        val d = SplashTimelineDurations.forStart(isWarmStart = false)
        assertEquals(520, d.introMs)
        assertEquals(1100, d.holdMs)
        assertEquals(220, d.settleMs)
        assertEquals(460, d.exitMs)
        assertEquals(2300, d.totalMs)
        assertEquals(2300, d.minimumVisibleMs)
    }

    @Test
    fun warmStart_shortensHold() {
        val d = SplashTimelineDurations.forStart(isWarmStart = true)
        assertEquals(320, d.introMs)
        assertEquals(360, d.holdMs)
        assertEquals(120, d.settleMs)
        assertEquals(260, d.exitMs)
        assertEquals(1060, d.totalMs)
        assertEquals(1060, d.minimumVisibleMs)
    }

    @Test
    fun completeOnce_returnsTrueOnlyFirstCall() {
        val guard = SplashCompletionGuard()
        assertTrue(guard.completeOnce())
        assertFalse(guard.completeOnce())
    }

    @Test
    fun guard_preventsMultipleNavigationSignals() {
        val guard = SplashCompletionGuard()
        var calls = 0
        repeat(3) {
            if (guard.completeOnce()) calls++
        }
        assertEquals(1, calls)
    }
}
