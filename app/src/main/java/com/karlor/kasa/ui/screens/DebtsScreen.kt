package com.karlor.kasa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karlor.kasa.domain.Money
import com.karlor.kasa.ui.UiState
import com.karlor.kasa.ui.components.*
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.MoneyStyle

@Composable
fun DebtsScreen(s: UiState, hidden: Boolean, contentPadding: PaddingValues, onOpenCards: () -> Unit, onOpenLoans: () -> Unit) {
    val c = Kasa.colors
    val ex = s.exposures
    val cardTotal = ex.sumOf { it.cardDebt }
    val loanTotal = ex.sumOf { it.loanDebt }
    val limit = ex.sumOf { it.totalLimit }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Borçlarım", style = MaterialTheme.typography.headlineMedium) }
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF6B81), Color(0xFFB0306B), Color(0xFF4F2A88)))),
            ) {
                Box(Modifier.size(170.dp).offset(x = 220.dp, y = (-50).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
                Column(Modifier.padding(22.dp)) {
                    Text("Toplam borç", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Text(Money.format(s.totalDebt, hidden), color = Color.White, style = MaterialTheme.typography.displaySmall.merge(MoneyStyle), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text("Aylık asgari yük: ${Money.format(s.monthlyBurden, hidden)}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DebtChip(Modifier.weight(1f), "Kredi kartları", Money.format(cardTotal, hidden, decimals = false))
                        DebtChip(Modifier.weight(1f), "Krediler", Money.format(loanTotal, hidden, decimals = false))
                    }
                }
            }
        }
        if (ex.isEmpty()) {
            item { EmptyState(Icons.Rounded.Savings, "Borç kaydı yok", "Kredi kartı veya kredi eklediğinizde hangi bankaya ne kadar borcunuz olduğu burada görünür.") }
            return@LazyColumn
        }
        if (limit > 0) item {
            GlassCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    LabeledValue("Toplam kart limiti", Money.format(limit, hidden, decimals = false), Modifier.weight(1f))
                    LabeledValue("Kullanılabilir", Money.format((limit - cardTotal).coerceAtLeast(0), hidden, decimals = false), valueColor = c.success, alignEnd = true)
                }
                Spacer(Modifier.height(10.dp))
                val util = cardTotal.toFloat() / limit
                ProgressLine(util, if (util > 0.7f) c.expense else if (util > 0.4f) c.warning else c.success)
                Spacer(Modifier.height(6.dp))
                Text("Limit kullanım oranı %${(util * 100).toInt().coerceIn(0, 999)}", style = MaterialTheme.typography.labelMedium, color = c.muted)
            }
        }
        item {
            GlassCard {
                SectionHeader("Banka bazında dağılım")
                val slices = ex.mapIndexed { i, e -> Slice(e.bank, e.total, c.chart[i % c.chart.size]) }.filter { it.value > 0 }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(slices, Modifier.size(130.dp), thickness = 18.dp) {
                        Text("${ex.size} banka", style = MaterialTheme.typography.labelMedium, color = c.muted)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slices.forEach { sl ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(sl.color))
                                Spacer(Modifier.width(8.dp))
                                Text(sl.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Money.compact(sl.value, hidden), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        itemsIndexed(ex, key = { _, e -> e.bank }) { i, e ->
            val color = c.chart[i % c.chart.size]
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.AccountBalance, color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.bank, style = MaterialTheme.typography.titleMedium)
                        Text("Aylık ${Money.format(e.monthlyBurden, hidden)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    Text(Money.format(e.total, hidden), style = MaterialTheme.typography.titleMedium.merge(MoneyStyle), color = c.expense)
                }
                Spacer(Modifier.height(10.dp))
                ProgressLine(if (s.totalDebt == 0L) 0f else e.total.toFloat() / s.totalDebt, color, height = 6.dp)
                Spacer(Modifier.height(10.dp))
                e.cards.forEach { card ->
                    DebtLine(Icons.Rounded.CreditCard, card.name, "Dönem ${Money.format(card.statementDebt, hidden, decimals = false)} · Asgari ${Money.format(card.minPayment, hidden, decimals = false)}", Money.format(card.currentDebt, hidden), onOpenCards)
                }
                e.loans.forEach { (loan, p) ->
                    DebtLine(Icons.Rounded.AccountBalance, loan.name, "${loan.loanType} · ${p.remaining} taksit kaldı", Money.format(p.remainingAmount, hidden), onOpenLoans)
                }
            }
        }
    }
}

@Composable
private fun DebtChip(modifier: Modifier, label: String, value: String) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)).padding(12.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall.merge(MoneyStyle), maxLines = 1)
    }
}

@Composable
private fun DebtLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, amount: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Kasa.colors.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = Kasa.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(amount, style = MaterialTheme.typography.labelLarge)
    }
}

