package com.karlor.kasa.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karlor.kasa.data.*
import com.karlor.kasa.domain.BankExposure
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.domain.ObligationEngine
import com.karlor.kasa.kasa
import com.karlor.kasa.notify.Notifier
import com.karlor.kasa.notify.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate

data class Definitions(
    val recurring: List<RecurringEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
)

data class UiState(
    val loaded: Boolean = false,
    val ym: Int = Months.current(),
    val today: LocalDate = LocalDate.now(),
    val txs: List<TxEntity> = emptyList(),
    val defs: Definitions = Definitions(),
    val obligations: List<Obligation> = emptyList(),
    val pending: List<Obligation> = emptyList(),
    val income: Long = 0,
    val expense: Long = 0,
    val unpaidObligations: Long = 0,
    val paidObligations: Long = 0,
    val unpaidIncome: Long = 0,
    val categoryTotals: List<Pair<String, Long>> = emptyList(),
    val exposures: List<BankExposure> = emptyList(),
) {
    val balance: Long get() = income - expense
    val forecast: Long get() = balance + unpaidIncome - unpaidObligations
    val totalDebt: Long get() = exposures.sumOf { it.total }
    val monthlyBurden: Long get() = exposures.sumOf { it.monthlyBurden }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.kasa
    private val repo = ctx.repository
    val settings: StateFlow<AppSettings> = ctx.settings.state

    private val ym = MutableStateFlow(Months.current())

    private val defs: Flow<Definitions> =
        combine(repo.recurring, repo.cards, repo.loans, repo.payments) { r, c, l, p -> Definitions(r, c, l, p) }

    private val monthTx: Flow<List<TxEntity>> = ym.flatMapLatest { m ->
        repo.transactions(Months.firstDay(m), Months.lastDay(m))
    }

    val state: StateFlow<UiState> = combine(ym, monthTx, defs) { m, txs, d -> build(m, txs, d) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun build(m: Int, txs: List<TxEntity>, d: Definitions): UiState {
        val today = LocalDate.now()
        val obligations = ObligationEngine.forMonth(m, d.recurring, d.cards, d.loans, d.payments)
        val expenses = obligations.filter { !it.isIncome }
        val income = txs.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        val expense = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
        val cats = txs.filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .map { (k, v) -> k to v.sumOf { it.amount } }
            .sortedByDescending { it.second }
        return UiState(
            loaded = true,
            ym = m,
            today = today,
            txs = txs,
            defs = d,
            obligations = obligations,
            pending = ObligationEngine.pending(today, 31, d.recurring, d.cards, d.loans, d.payments),
            income = income,
            expense = expense,
            unpaidObligations = expenses.filter { !it.paid && !it.nothingDue }.sumOf { it.amount },
            paidObligations = expenses.filter { it.paid }.sumOf { it.payment?.amount ?: 0 },
            unpaidIncome = obligations.filter { it.isIncome && !it.paid }.sumOf { it.amount },
            categoryTotals = cats,
            exposures = ObligationEngine.exposures(d.cards, d.loans, d.payments),
        )
    }

    fun setMonth(m: Int) { ym.value = m }

    private fun io(block: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { block() }
    }

    fun saveTx(tx: TxEntity) = io { repo.saveTransaction(tx) }
    fun deleteTx(id: Long) = io { repo.deleteTransaction(id) }

    fun markPaid(o: Obligation, amount: Long, date: LocalDate) = io {
        repo.markPaid(o, amount, date)
        Notifier.cancelFor(ctx, o.key)
    }

    fun revert(o: Obligation) = io { repo.revertPayment(o.kind, o.refId, o.ym) }

    fun saveRecurring(r: RecurringEntity) = io { repo.saveRecurring(r); ReminderScheduler.runNow(ctx) }
    fun deleteRecurring(id: Long) = io { repo.deleteRecurring(id) }
    fun saveCard(c: CardEntity) = io { repo.saveCard(c); ReminderScheduler.runNow(ctx) }
    fun deleteCard(id: Long) = io { repo.deleteCard(id) }
    fun saveLoan(l: LoanEntity) = io { repo.saveLoan(l); ReminderScheduler.runNow(ctx) }
    fun deleteLoan(id: Long) = io { repo.deleteLoan(id) }

    fun updateSettings(t: (AppSettings) -> AppSettings) {
        ctx.settings.update(t)
        ReminderScheduler.runNow(ctx)
    }

    fun testReminders() = ReminderScheduler.runNow(ctx)

    fun wipe() = io { repo.wipe(); ctx.settings.clearReminders() }

    /** Writes all transactions as CSV (UTF-8 with BOM for Excel), neutralising formula injection. */
    suspend fun exportCsv(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val rows = repo.allTransactions()
            ctx.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { w ->
                w.write("﻿")
                w.write("Tarih;Tür;Kategori;Başlık;Tutar (TL);Not\n")
                for (t in rows) {
                    val cols = listOf(
                        LocalDate.ofEpochDay(t.date).toString(),
                        if (t.type == TxType.INCOME) "Gelir" else "Gider",
                        t.category, t.title,
                        BigDecimal.valueOf(if (t.type == TxType.INCOME) t.amount else -t.amount, 2).toPlainString().replace('.', ','),
                        t.note,
                    )
                    w.write(cols.joinToString(";") { csvCell(it) })
                    w.write("\n")
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    private fun csvCell(raw: String): String {
        var v = raw.replace("\r", " ").replace("\n", " ")
        // CWE-1236: prevent spreadsheet formula execution.
        if (v.isNotEmpty() && v[0] in "=+-@\t" && v.toBigDecimalOrNull() == null && !v.matches(Regex("^-?[0-9]+,[0-9]{2}$"))) v = "'$v"
        return "\"" + v.replace("\"", "\"\"") + "\""
    }
}
