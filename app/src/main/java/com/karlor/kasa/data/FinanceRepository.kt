package com.karlor.kasa.data

import androidx.room.withTransaction
import com.karlor.kasa.domain.Catalog
import com.karlor.kasa.domain.Obligation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Single entry point for all persistence. All multi-row mutations are atomic. */
class FinanceRepository(private val db: AppDatabase) {

    fun transactions(from: LocalDate, to: LocalDate): Flow<List<TxEntity>> =
        db.tx().observeRange(from.toEpochDay(), to.toEpochDay())

    val recurring: Flow<List<RecurringEntity>> = db.recurring().observeAll()
    val cards: Flow<List<CardEntity>> = db.cards().observeAll()
    val loans: Flow<List<LoanEntity>> = db.loans().observeAll()
    val payments: Flow<List<PaymentEntity>> = db.payments().observeAll()

    data class Snapshot(
        val recurring: List<RecurringEntity>,
        val cards: List<CardEntity>,
        val loans: List<LoanEntity>,
        val payments: List<PaymentEntity>,
    )

    suspend fun snapshot(): Snapshot = Snapshot(
        db.recurring().getAll(), db.cards().getAll(), db.loans().getAll(), db.payments().getAll(),
    )

    suspend fun allTransactions(): List<TxEntity> = db.tx().getAll()

    // ---- Transactions -------------------------------------------------------------------------

    suspend fun saveTransaction(tx: TxEntity) {
        require(tx.amount > 0) { "amount must be positive" }
        if (tx.id == 0L) db.tx().insert(tx) else db.tx().update(tx)
    }

    /** Deleting a payment-linked transaction also reverts the payment (and card balances). */
    suspend fun deleteTransaction(id: Long) = db.withTransaction {
        val tx = db.tx().get(id) ?: return@withTransaction
        if (tx.linkKind != null && tx.linkId != null && tx.linkYm != null) {
            revertPaymentInternal(tx.linkKind, tx.linkId, tx.linkYm)
        } else {
            db.tx().delete(id)
        }
    }

    // ---- Obligation definitions -----------------------------------------------------------------

    suspend fun saveRecurring(item: RecurringEntity) = db.recurring().upsert(item)
    suspend fun saveCard(item: CardEntity) = db.cards().upsert(item)
    suspend fun saveLoan(item: LoanEntity) = db.loans().upsert(item)

    suspend fun deleteRecurring(id: Long) = db.withTransaction {
        db.payments().deleteFor(ObligationKind.RECURRING, id); db.recurring().delete(id)
    }

    suspend fun deleteCard(id: Long) = db.withTransaction {
        db.payments().deleteFor(ObligationKind.CARD, id); db.cards().delete(id)
    }

    suspend fun deleteLoan(id: Long) = db.withTransaction {
        db.payments().deleteFor(ObligationKind.LOAN, id); db.loans().delete(id)
    }

    // ---- Payments -----------------------------------------------------------------------------

    /**
     * Marks an obligation instance as paid (or received for income). Idempotent: a second call for
     * the same (kind, ref, month) is a no-op, which protects against double taps / notification races.
     */
    suspend fun markPaid(
        kind: ObligationKind,
        refId: Long,
        ym: Int,
        amount: Long,
        date: LocalDate = LocalDate.now(),
    ): Boolean = db.withTransaction {
        require(amount >= 0) { "amount must not be negative" }
        if (db.payments().find(kind, refId, ym) != null) return@withTransaction false

        var title: String
        var category: String
        var type = TxType.EXPENSE
        when (kind) {
            ObligationKind.RECURRING -> {
                val r = db.recurring().get(refId) ?: return@withTransaction false
                title = r.name; category = r.category
                if (r.isIncome) type = TxType.INCOME
            }
            ObligationKind.CARD -> {
                val c = db.cards().get(refId) ?: return@withTransaction false
                title = "${c.bank} · ${c.name} ödemesi"; category = Catalog.CARD_PAYMENT
                db.cards().upsert(
                    c.copy(
                        statementDebt = (c.statementDebt - amount).coerceAtLeast(0),
                        currentDebt = (c.currentDebt - amount).coerceAtLeast(0),
                    ),
                )
            }
            ObligationKind.LOAN -> {
                val l = db.loans().get(refId) ?: return@withTransaction false
                val idx = ym - l.startYm + 1
                title = "${l.bank} · ${l.name} ($idx/${l.totalInstallments}. taksit)"; category = Catalog.LOAN_PAYMENT
            }
        }
        val txId = if (amount > 0) {
            db.tx().insert(
                TxEntity(
                    type = type, amount = amount, category = category, title = title,
                    date = date.toEpochDay(), linkKind = kind, linkId = refId, linkYm = ym,
                ),
            )
        } else null
        db.payments().insert(PaymentEntity(kind = kind, refId = refId, ym = ym, amount = amount, txId = txId))
        true
    }

    suspend fun markPaid(o: Obligation, amount: Long, date: LocalDate = LocalDate.now()) =
        markPaid(o.kind, o.refId, o.ym, amount, date)

    suspend fun revertPayment(kind: ObligationKind, refId: Long, ym: Int) = db.withTransaction {
        revertPaymentInternal(kind, refId, ym)
    }

    private suspend fun revertPaymentInternal(kind: ObligationKind, refId: Long, ym: Int) {
        val p = db.payments().find(kind, refId, ym) ?: return
        if (kind == ObligationKind.CARD) {
            db.cards().get(refId)?.let { c ->
                db.cards().upsert(
                    c.copy(
                        statementDebt = c.statementDebt + p.amount,
                        currentDebt = c.currentDebt + p.amount,
                    ),
                )
            }
        }
        p.txId?.let { db.tx().delete(it) }
        db.payments().delete(p.id)
    }

    suspend fun wipe() = db.withTransaction { db.clearAllTables() }
}
