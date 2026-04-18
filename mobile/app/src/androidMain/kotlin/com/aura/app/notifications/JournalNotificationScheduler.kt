package com.aura.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import com.aura.app.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object JournalNotificationScheduler {
    private const val ROUTINE_CHANNEL_ID = "aura_routine_reminders"
    private const val JOURNAL_CHANNEL_ID = "aura_journal_reminders"
    private const val AURA_NOTIFICATION_COLOR = "#F9A8D4"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val routineChannel = NotificationChannel(
                ROUTINE_CHANNEL_ID,
                "Ритуал ухода",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val journalChannel = NotificationChannel(
                JOURNAL_CHANNEL_ID,
                "Журнал кожи",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(routineChannel)
            notificationManager.createNotificationChannel(journalChannel)
        }
    }

    fun showRoutineReminderNow(context: Context): Boolean {
        return showNotification(
            context = context,
            channelId = ROUTINE_CHANNEL_ID,
            id = 22001,
            title = "Пора к ритуалу ухода",
            text = "Откройте Aura и отметьте шаги на сегодня"
        )
    }

    fun showJournalReminderNow(context: Context): Boolean {
        return showNotification(
            context = context,
            channelId = JOURNAL_CHANNEL_ID,
            id = 22002,
            title = "Пора заполнить журнал кожи",
            text = "Добавьте сегодняшние данные в журнал"
        )
    }

    fun showReminderNow(context: Context, id: Int, title: String, text: String) {
        showNotification(context, JOURNAL_CHANNEL_ID, id, title, text)
    }

    private fun showNotification(context: Context, channelId: String, id: Int, title: String, text: String): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_aura)
            .setColor(Color.parseColor(AURA_NOTIFICATION_COLOR))
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        return true
    }
}
