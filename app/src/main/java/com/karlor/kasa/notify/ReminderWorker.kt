package com.karlor.kasa.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.karlor.kasa.domain.ObligationEngine
import com.karlor.kasa.domain.ReminderPolicy
import com.karlor.kasa.kasa
import java.time.LocalDate
import java.time.LocalTime

/** Runs periodically; decides per obligation whether a (repeat) reminder is due. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext.kasa
        val settings = app.settings.state.value
        if (!settings.notificationsEnabled) return Result.success()

        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val s = app.repository.snapshot()
        val pending = ObligationEngine.pending(today, settings.leadDays, s.recurring, s.cards, s.loans, s.payments)
        app.settings.pruneReminders(pending.map { it.key }.toSet())

        // Overdue items are allowed through quiet hours only if they were never notified.
        val quiet = ReminderPolicy.isQuiet(LocalTime.now(), settings.quietHours)
        var posted = 0
        for (o in pending) {
            val days = o.daysLeft(today)
            val last = app.settings.lastNotified(o.key)
            if (quiet && last != null) continue
            if (quiet && days >= 0) continue
            if (ReminderPolicy.shouldNotify(now, last, app.settings.snoozedUntil(o.key), days, settings.leadDays)) {
                Notifier.post(applicationContext, o, days, settings.hideAmounts)
                app.settings.setLastNotified(o.key, now)
                posted++
            }
        }
        if (posted > 0) Notifier.postSummary(applicationContext, pending.size, pending.sumOf { it.amount }, settings.hideAmounts)
        return Result.success()
    }
}
