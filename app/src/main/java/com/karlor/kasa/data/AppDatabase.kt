package com.karlor.kasa.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TxEntity::class, RecurringEntity::class, CardEntity::class, LoanEntity::class, PaymentEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tx(): TxDao
    abstract fun recurring(): RecurringDao
    abstract fun cards(): CardDao
    abstract fun loans(): LoanDao
    abstract fun payments(): PaymentDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kasa.db")
                .build()
    }
}
