package com.karlor.kasa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TxDao {
    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeRange(from: Long, to: Long): Flow<List<TxEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<TxEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TxEntity?

    @Insert
    suspend fun insert(tx: TxEntity): Long

    @Update
    suspend fun update(tx: TxEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY isIncome DESC, dueDay ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring")
    suspend fun getAll(): List<RecurringEntity>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun get(id: Long): RecurringEntity?

    @Upsert
    suspend fun upsert(item: RecurringEntity): Long

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY bank COLLATE NOCASE, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun get(id: Long): CardEntity?

    @Upsert
    suspend fun upsert(item: CardEntity): Long

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY bank COLLATE NOCASE, name COLLATE NOCASE")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans")
    suspend fun getAll(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun get(id: Long): LoanEntity?

    @Upsert
    suspend fun upsert(item: LoanEntity): Long

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments")
    suspend fun getAll(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE kind = :kind AND refId = :refId AND ym = :ym LIMIT 1")
    suspend fun find(kind: ObligationKind, refId: Long, ym: Int): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(p: PaymentEntity): Long

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM payments WHERE kind = :kind AND refId = :refId")
    suspend fun deleteFor(kind: ObligationKind, refId: Long)

    @Query("DELETE FROM payments WHERE txId = :txId")
    suspend fun deleteByTx(txId: Long)
}
