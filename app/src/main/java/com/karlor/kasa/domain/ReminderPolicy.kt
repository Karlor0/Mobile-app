package com.karlor.kasa.domain

import java.time.LocalTime

/**
 * Escalating reminder cadence: the closer the due date, the more often the user is nudged
 * until they press "Ödedim". Overdue items keep nagging.
 */
object ReminderPolicy {
    private const val HOUR = 60L * 60 * 1000

    /** Minimum interval between two notifications for the same obligation, or null = don't notify. */
    fun intervalMillis(daysLeft: Long, leadDays: Int): Long? = when {
        daysLeft > leadDays -> null
        daysLeft >= 4 -> 24 * HOUR
        daysLeft >= 2 -> 12 * HOUR
        daysLeft == 1L -> 6 * HOUR
        daysLeft == 0L -> 3 * HOUR
        else -> 4 * HOUR // overdue
    }

    fun isQuiet(now: LocalTime, enabled: Boolean, startHour: Int = 23, endHour: Int = 8): Boolean {
        if (!enabled) return false
        val h = now.hour
        return if (startHour > endHour) h >= startHour || h < endHour else h in startHour until endHour
    }

    fun shouldNotify(
        nowMillis: Long,
        lastNotifiedMillis: Long?,
        snoozedUntilMillis: Long?,
        daysLeft: Long,
        leadDays: Int,
    ): Boolean {
        val interval = intervalMillis(daysLeft, leadDays) ?: return false
        if (snoozedUntilMillis != null && nowMillis < snoozedUntilMillis) return false
        if (lastNotifiedMillis == null) return true
        // A finished snooze always re-notifies; otherwise respect the cadence (with 5 min tolerance
        // because WorkManager periodic runs drift).
        if (snoozedUntilMillis != null && lastNotifiedMillis < snoozedUntilMillis) return true
        return nowMillis - lastNotifiedMillis >= interval - 5 * 60 * 1000
    }
}
