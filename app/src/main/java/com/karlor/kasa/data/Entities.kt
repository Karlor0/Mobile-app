package com.karlor.kasa.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxType { INCOME, EXPENSE }

/** Origin of an obligation that a payment / transaction can be linked to. */
enum class ObligationKind { RECURRING, CARD, LOAN }

/**
 * All monetary values are stored as **kuruş** (Long, 1 TL = 100) to avoid floating point drift.
 * Dates are stored as epoch-day (Long), months as "ym" = year * 12 + (month - 1).
 */
@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index(value = ["linkKind", "linkId", "linkYm"])],
)
data class TxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val category: String,
    val title: String,
    val note: String = "",
    val date: Long,
    val linkKind: ObligationKind? = null,
    val linkId: Long? = null,
    val linkYm: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Fixed monthly expense (rent, bills, subscriptions…) or fixed income (salary). */
@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val amount: Long,
    val dueDay: Int,
    val isIncome: Boolean = false,
    val provider: String = "",
    val startYm: Int,
    val active: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bank: String,
    val name: String,
    val last4: String = "",
    val creditLimit: Long,
    val currentDebt: Long,
    val statementDebt: Long,
    val minPayment: Long,
    val statementDay: Int,
    val dueDay: Int,
    val colorIndex: Int = 0,
    val startYm: Int,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bank: String,
    val name: String,
    val loanType: String,
    val principal: Long,
    val installment: Long,
    val totalInstallments: Int,
    /** Installments already paid before the loan was added to the app. */
    val initialPaid: Int,
    /** Month of installment #1. */
    val startYm: Int,
    val dueDay: Int,
    /** Monthly interest rate in percent, informational only. */
    val interestRate: Double = 0.0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["kind", "refId", "ym"], unique = true)],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ObligationKind,
    val refId: Long,
    val ym: Int,
    val amount: Long,
    val paidAt: Long = System.currentTimeMillis(),
    val txId: Long? = null,
)
