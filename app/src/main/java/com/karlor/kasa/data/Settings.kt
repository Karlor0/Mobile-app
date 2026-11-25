package com.karlor.kasa.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val leadDays: Int = 7,
    val quietHours: Boolean = true,
    val appLock: Boolean = false,
    val secureScreen: Boolean = false,
    val hideAmounts: Boolean = false,
    val themeMode: Int = 0, // 0 system, 1 light, 2 dark
)

/** Small key/value settings + reminder bookkeeping, backed by private SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kasa_settings", Context.MODE_PRIVATE)
    private val reminders: SharedPreferences =
        context.applicationContext.getSharedPreferences("kasa_reminders", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun read() = AppSettings(
        notificationsEnabled = prefs.getBoolean("notif", true),
        leadDays = prefs.getInt("lead", 7).coerceIn(1, 15),
        quietHours = prefs.getBoolean("quiet", true),
        appLock = prefs.getBoolean("lock", false),
        secureScreen = prefs.getBoolean("secure", false),
        hideAmounts = prefs.getBoolean("hide", false),
        themeMode = prefs.getInt("theme", 0).coerceIn(0, 2),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val n = transform(_state.value)
        prefs.edit()
            .putBoolean("notif", n.notificationsEnabled)
            .putInt("lead", n.leadDays.coerceIn(1, 15))
            .putBoolean("quiet", n.quietHours)
            .putBoolean("lock", n.appLock)
            .putBoolean("secure", n.secureScreen)
            .putBoolean("hide", n.hideAmounts)
            .putInt("theme", n.themeMode.coerceIn(0, 2))
            .apply()
        _state.value = n
    }

    fun lastNotified(key: String): Long? = reminders.getLong("n_$key", -1).takeIf { it > 0 }
    fun setLastNotified(key: String, at: Long) = reminders.edit().putLong("n_$key", at).apply()
    fun snoozedUntil(key: String): Long? = reminders.getLong("s_$key", -1).takeIf { it > 0 }
    fun snooze(key: String, until: Long) = reminders.edit().putLong("s_$key", until).apply()

    /** Drops bookkeeping for keys that are no longer pending to keep the file small. */
    fun pruneReminders(activeKeys: Set<String>) {
        val stale = reminders.all.keys.filter { it.length > 2 && it.substring(2) !in activeKeys }
        if (stale.isEmpty()) return
        reminders.edit().apply { stale.forEach { remove(it) } }.apply()
    }

    fun clearReminders() = reminders.edit().clear().apply()
}
