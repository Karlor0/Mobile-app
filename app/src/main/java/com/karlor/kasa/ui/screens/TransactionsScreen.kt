package com.karlor.kasa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.karlor.kasa.data.TxEntity
import com.karlor.kasa.data.TxType
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.ui.UiState
import com.karlor.kasa.ui.components.*
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.MoneyStyle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    s: UiState,
    hidden: Boolean,
    contentPadding: PaddingValues,
    onMonth: (Int) -> Unit,
    onTx: (TxEntity) -> Unit,
    onDelete: (TxEntity) -> Unit,
    onAdd: () -> Unit,
) {
    val c = Kasa.colors
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<TxEntity?>(null) }

    val list = remember(s.txs, filter, query) {
        s.txs.filter {
            when (filter) {
                1 -> it.type == TxType.INCOME
                2 -> it.type == TxType.EXPENSE
                else -> true
            }
        }.filter {
            query.isBlank() || it.title.contains(query, true) || it.category.contains(query, true) || it.note.contains(query, true)
        }
    }
    val grouped = remember(list) { list.groupBy { it.date }.toSortedMap(compareByDescending { it }) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 96.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("İşlemler", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MonthSwitcher(s.ym, onMonth) }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryBox(Modifier.weight(1f), "Gelir", Money.format(s.income, hidden, decimals = false), c.income)
                SummaryBox(Modifier.weight(1f), "Gider", Money.format(s.expense, hidden, decimals = false), c.expense)
                SummaryBox(Modifier.weight(1f), "Net", Money.format(s.balance, hidden, decimals = false), MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query, onValueChange = { if (it.length <= 40) query = it },
                placeholder = { Text("Ara: başlık, kategori, not") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true, shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Tümü", "Gelir", "Gider").forEachIndexed { i, label ->
                    SegmentedButton(selected = filter == i, onClick = { filter = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(label) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    Icons.AutoMirrored.Rounded.ReceiptLong,
                    if (query.isBlank()) "Henüz işlem yok" else "Sonuç bulunamadı",
                    if (query.isBlank()) "Gelir ve giderlerinizi ekleyerek ayınızı takip edin." else "Farklı bir arama deneyin.",
                    action = if (query.isBlank()) "İşlem ekle" else null, onAction = onAdd,
                )
            }
        }
        grouped.forEach { (day, txs) ->
            stickyHeader(key = "h$day") {
                val d = LocalDate.ofEpochDay(day)
                val net = txs.sumOf { if (it.type == TxType.INCOME) it.amount else -it.amount }
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dayLabel(d, s.today), style = MaterialTheme.typography.labelLarge, color = c.muted, modifier = Modifier.weight(1f))
                    Text(Money.format(net, hidden, signed = true), style = MaterialTheme.typography.labelLarge, color = c.muted)
                }
            }
            items(txs, key = { it.id }) { tx ->
                val state = rememberSwipeToDismissBoxState(confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) pendingDelete = tx
                    false
                })
                SwipeToDismissBox(
                    state = state,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(c.expense.copy(alpha = 0.18f)).padding(end = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Icon(Icons.Rounded.Delete, "Sil", tint = c.expense) }
                    },
                    modifier = Modifier.animateItem(),
                ) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background)) { TxRow(tx, hidden) { onTx(tx) } }
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("İşlem silinsin mi?") },
            text = {
                Text(
                    if (tx.linkKind != null) "Bu işlem bir ödeme kaydına bağlı. Silerseniz ilgili ödeme \"ödenmedi\" durumuna döner."
                    else "\"${tx.title.ifBlank { tx.category }}\" kalıcı olarak silinecek.",
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(tx); pendingDelete = null }) { Text("Sil", color = c.expense) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Vazgeç") } },
        )
    }
}

private fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Bugün"
    today.minusDays(1) -> "Dün"
    else -> Months.dayMonthWeekday(d)
}

@Composable
private fun SummaryBox(modifier: Modifier, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    GlassCard(modifier, padding = 12.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Kasa.colors.muted)
        Text(value, style = MaterialTheme.typography.titleSmall.merge(MoneyStyle), color = color, maxLines = 1)
    }
}
