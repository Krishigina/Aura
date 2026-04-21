package com.aura.feature.splash

data class SplashTimelineDurations(
    val introMs: Int,
    val holdMs: Int,
    val settleMs: Int,
    val exitMs: Int,
) {
    val totalMs: Int get() = introMs + holdMs + settleMs + exitMs
    val minimumVisibleMs: Int get() = totalMs

    companion object {
        fun forStart(isWarmStart: Boolean): SplashTimelineDurations {
            val intro = if (isWarmStart) 320 else 520
            val hold = if (isWarmStart) 360 else 1100
            val settle = if (isWarmStart) 120 else 220
            val exit = if (isWarmStart) 260 else 460
            return SplashTimelineDurations(introMs = intro, holdMs = hold, settleMs = settle, exitMs = exit)
        }
    }
}

class SplashCompletionGuard {
    private var completed = false

    fun completeOnce(): Boolean {
        if (completed) return false
        completed = true
        return true
    }
}
