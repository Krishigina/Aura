package com.aura.core.platform

import android.os.Build

private const val AndroidEmulatorApiBaseUrl = "http://10.0.2.2:3002"
private const val AndroidPhysicalDeviceApiBaseUrl = "http://192.168.0.244:3002"

actual fun defaultApiBaseUrl(): String = if (isProbablyRunningOnEmulator()) {
    AndroidEmulatorApiBaseUrl
} else {
    AndroidPhysicalDeviceApiBaseUrl
}

private fun isProbablyRunningOnEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.lowercase().contains("emulator") ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
        Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
        Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
        Build.PRODUCT == "google_sdk"
}
