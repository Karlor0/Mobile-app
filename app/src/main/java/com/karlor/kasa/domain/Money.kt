package com.karlor.kasa.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Money helpers. Values are kuruş (1 TL = 100 kuruş). */
object Money {
    private val TR = Locale("tr", "TR")
    /** Upper bound (1 trillion TL) – rejects absurd / overflowing input. */
    private const val MAX_KURUS = 100_000_000_000_000L

    private val formatter = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", DecimalFormatSymbols(TR))
    }
    private val formatterNoDecimals = ThreadLocal.withInitial {
        DecimalFormat("#,##0", DecimalFormatSymbols(TR))
    }

    fun format(kurus: Long, hidden: Boolean = false, signed: Boolean = false, decimals: Boolean = true): String {
        if (hidden) return "₺ •••••"
        val sign = when {
            kurus < 0 -> "−"
            signed && kurus > 0 -> "+"
            else -> ""
        }
        val value = BigDecimal.valueOf(abs(kurus), 2)
        val body = if (decimals) formatter.get()!!.format(value) else formatterNoDecimals.get()!!.format(value)
        return "$sign₺$body"
    }

    /** Compact representation for chart labels: ₺12,4B / ₺1,2Mn. */
    fun compact(kurus: Long, hidden: Boolean = false): String {
        if (hidden) return "₺ •••"
        val tl = abs(kurus) / 100.0
        val sign = if (kurus < 0) "−" else ""
        return when {
            tl >= 1_000_000 -> "$sign₺${"%.1f".format(TR, tl / 1_000_000)}Mn"
            tl >= 10_000 -> "$sign₺${"%.1f".format(TR, tl / 1_000)}B"
            else -> sign + format(abs(kurus), decimals = false)
        }
    }

    /** Value suitable for pre-filling an input field ("1234,5"). */
    fun toInput(kurus: Long): String {
        if (kurus == 0L) return ""
        return BigDecimal.valueOf(kurus, 2).stripTrailingZeros().toPlainString().replace('.', ',')
    }

    /**
     * Parses user input in Turkish or international notation:
     * "1.234,56", "1234,56", "1234.56", "1 234", "₺500".
     * Returns null for invalid, negative or out-of-range values.
     */
    fun parse(raw: String): Long? {
        var s = raw.trim().replace("₺", "").replace("TL", "", ignoreCase = true)
            .replace(" ", "").replace(" ", "")
        if (s.isEmpty()) return null
        if (!s.all { it.isDigit() || it == '.' || it == ',' }) return null
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        s = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) s.replace(".", "").replace(',', '.')
                else s.replace(",", "")
            }
            lastComma >= 0 -> {
                if (s.count { it == ',' } > 1) return null
                s.replace(',', '.')
            }
            lastDot >= 0 -> {
                val dots = s.count { it == '.' }
                val decimalsAfter = s.length - lastDot - 1
                if (dots > 1 || decimalsAfter == 3) s.replace(".", "") else s
            }
            else -> s
        }
        if (s.startsWith('.') || s.endsWith('.')) s = s.trim('.')
        if (s.isEmpty()) return null
        return try {
            val bd = BigDecimal(s)
            if (bd.scale() > 2 && bd.stripTrailingZeros().scale() > 2) return null
            val k = bd.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
            if (k < 0 || k > MAX_KURUS) null else k
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }
}
