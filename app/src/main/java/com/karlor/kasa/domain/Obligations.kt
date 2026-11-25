package com.karlor.kasa.domain

import com.karlor.kasa.data.CardEntity
import com.karlor.kasa.data.LoanEntity
import com.karlor.kasa.data.ObligationKind
import com.karlor.kasa.data.PaymentEntity
import com.karlor.kasa.data.RecurringEntity
import java.time.LocalDate

/** One concrete due instance (e.g. "Kira – Kasım 2026"). */
data class Obligation(
    val kind: ObligationKind,
    val refId: Long,
    val ym: Int,
    val title: String,
    val subtitle: String,
    val category: String,
    val amount: Long,
    val minAmount: Long = 0,
    val dueDate: LocalDate,
    val isIncome: Boolean,
    val payment: PaymentEntity?,
    /** For loans: "5/36". */
    val installmentLabel: String? = null,
    /** True when there is nothing to pay (e.g. card without debt). */
    val nothingDue: Boolean = false,
) {
    val paid: Boolean get() = payment != null
    val key: String get() = "${kind.name}_${refId}_$ym"
    fun daysLeft(today: LocalDate): Long = Months.daysBetween(today, dueDate)
}

data class LoanProgress(val paid: Int, val total: Int, val remainingAmount: Long) {
    val remaining: Int get() = (total - paid).coerceAtLeast(0)
    val fraction: Float get() = if (total == 0) 0f else paid.toFloat() / total
}

data class BankExposure(
    val bank: String,
    val cardDebt: Long,
    val loanDebt: Long,
    val monthlyBurden: Long,
    val totalLimit: Long,
    val cards: List<CardEntity>,
    val loans: List<Pair<LoanEntity, LoanProgress>>,
) {
    val total: Long get() = cardDebt + loanDebt
}

/** Pure, side-effect-free business rules (unit-testable). */
object ObligationEngine {

    fun loanProgress(loan: LoanEntity, payments: List<PaymentEntity>): LoanProgress {
        val recorded = payments.count {
            it.kind == ObligationKind.LOAN && it.refId == loan.id &&
                (it.ym - loan.startYm) in loan.initialPaid until loan.totalInstallments
        }
        val paid = (loan.initialPaid + recorded).coerceAtMost(loan.totalInstallments)
        val remaining = (loan.totalInstallments - paid).coerceAtLeast(0)
        return LoanProgress(paid, loan.totalInstallments, safeMul(loan.installment, remaining.toLong()))
    }

    fun forMonth(
        ym: Int,
        recurring: List<RecurringEntity>,
        cards: List<CardEntity>,
        loans: List<LoanEntity>,
        payments: List<PaymentEntity>,
    ): List<Obligation> {
        val pay = payments.associateBy { Triple(it.kind, it.refId, it.ym) }
        val out = ArrayList<Obligation>()

        for (r in recurring) {
            if (!r.active || ym < r.startYm) continue
            out += Obligation(
                kind = ObligationKind.RECURRING, refId = r.id, ym = ym,
                title = r.name,
                subtitle = r.provider.ifBlank { r.category },
                category = r.category,
                amount = r.amount,
                dueDate = Months.dueDate(ym, r.dueDay),
                isIncome = r.isIncome,
                payment = pay[Triple(ObligationKind.RECURRING, r.id, ym)],
            )
        }

        for (c in cards) {
            if (!c.active || ym < c.startYm) continue
            val p = pay[Triple(ObligationKind.CARD, c.id, ym)]
            out += Obligation(
                kind = ObligationKind.CARD, refId = c.id, ym = ym,
                title = c.name,
                subtitle = c.bank + if (c.last4.isNotBlank()) " •••• ${c.last4}" else "",
                category = Catalog.CARD_PAYMENT,
                amount = c.statementDebt,
                minAmount = c.minPayment,
                dueDate = Months.dueDate(ym, c.dueDay),
                isIncome = false,
                payment = p,
                nothingDue = p == null && c.statementDebt <= 0 && c.currentDebt <= 0,
            )
        }

        for (l in loans) {
            if (!l.active) continue
            val idx = ym - l.startYm
            if (idx < 0 || idx >= l.totalInstallments) continue
            val p = pay[Triple(ObligationKind.LOAN, l.id, ym)]
            // Installments before the app tracked the loan are implicitly paid; hide them.
            if (idx < l.initialPaid && p == null) continue
            out += Obligation(
                kind = ObligationKind.LOAN, refId = l.id, ym = ym,
                title = l.name,
                subtitle = "${l.bank} · ${l.loanType}",
                category = Catalog.LOAN_PAYMENT,
                amount = l.installment,
                dueDate = Months.dueDate(ym, l.dueDay),
                isIncome = false,
                payment = p,
                installmentLabel = "${idx + 1}/${l.totalInstallments}",
            )
        }
        out.sortWith(compareBy<Obligation> { it.paid }.thenBy { it.dueDate }.thenBy { it.title })
        return out
    }

    /**
     * Unpaid expense obligations around [today]: overdue ones from the previous and current month
     * plus everything due within [horizonDays] (which may reach into next month).
     */
    fun pending(
        today: LocalDate,
        horizonDays: Int,
        recurring: List<RecurringEntity>,
        cards: List<CardEntity>,
        loans: List<LoanEntity>,
        payments: List<PaymentEntity>,
    ): List<Obligation> {
        val cur = Months.of(today)
        return (cur - 1..cur + 1)
            .flatMap { forMonth(it, recurring, cards, loans, payments) }
            .filter { !it.isIncome && !it.paid && !it.nothingDue }
            .filter { createdBeforeDue(it, recurring, cards, loans) }
            .filter { it.daysLeft(today) <= horizonDays }
            .sortedBy { it.dueDate }
    }

    /** Do not flag something as overdue for a month before the user even created it. */
    private fun createdBeforeDue(
        o: Obligation,
        recurring: List<RecurringEntity>,
        cards: List<CardEntity>,
        loans: List<LoanEntity>,
    ): Boolean {
        if (o.ym >= Months.current()) return true
        val createdAt = when (o.kind) {
            ObligationKind.RECURRING -> recurring.firstOrNull { it.id == o.refId }?.createdAt
            ObligationKind.CARD -> cards.firstOrNull { it.id == o.refId }?.createdAt
            ObligationKind.LOAN -> return true // past installments are user-declared
        } ?: return false
        val createdDay = java.time.Instant.ofEpochMilli(createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return !createdDay.isAfter(o.dueDate)
    }

    fun exposures(cards: List<CardEntity>, loans: List<LoanEntity>, payments: List<PaymentEntity>): List<BankExposure> {
        val banks = (cards.filter { it.active }.map { it.bank } + loans.filter { it.active }.map { it.bank }).distinct()
        return banks.map { bank ->
            val bc = cards.filter { it.active && it.bank == bank }
            val bl = loans.filter { it.active && it.bank == bank }.map { it to loanProgress(it, payments) }
            BankExposure(
                bank = bank,
                cardDebt = bc.sumOf { it.currentDebt },
                loanDebt = bl.sumOf { it.second.remainingAmount },
                monthlyBurden = bc.sumOf { it.minPayment } + bl.filter { it.second.remaining > 0 }.sumOf { it.first.installment },
                totalLimit = bc.sumOf { it.creditLimit },
                cards = bc,
                loans = bl,
            )
        }.sortedByDescending { it.total }
    }

    private fun safeMul(a: Long, b: Long): Long = try {
        Math.multiplyExact(a, b)
    } catch (_: ArithmeticException) {
        Long.MAX_VALUE
    }
}
