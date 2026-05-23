package com.aura.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.core.domain.model.ReminderFrequency
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ProfileReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val hasNotificationPermission: Boolean
        get() = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override suspend fun doWork(): Result {
        val domain = inputData.getString(KEY_DOMAIN) ?: return Result.success()
        val forceImmediate = inputData.getBoolean(KEY_FORCE_IMMEDIATE, false)
        if (!hasNotificationPermission) {
            println("[ProfileNotifications][worker] Skip $domain: POST_NOTIFICATIONS not granted")
            return Result.success()
        }
        if (inputData.getBoolean(KEY_DISABLE_ALL, false)) {
            println("[ProfileNotifications][worker] Skip $domain: disable_all=true")
            return Result.success()
        }

        val frequency = inputData.getString(KEY_FREQUENCY)
            ?.let { runCatching { ReminderFrequency.valueOf(it) }.getOrNull() }
            ?: ReminderFrequency.NONE

        if (frequency == ReminderFrequency.NONE) {
            println("[ProfileNotifications][worker] Skip $domain: frequency none")
            return Result.success()
        }

        val now = LocalDateTime.now()
        if (!isDueNow(now, frequency)) {
            println("[ProfileNotifications][worker] Skip $domain: not due now")
            return Result.success()
        }
        if (!forceImmediate && alreadySentToday(now.toLocalDate(), domain)) {
            println("[ProfileNotifications][worker] Skip $domain: already sent today")
            return Result.success()
        }

        val sent = if (domain == "routine") {
            JournalNotificationScheduler.showRoutineReminderNow(applicationContext)
        } else if (domain == "journal") {
            JournalNotificationScheduler.showJournalReminderNow(applicationContext)
        } else {
            println("[ProfileNotifications][worker] Skip: unknown domain $domain")
            false
        }

        if (sent) {
            markSent(now.toLocalDate(), domain)
        } else {
            println("[ProfileNotifications][worker] Skip $domain: notification not posted")
        }
        return Result.success()
    }

    private fun isDueNow(now: LocalDateTime, frequency: ReminderFrequency): Boolean {
        val dueTime = parseReminderTime(inputData.getString(KEY_REMINDER_TIME))
        if (now.toLocalTime().isBefore(dueTime)) return false

        return when (frequency) {
            ReminderFrequency.DAILY -> true
            ReminderFrequency.WEEKLY -> {
                val configured = inputData.getInt(KEY_WEEKDAY, -1)
                ProfileReminderScheduleLogic.isWeeklyDue(configured, now.dayOfWeek.value)
            }
            ReminderFrequency.MONTHLY -> {
                val configured = inputData.getInt(KEY_MONTH_DAY, -1)
                ProfileReminderScheduleLogic.isMonthlyDue(now.toLocalDate(), configured)
            }
            ReminderFrequency.NONE -> false
        }
    }

    private fun parseReminderTime(value: String?): LocalTime {
        if (value.isNullOrBlank()) return LocalTime.of(9, 0)
        return runCatching { LocalTime.parse(value) }
            .recoverCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm")) }
            .getOrDefault(LocalTime.of(9, 0))
    }

    private fun alreadySentToday(date: LocalDate, domain: String): Boolean {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sentDate = prefs.getString(sentDateKey(domain), null)
        return sentDate == date.toString()
    }

    private fun markSent(date: LocalDate, domain: String) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(sentDateKey(domain), date.toString()).apply()
    }

    private fun sentDateKey(domain: String): String = ProfileReminderScheduleLogic.sentDateKey(domain)

    companion object {
        const val KEY_DOMAIN = "domain"
        const val KEY_DISABLE_ALL = "disable_all"
        const val KEY_FREQUENCY = "frequency"
        const val KEY_WEEKDAY = "weekday"
        const val KEY_MONTH_DAY = "month_day"
        const val KEY_REMINDER_TIME = "reminder_time"
        const val KEY_FORCE_IMMEDIATE = "force_immediate"

        private const val PREFS_NAME = "profile_notification_worker"
    }
}
