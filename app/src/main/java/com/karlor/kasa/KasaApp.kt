package com.karlor.kasa

import android.app.Application
import com.karlor.kasa.data.AppDatabase
import com.karlor.kasa.data.FinanceRepository
import com.karlor.kasa.data.SettingsStore
import com.karlor.kasa.notify.Notifier
import com.karlor.kasa.notify.ReminderScheduler

/** Manual DI container: tiny app, no need for a DI framework. */
class KasaApp : Application() {
    val database by lazy { AppDatabase.build(this) }
    val repository by lazy { FinanceRepository(database) }
    val settings by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        ReminderScheduler.schedule(this)
    }
}

val android.content.Context.kasa: KasaApp get() = applicationContext as KasaApp
