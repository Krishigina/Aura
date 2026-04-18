package com.aura.app

import android.app.Application
import com.aura.core.notifications.AndroidProfileNotificationsBridge
import com.aura.core.data.repository.session.SessionStorage
import com.aura.core.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidProfileNotificationsBridge.init(this)
        SessionStorage.init(this)
        startKoin {
            androidContext(this@AuraApplication)
            modules(appModules)
        }
    }
}
