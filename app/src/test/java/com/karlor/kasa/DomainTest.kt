package com.karlor.kasa

import com.karlor.kasa.data.CardEntity
import com.karlor.kasa.data.LoanEntity
import com.karlor.kasa.data.ObligationKind
import com.karlor.kasa.data.PaymentEntity
import com.karlor.kasa.data.RecurringEntity
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.ObligationEngine
import com.karlor.kasa.domain.ReminderPolicy
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MoneyTest {
    @Test fun parsesTurkishAndInternationalNotation() {
        assertEquals(123456L, Money.parse("1.234,56"))
        assertEquals(123456L, Money.parse("1234,56"))
        assertEquals(123456L, Money.parse("1234.56"))
        assertEquals(123400L, Money.parse("1.234"))
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(50000L, Money.parse("₺500"))
        assertEquals(100000000L, Money.parse("1.000.000"))
        assertEquals(123456L, Money.parse("1,234.56"))
    }

    @Test fun rejectsInvalid() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1,2,3"))
        assertNull(Money.parse("1.2345"))
        assertNull(Money.parse("99999999999999999999"))
    }

    @Test fun formats() {
        assertEquals("₺1.234,56", Money.format(123456))
        assertEquals("−₺5,00", Money.format(-500))
        assertEquals("+₺5,00", Money.format(500, signed = true))
        assertEquals("₺ •••••", Money.format(500, hidden = true))
        assertEquals("1234,5", Money.toInput(123450))
    }
}

class MonthsTest {
    @Test fun clampsDueDayToMonthLength() {
        val feb = Months.of(LocalDate.of(2026, 2, 1))
        assertEquals(LocalDate.of(2026, 2, 28), Months.dueDate(feb, 31))
        assertEquals(LocalDate.of(2026, 12, 31), Months.dueDate(Months.of(LocalDate.of(2026, 12, 5)), 31))
        assertEquals(LocalDate.of(2027, 1, 1), Months.firstDay(Months.of(LocalDate.of(2026, 12, 5)) + 1))
    }
}

class ObligationEngineTest {
    private val ym = Months.of(LocalDate.of(2026, 11, 1))
    private val old = 0L // created long ago

    private val rent = RecurringEntity(id = 1, name = "Kira", category = "Kira", amount = 2_000_000, dueDay = 5, startYm = ym - 3, createdAt = old)
    private val salary = RecurringEntity(id = 2, name = "Maaş", category = "Maaş", amount = 5_000_000, dueDay = 1, isIncome = true, startYm = ym - 3, createdAt = old)
    private val card = CardEntity(id = 1, bank = "Akbank", name = "Axess", creditLimit = 5_000_000, currentDebt = 1_200_000, statementDebt = 800_000, minPayment = 200_000, statementDay = 1, dueDay = 11, startYm = ym - 3, createdAt = old)
    private val loan = LoanEntity(id = 1, bank = "Garanti BBVA", name = "Araba", loanType = "Taşıt Kredisi", principal = 30_000_000, installment = 1_500_000, totalInstallments = 24, initialPaid = 10, startYm = ym - 10, dueDay = 20)

    @Test fun monthContainsAllKinds() {
        val o = ObligationEngine.forMonth(ym, listOf(rent, salary), listOf(card), listOf(loan), emptyList())
        assertEquals(4, o.size)
        val l = o.first { it.kind == ObligationKind.LOAN }
        assertEquals("11/24", l.installmentLabel)
    }

    @Test fun loanInstallmentsBeforeTrackingAreHidden() {
        val o = ObligationEngine.forMonth(ym - 1, emptyList(), emptyList(), listOf(loan), emptyList())
        assertTrue(o.isEmpty())
        val afterEnd = ObligationEngine.forMonth(ym + 14, emptyList(), emptyList(), listOf(loan), emptyList())
        assertTrue(afterEnd.isEmpty())
    }

    @Test fun loanProgressCountsPayments() {
        val p = listOf(PaymentEntity(kind = ObligationKind.LOAN, refId = 1, ym = ym, amount = 1_500_000))
        val prog = ObligationEngine.loanProgress(loan, p)
        assertEquals(11, prog.paid)
        assertEquals(13, prog.remaining)
        assertEquals(13L * 1_500_000, prog.remainingAmount)
    }

    @Test fun pendingIncludesOverdueAndUpcomingButNotPaidOrIncome() {
        val today = LocalDate.of(2026, 11, 8)
        val payments = listOf(PaymentEntity(kind = ObligationKind.CARD, refId = 1, ym = ym, amount = 800_000))
        val pending = ObligationEngine.pending(today, 7, listOf(rent, salary), listOf(card), listOf(loan), payments)
        // Rent (5 Nov) overdue + previous months' rent & card & loan; paid card excluded; salary excluded.
        assertTrue(pending.any { it.kind == ObligationKind.RECURRING && it.ym == ym && it.daysLeft(today) == -3L })
        assertFalse(pending.any { it.isIncome })
        assertFalse(pending.any { it.kind == ObligationKind.CARD && it.ym == ym })
        // Loan due 20 Nov is 12 days away -> outside 7 day horizon.
        assertFalse(pending.any { it.kind == ObligationKind.LOAN && it.ym == ym })
    }

    @Test fun exposureGroupsByBank() {
        val e = ObligationEngine.exposures(listOf(card), listOf(loan), emptyList())
        assertEquals(2, e.size)
        assertEquals("Garanti BBVA", e.first().bank) // larger total first
        assertEquals(14L * 1_500_000, e.first().loanDebt)
        assertEquals(1_200_000L, e.last().cardDebt)
    }
}

class ReminderPolicyTest {
    private val h = 3_600_000L

    @Test fun cadenceEscalatesTowardsDueDate() {
        assertNull(ReminderPolicy.intervalMillis(10, 7))
        assertEquals(24 * h, ReminderPolicy.intervalMillis(6, 7))
        assertEquals(12 * h, ReminderPolicy.intervalMillis(3, 7))
        assertEquals(6 * h, ReminderPolicy.intervalMillis(1, 7))
        assertEquals(3 * h, ReminderPolicy.intervalMillis(0, 7))
        assertEquals(4 * h, ReminderPolicy.intervalMillis(-2, 7))
    }

    @Test fun respectsIntervalAndSnooze() {
        val now = 100 * h
        assertTrue(ReminderPolicy.shouldNotify(now, null, null, 0, 7))
        assertFalse(ReminderPolicy.shouldNotify(now, now - h, null, 0, 7))
        assertTrue(ReminderPolicy.shouldNotify(now, now - 3 * h, null, 0, 7))
        assertFalse(ReminderPolicy.shouldNotify(now, null, now + h, 0, 7))
        // snooze ended after last notification -> notify again
        assertTrue(ReminderPolicy.shouldNotify(now, now - 2 * h, now - 1, 5, 7))
    }

    @Test fun quietHoursWrapMidnight() {
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(23, 30), true))
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(3, 0), true))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(9, 0), true))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(3, 0), false))
    }
}
