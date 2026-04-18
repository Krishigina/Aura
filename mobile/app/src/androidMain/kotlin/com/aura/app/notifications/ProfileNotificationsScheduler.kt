package com.aura.app.notifications

import android.content.Context
import androidx.annotation.Keep
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.domain.model.ReminderFrequency
import java.util.concurrent.TimeUnit

@Keep
object ProfileNotificationsScheduler {
    private const val DOMAIN_ROUTINE = "routine"
    private const val DOMAIN_JOURNAL = "journal"
    private const val ROUTINE_WORK = "profile_routine_notifications"
    private const val JOURNAL_WORK = "profile_journal_notifications"
    private const val ROUTINE_IMMEDIATE_WORK = "profile_routine_notifications_immediate"
    private const val JOURNAL_IMMEDIATE_WORK = "profile_journal_notifications_immediate"
    private const val WORKER_PREFS_NAME = "profile_notification_worker"

    @Keep
    fun sync(context: Context, settings: ProfileNotificationSettings) {
        val appContext = context.applicationContext
        JournalNotificationScheduler.ensureChannel(appContext)
        val workManager = WorkManager.getInstance(appContext)

        if (settings.disable_all) {
            workManager.cancelUniqueWork(ROUTINE_WORK)
            workManager.cancelUniqueWork(JOURNAL_WORK)
            workManager.cancelUniqueWork(ROUTINE_IMMEDIATE_WORK)
            workManager.cancelUniqueWork(JOURNAL_IMMEDIATE_WORK)
            return
        }

        syncRoutine(appContext, workManager, settings)
        syncJournal(appContext, workManager, settings)
    }

    private fun syncRoutine(appContext: Context, workManager: WorkManager, settings: ProfileNotificationSettings) {
        if (settings.routine.frequency == ReminderFrequency.NONE) {
            workManager.cancelUniqueWork(ROUTINE_WORK)
            workManager.cancelUniqueWork(ROUTINE_IMMEDIATE_WORK)
            return
        }

        workManager.enqueueUniquePeriodicWork(
            ROUTINE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            requestForDomain(DOMAIN_ROUTINE, settings)
        )
        clearSentTodayFlag(appContext, DOMAIN_ROUTINE)
        workManager.enqueueUniqueWork(
            ROUTINE_IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            immediateRequestForDomain(DOMAIN_ROUTINE, settings)
        )
    }

    private fun syncJournal(appContext: Context, workManager: WorkManager, settings: ProfileNotificationSettings) {
        if (settings.journal.frequency == ReminderFrequency.NONE) {
            workManager.cancelUniqueWork(JOURNAL_WORK)
            workManager.cancelUniqueWork(JOURNAL_IMMEDIATE_WORK)
            return
        }

        workManager.enqueueUniquePeriodicWork(
            JOURNAL_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            requestForDomain(DOMAIN_JOURNAL, settings)
        )
        clearSentTodayFlag(appContext, DOMAIN_JOURNAL)
        workManager.enqueueUniqueWork(
            JOURNAL_IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            immediateRequestForDomain(DOMAIN_JOURNAL, settings)
        )
    }

    private fun clearSentTodayFlag(context: Context, domain: String) {
        val key = ProfileReminderScheduleLogic.sentDateKey(domain)
        context.getSharedPreferences(WORKER_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }

    private fun requestForDomain(domain: String, settings: ProfileNotificationSettings) =
        PeriodicWorkRequestBuilder<ProfileReminderWorker>(15, TimeUnit.MINUTES)
            .setInputData(inputDataFor(domain, settings))
            .build()

    private fun immediateRequestForDomain(domain: String, settings: ProfileNotificationSettings) =
        OneTimeWorkRequestBuilder<ProfileReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putAll(inputDataFor(domain, settings))
                    .putBoolean(ProfileReminderWorker.KEY_FORCE_IMMEDIATE, true)
                    .build()
            )
            .build()

    private fun inputDataFor(domain: String, settings: ProfileNotificationSettings): Data {
        val preference = if (domain == DOMAIN_ROUTINE) settings.routine else settings.journal
        return Data.Builder()
            .putString(ProfileReminderWorker.KEY_DOMAIN, domain)
            .putBoolean(ProfileReminderWorker.KEY_DISABLE_ALL, settings.disable_all)
            .putString(ProfileReminderWorker.KEY_FREQUENCY, preference.frequency.name)
            .putInt(ProfileReminderWorker.KEY_WEEKDAY, preference.weekday ?: -1)
            .putInt(ProfileReminderWorker.KEY_MONTH_DAY, preference.month_day ?: -1)
            .putString(ProfileReminderWorker.KEY_REMINDER_TIME, preference.reminder_time ?: "")
            .build()
    }
}
