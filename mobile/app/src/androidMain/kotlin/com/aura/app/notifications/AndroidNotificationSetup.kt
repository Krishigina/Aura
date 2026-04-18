package com.aura.app.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import com.aura.core.notifications.AndroidProfileNotificationsBridge

private const val POST_NOTIFICATIONS_REQUEST_CODE = 1001

internal fun ComponentActivity.configureAndroidNotifications() {
    AndroidProfileNotificationsBridge.init(applicationContext)
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQUEST_CODE)
    }
    JournalNotificationScheduler.ensureChannel(this)
}
