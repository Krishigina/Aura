package com.aura.core.notifications

import android.content.Context
import com.aura.core.domain.model.ProfileNotificationSettings

object AndroidProfileNotificationsBridge {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun schedule(settings: ProfileNotificationSettings) {
        val context = appContext
        if (context == null) {
            return
        }
        runCatching {
            val schedulerClass = Class.forName("com.aura.app.notifications.ProfileNotificationsScheduler")
            val schedulerInstance = schedulerClass.getField("INSTANCE").get(null)
            val syncMethod = schedulerClass.getMethod(
                "sync",
                Context::class.java,
                ProfileNotificationSettings::class.java,
            )
            syncMethod.invoke(schedulerInstance, context, settings)
        }
    }
}
