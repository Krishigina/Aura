package com.aura.app

import android.app.Application
import com.aura.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AuraApplication)
            modules(appModule)
        }
    }
}