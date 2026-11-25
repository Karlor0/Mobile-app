package com.karlor.kasa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.ui.UiState
import com.karlor.kasa.ui.components.*
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.MoneyStyle
import java.time.LocalTime

@Composable
fun DashboardScreen(
    s: UiState,
    hidden: Boolean,
    contentPadding: PaddingValues,
    onMonth: (Int) -> Unit,
    onToggleHidden: () -> Unit,
    onSettings: () -> Unit,
    onPay: (Obligation) -> Unit,
    onUndo: (Obligation) -> Unit,
    onSeeAllTx: () -> Unit,
    onSeePayments: () -> Unit,
    onSeeDebts: () -> Unit,
    onTx: (com.karlor.kasa.data.TxEntity) -> Unit,
) {
    val c = Kasa.colors
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    Text("Kasam", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = onToggleHidden) {
                    Icon(if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Tutarları gizle/göster")
                }
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Ayarlar") }
            }
        }
        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MonthSwitcher(s.ym, onMonth) } }
        item { HeroCard(s, hidden) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val total = s.unpaidObligations + s.paidObligations
                StatTile(
                    Modifier.weight(1f), Icons.Rounded.EventRepeat, c.warning, "Kalan ödemeler",
                    Money.format(s.unpaidObligations, hidden, decimals = false),
                    fraction = if (total == 0L) 1f else s.paidObligations.toFloat() / total,
                    footer = "${s.obligations.count { !it.isIncome && it.paid }}/${s.obligations.count { !it.isIncome && !it.nothingDue }} ödendi",
                    onClick = onSeePayments,
                )
                StatTile(
                    Modifier.weight(1f), Icons.Rounded.AccountBalance, c.expense, "Toplam borç",
                    Money.format(s.totalDebt, hidden, decimals = false),
                    fraction = null,
                    footer = "Aylık yük ${Money.compact(s.monthlyBurden, hidden)}",
                    onClick = onSeeDebts,
                )
            }
        }
        item {
            GlassCard {
                SectionHeader("Yaklaşan ödemeler", action = "Tümü", onAction = onSeePayments)
                val list = s.pending.take(6)
                if (list.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        IconBadge(Icons.Rounded.TaskAlt, c.success)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Her şey yolunda", style = MaterialTheme.typography.titleSmall)
                            Text("Önümüzdeki 30 günde bekleyen ödeme yok.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                } else {
                    list.forEachIndexed { i, o ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ObligationRow(o, s.today, hidden, onPay = { onPay(o) }, onUndo = { onUndo(o) })
                    }
                    if (s.pending.size > list.size) {
                        Text("+${s.pending.size - list.size} ödeme daha", style = MaterialTheme.typography.labelMedium, color = c.muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        item { SpendingCard(s, hidden) }
        item {
            GlassCard {
                SectionHeader("Son işlemler", action = "Tümü", onAction = onSeeAllTx)
                if (s.txs.isEmpty()) {
                    Text("Bu ay henüz işlem yok. + ile ilk işleminizi ekleyin.", style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    s.txs.take(5).forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        TxRow(t, hidden) { onTx(t) }
                    }
                }
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Günaydın ☀️"
    in 12..17 -> "İyi günler 👋"
    in 18..22 -> "İyi akşamlar 🌙"
    else -> "İyi geceler ✨"
}

@Composable
private fun HeroCard(s: UiState, hidden: Boolean) {
    val c = Kasa.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(c.heroBrush),
    ) {
        // decorative orbs
        Box(Modifier.size(180.dp).offset(x = 210.dp, y = (-60).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        Box(Modifier.size(120.dp).offset(x = (-40).dp, y = 120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
        Column(Modifier.padding(22.dp)) {
            Text("Bu ayki bakiye", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                Money.format(s.balance, hidden),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall.merge(MoneyStyle),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ay sonu tahmini: ${Money.format(s.forecast, hidden)}",
                color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(Modifier.weight(1f), Icons.Rounded.ArrowDownward, "Gelir", Money.format(s.income, hidden, decimals = false))
                HeroStat(Modifier.weight(1f), Icons.Rounded.ArrowUpward, "Gider", Money.format(s.expense, hidden, decimals = false))
            }
        }
    }
}

@Composable
private fun HeroStat(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    value: String,
    fraction: Float?,
    footer: String,
    onClick: () -> Unit,
) {
    GlassCard(modifier, onClick = onClick, padding = 16.dp) {
        IconBadge(icon, tint, size = 36.dp)
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Kasa.colors.muted)
        Text(value, style = MaterialTheme.typography.titleLarge.merge(MoneyStyle), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        if (fraction != null) {
            ProgressLine(fraction, Kasa.colors.success, height = 6.dp)
            Spacer(Modifier.height(6.dp))
        }
        Text(footer, style = MaterialTheme.typography.labelSmall, color = Kasa.colors.muted, maxLines = 1)
    }
}

@Composable
private fun SpendingCard(s: UiState, hidden: Boolean) {
    val c = Kasa.colors
    GlassCard {
        SectionHeader("Harcama dağılımı")
        if (s.categoryTotals.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, "Veri yok", "Gider ekledikçe kategorilere göre dağılım burada görünecek.")
            return@GlassCard
        }
        val top = s.categoryTotals.take(6)
        val rest = s.categoryTotals.drop(6).sumOf { it.second }
        val slices = top.map { (k, v) -> Slice(k, v, CategoryVisuals.of(k).color) } +
            if (rest > 0) listOf(Slice("Diğer", rest, c.muted)) else emptyList()
        val total = slices.sumOf { it.value }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(slices, Modifier.size(150.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Toplam", style = MaterialTheme.typography.labelSmall, color = c.muted)
                    Text(Money.compact(total, hidden), style = MaterialTheme.typography.titleMedium.merge(MoneyStyle))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEach { sl ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(sl.color))
                        Spacer(Modifier.width(8.dp))
                        Text(sl.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("%${(sl.value * 100 / total.coerceAtLeast(1))}", style = MaterialTheme.typography.labelMedium, color = c.muted)
                    }
                }
            }
        }
    }
}
