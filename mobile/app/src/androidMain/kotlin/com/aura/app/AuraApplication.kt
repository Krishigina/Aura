package com.aura.app

import android.app.Application
import android.os.Build
import com.aura.core.data.repository.AndroidTokenStorage
import com.aura.core.data.repository.TokenManager
import com.aura.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val hasExplicitUrl = !System.getenv("AURA_API_URL").isNullOrBlank() ||
            !System.getProperty("aura.api.url").isNullOrBlank()

        if (!hasExplicitUrl) {
            val defaultApiUrl = if (isProbablyEmulator()) {
                "http://10.0.2.2:3002"
            } else {
                "http://localhost:3002"
            }
            System.setProperty("aura.api.url", defaultApiUrl)
        }

        TokenManager.initialize(AndroidTokenStorage(this))

        startKoin {
            androidContext(this@AuraApplication)
            modules(appModule)
        }
    }

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val brand = Build.BRAND ?: ""
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for x86", ignoreCase = true) ||
            manufacturer.contains("Genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk", ignoreCase = true)
    }
}
