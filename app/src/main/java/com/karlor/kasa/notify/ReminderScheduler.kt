package com.karlor.kasa.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val PERIODIC = "kasa_reminders"
    private const val NOW = "kasa_reminders_now"

    /** 30 minute cadence: fine enough for the 3-hour "due today" interval, light on battery. */
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.MINUTES, 10, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun runNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }
}
