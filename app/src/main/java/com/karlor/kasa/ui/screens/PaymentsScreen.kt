package com.karlor.kasa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karlor.kasa.data.CardEntity
import com.karlor.kasa.data.LoanEntity
import com.karlor.kasa.data.ObligationKind
import com.karlor.kasa.data.RecurringEntity
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.domain.ObligationEngine
import com.karlor.kasa.ui.UiState
import com.karlor.kasa.ui.components.*
import com.karlor.kasa.ui.theme.CardGradients
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.MoneyStyle

@Composable
fun PaymentsScreen(
    s: UiState,
    hidden: Boolean,
    contentPadding: PaddingValues,
    initialTab: Int,
    onMonth: (Int) -> Unit,
    onPay: (Obligation) -> Unit,
    onUndo: (Obligation) -> Unit,
    onEditRecurring: (RecurringEntity?) -> Unit,
    onEditCard: (CardEntity?) -> Unit,
    onEditLoan: (LoanEntity?) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val c = Kasa.colors
    val expenseObl = s.obligations.filter { !it.isIncome && !it.nothingDue }
    val total = expenseObl.sumOf { it.payment?.amount ?: it.amount }
    val paid = expenseObl.filter { it.paid }.sumOf { it.payment?.amount ?: 0 }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Ödemeler", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MonthSwitcher(s.ym, onMonth) }
        }
        item {
            GlassCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    LabeledValue("Bu ay ödenecek", Money.format(total, hidden), Modifier.weight(1f))
                    LabeledValue("Kalan", Money.format((total - paid).coerceAtLeast(0), hidden), valueColor = c.warning, alignEnd = true)
                }
                Spacer(Modifier.height(12.dp))
                ProgressLine(if (total == 0L) 0f else paid.toFloat() / total, c.success)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${expenseObl.count { it.paid }} / ${expenseObl.size} ödeme tamamlandı",
                    style = MaterialTheme.typography.labelMedium, color = c.muted,
                )
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Sabit", "Kartlar", "Krediler").forEachIndexed { i, label ->
                    SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                }
            }
        }
        when (tab) {
            0 -> recurringSection(s, hidden, onPay, onUndo, onEditRecurring)
            1 -> cardsSection(s, hidden, onPay, onUndo, onEditCard)
            else -> loansSection(s, hidden, onPay, onUndo, onEditLoan)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recurringSection(
    s: UiState, hidden: Boolean, onPay: (Obligation) -> Unit, onUndo: (Obligation) -> Unit, onEdit: (RecurringEntity?) -> Unit,
) {
    val obl = s.obligations.filter { it.kind == ObligationKind.RECURRING }
    val byId = s.defs.recurring.associateBy { it.id }
    val inactive = s.defs.recurring.filter { r -> obl.none { it.refId == r.id } }
    item { AddButton("Sabit gider / gelir ekle") { onEdit(null) } }
    if (obl.isEmpty() && inactive.isEmpty()) {
        item { EmptyState(Icons.Rounded.EventRepeat, "Sabit kalem yok", "Kira, faturalar, abonelikler ve maaş gibi her ay tekrar eden kalemleri ekleyin.") }
    }
    val expenses = obl.filter { !it.isIncome }
    val incomes = obl.filter { it.isIncome }
    if (expenses.isNotEmpty()) item {
        GlassCard {
            SectionHeader("Sabit giderler")
            expenses.forEachIndexed { i, o ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ObligationRow(o, s.today, hidden, { onPay(o) }, { onUndo(o) }) { onEdit(byId[o.refId]) }
            }
        }
    }
    if (incomes.isNotEmpty()) item {
        GlassCard {
            SectionHeader("Sabit gelirler")
            incomes.forEachIndexed { i, o ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ObligationRow(o, s.today, hidden, { onPay(o) }, { onUndo(o) }) { onEdit(byId[o.refId]) }
            }
        }
    }
    if (inactive.isNotEmpty()) item {
        GlassCard {
            SectionHeader("Bu ay geçerli olmayanlar")
            inactive.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onEdit(r) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(CategoryVisuals.of(r.category).icon, Kasa.colors.muted, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(r.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Pill(if (!r.active) "Pasif" else "Başlangıç ${Months.shortLabel(r.startYm)}", Kasa.colors.muted)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.cardsSection(
    s: UiState, hidden: Boolean, onPay: (Obligation) -> Unit, onUndo: (Obligation) -> Unit, onEdit: (CardEntity?) -> Unit,
) {
    item { AddButton("Kredi kartı ekle") { onEdit(null) } }
    if (s.defs.cards.isEmpty()) {
        item { EmptyState(Icons.Rounded.CreditCard, "Kart eklenmedi", "Kartlarınızı ekleyin; son ödeme gününden önce artan sıklıkta hatırlatalım.") }
    }
    items(s.defs.cards, key = { "card${it.id}" }) { card ->
        val o = s.obligations.firstOrNull { it.kind == ObligationKind.CARD && it.refId == card.id }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CreditCardVisual(card, hidden) { onEdit(card) }
            if (o != null) {
                GlassCard(padding = 12.dp) {
                    ObligationRow(
                        o, s.today, hidden, { onPay(o) }, { onUndo(o) },
                        titleOverride = "${Months.label(o.ym)} ekstresi",
                        subtitleOverride = if (card.minPayment > 0) "Asgari ${Money.format(card.minPayment, hidden)}" else "Dönem borcu",
                    )
                }
            }
        }
    }
}

@Composable
fun CreditCardVisual(card: CardEntity, hidden: Boolean, onClick: () -> Unit) {
    val grad = CardGradients[card.colorIndex.mod(CardGradients.size)]
    val used = if (card.creditLimit > 0) card.currentDebt.toFloat() / card.creditLimit else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.75f)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(grad))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.size(200.dp).offset(x = 180.dp, y = (-70).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        Box(Modifier.size(140.dp).offset(x = 230.dp, y = 90.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(card.bank, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(card.name, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.Contactless, null, tint = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.weight(1f))
            Text("Güncel borç", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Text(Money.format(card.currentDebt, hidden), color = Color.White, style = MaterialTheme.typography.headlineSmall.merge(MoneyStyle))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(used.coerceIn(0f, 1f)).clip(RoundedCornerShape(50)).background(Color.White))
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    if (card.last4.isNotBlank()) "•••• ${card.last4}" else "Limit ${Money.format(card.creditLimit, hidden, decimals = false)}",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.weight(1f),
                )
                Text(
                    "Kesim ${card.statementDay} · Son ödeme ${card.dueDay}",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.loansSection(
    s: UiState, hidden: Boolean, onPay: (Obligation) -> Unit, onUndo: (Obligation) -> Unit, onEdit: (LoanEntity?) -> Unit,
) {
    item { AddButton("Kredi ekle") { onEdit(null) } }
    if (s.defs.loans.isEmpty()) {
        item { EmptyState(Icons.Rounded.AccountBalance, "Kredi eklenmedi", "İhtiyaç, konut, taşıt kredilerinizi ekleyin; taksitleri ve kalan borcu takip edin.") }
    }
    items(s.defs.loans, key = { "loan${it.id}" }) { loan ->
        val prog = ObligationEngine.loanProgress(loan, s.defs.payments)
        val o = s.obligations.firstOrNull { it.kind == ObligationKind.LOAN && it.refId == loan.id }
        val c = Kasa.colors
        GlassCard(onClick = { onEdit(loan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.AccountBalance, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(loan.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${loan.bank} · ${loan.loanType}", style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (prog.remaining == 0) Pill("Kapandı", c.success)
            }
            Spacer(Modifier.height(14.dp))
            Row {
                LabeledValue("Aylık taksit", Money.format(loan.installment, hidden), Modifier.weight(1f))
                LabeledValue("Kalan borç", Money.format(prog.remainingAmount, hidden), Modifier.weight(1f), valueColor = c.expense)
                LabeledValue("Taksit", "${prog.paid}/${prog.total}", alignEnd = true)
            }
            Spacer(Modifier.height(12.dp))
            ProgressLine(prog.fraction, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            val endYm = loan.startYm + loan.totalInstallments - 1
            Text(
                "Son taksit ${Months.label(endYm)} · Her ayın ${loan.dueDay}. günü" +
                    if (loan.interestRate > 0) " · Faiz %${"%.2f".format(Months.TR, loan.interestRate)}" else "",
                style = MaterialTheme.typography.labelSmall, color = c.muted,
            )
            if (o != null) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ObligationRow(
                    o, s.today, hidden, { onPay(o) }, { onUndo(o) },
                    titleOverride = "Bu ayın taksiti", subtitleOverride = "${o.installmentLabel}. taksit",
                )
            }
        }
    }
}

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
