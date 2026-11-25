package com.karlor.kasa.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** "ym" = year * 12 + (month - 1): a compact, sortable month key. */
object Months {
    val TR: Locale = Locale("tr", "TR")
    private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", TR)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", TR)
    private val dayMonthWeekday = DateTimeFormatter.ofPattern("d MMMM, EEEE", TR)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)
    private val shortMonth = DateTimeFormatter.ofPattern("LLL", TR)

    fun of(date: LocalDate): Int = date.year * 12 + (date.monthValue - 1)
    fun current(): Int = of(LocalDate.now())
    fun toYearMonth(ym: Int): YearMonth = YearMonth.of(Math.floorDiv(ym, 12), Math.floorMod(ym, 12) + 1)

    fun firstDay(ym: Int): LocalDate = toYearMonth(ym).atDay(1)
    fun lastDay(ym: Int): LocalDate = toYearMonth(ym).atEndOfMonth()

    /** Due date for a day-of-month, clamped to the month length (31 → 28/29/30). */
    fun dueDate(ym: Int, day: Int): LocalDate {
        val m = toYearMonth(ym)
        return m.atDay(day.coerceIn(1, m.lengthOfMonth()))
    }

    fun label(ym: Int): String = toYearMonth(ym).format(monthYear).replaceFirstChar { it.titlecase(TR) }
    fun shortLabel(ym: Int): String = toYearMonth(ym).format(shortMonth).replaceFirstChar { it.titlecase(TR) }
    fun dayMonth(d: LocalDate): String = d.format(dayMonth)
    fun dayMonthWeekday(d: LocalDate): String = d.format(dayMonthWeekday)
    fun full(d: LocalDate): String = d.format(dayMonthYear)

    fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    fun relative(days: Long): String = when {
        days < -1 -> "${-days} gün gecikti"
        days == -1L -> "Dün gecikti"
        days == 0L -> "Bugün son gün"
        days == 1L -> "Yarın"
        else -> "$days gün kaldı"
    }
}
