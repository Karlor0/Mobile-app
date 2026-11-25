package com.karlor.kasa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karlor.kasa.data.*
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.ui.screens.*
import com.karlor.kasa.ui.theme.Kasa
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Özet", Icons.Rounded.SpaceDashboard),
    Tx("İşlemler", Icons.AutoMirrored.Rounded.ReceiptLong),
    Pay("Ödemeler", Icons.Rounded.EventRepeat),
    Debt("Borçlar", Icons.Rounded.AccountBalance),
}

private sealed interface Sheet {
    data class TxEdit(val tx: TxEntity?, val income: Boolean = false) : Sheet
    data class RecurringEdit(val item: RecurringEntity?) : Sheet
    data class CardEdit(val item: CardEntity?) : Sheet
    data class LoanEdit(val item: LoanEntity?) : Sheet
    data class Pay(val o: Obligation) : Sheet
}

@Composable
fun AppRoot(vm: MainViewModel, lockSupported: Boolean) {
    val s by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hidden = settings.hideAmounts
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var paymentsTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var undoTarget by remember { mutableStateOf<Obligation?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(msg: String) { scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }

    BackHandler(enabled = showSettings || tab != Tab.Home) {
        if (showSettings) showSettings = false else tab = Tab.Home
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (!showSettings) BottomBar(tab) { tab = it } },
        floatingActionButton = {
            if (!showSettings && (tab == Tab.Home || tab == Tab.Tx)) {
                AddFab { sheet = Sheet.TxEdit(null) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().background(backgroundBrush())) {
            AnimatedContent(targetState = if (showSettings) null else tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tabs") { t ->
                when (t) {
                    null -> SettingsScreen(
                        settings = settings, lockSupported = lockSupported, contentPadding = padding,
                        onBack = { showSettings = false }, onUpdate = vm::updateSettings,
                        onTestReminders = vm::testReminders, onExport = vm::exportCsv, onWipe = vm::wipe, onMessage = ::toast,
                    )
                    Tab.Home -> DashboardScreen(
                        s, hidden, padding,
                        onMonth = vm::setMonth,
                        onToggleHidden = { vm.updateSettings { it.copy(hideAmounts = !it.hideAmounts) } },
                        onSettings = { showSettings = true },
                        onPay = { sheet = Sheet.Pay(it) },
                        onUndo = { undoTarget = it },
                        onSeeAllTx = { tab = Tab.Tx },
                        onSeePayments = { tab = Tab.Pay },
                        onSeeDebts = { tab = Tab.Debt },
                        onTx = { sheet = Sheet.TxEdit(it) },
                    )
                    Tab.Tx -> TransactionsScreen(
                        s, hidden, padding, onMonth = vm::setMonth,
                        onTx = { sheet = Sheet.TxEdit(it) },
                        onDelete = { vm.deleteTx(it.id); toast("İşlem silindi") },
                        onAdd = { sheet = Sheet.TxEdit(null) },
                    )
                    Tab.Pay -> PaymentsScreen(
                        s, hidden, padding, initialTab = paymentsTab, onMonth = vm::setMonth,
                        onPay = { sheet = Sheet.Pay(it) }, onUndo = { undoTarget = it },
                        onEditRecurring = { sheet = Sheet.RecurringEdit(it) },
                        onEditCard = { sheet = Sheet.CardEdit(it) },
                        onEditLoan = { sheet = Sheet.LoanEdit(it) },
                    )
                    Tab.Debt -> DebtsScreen(
                        s, hidden, padding,
                        onOpenCards = { paymentsTab = 1; tab = Tab.Pay },
                        onOpenLoans = { paymentsTab = 2; tab = Tab.Pay },
                    )
                }
            }
        }
    }

    when (val sh = sheet) {
        null -> Unit
        is Sheet.TxEdit -> TxSheet(sh.tx, sh.income, onDismiss = { sheet = null },
            onSave = { vm.saveTx(it); sheet = null; toast("Kaydedildi") },
            onDelete = { vm.deleteTx(it.id); sheet = null; toast("İşlem silindi") })
        is Sheet.RecurringEdit -> RecurringSheet(sh.item, onDismiss = { sheet = null },
            onSave = { vm.saveRecurring(it); sheet = null; toast("Kaydedildi") },
            onDelete = { vm.deleteRecurring(it); sheet = null; toast("Silindi") })
        is Sheet.CardEdit -> CardSheet(sh.item, onDismiss = { sheet = null },
            onSave = { vm.saveCard(it); sheet = null; toast("Kart kaydedildi") },
            onDelete = { vm.deleteCard(it); sheet = null; toast("Kart silindi") })
        is Sheet.LoanEdit -> LoanSheet(sh.item, onDismiss = { sheet = null },
            onSave = { vm.saveLoan(it); sheet = null; toast("Kredi kaydedildi") },
            onDelete = { vm.deleteLoan(it); sheet = null; toast("Kredi silindi") })
        is Sheet.Pay -> {
            val card = if (sh.o.kind == ObligationKind.CARD) s.defs.cards.firstOrNull { it.id == sh.o.refId } else null
            PaySheet(sh.o, card, onDismiss = { sheet = null }) { amount, date ->
                vm.markPaid(sh.o, amount, date); sheet = null
                toast(if (sh.o.isIncome) "Gelir kaydedildi" else "Ödeme kaydedildi ✓")
            }
        }
    }

    undoTarget?.let { o ->
        AlertDialog(
            onDismissRequest = { undoTarget = null },
            title = { Text("Ödeme geri alınsın mı?") },
            text = { Text("\"${o.title}\" ödenmedi olarak işaretlenecek ve ilgili işlem silinecek. Hatırlatmalar yeniden başlar.") },
            confirmButton = { TextButton(onClick = { vm.revert(o); undoTarget = null; toast("Geri alındı") }) { Text("Geri al") } },
            dismissButton = { TextButton(onClick = { undoTarget = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun backgroundBrush(): Brush {
    val bg = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (Kasa.colors.isDark) 0.10f else 0.06f)
    return Brush.verticalGradient(listOf(tint, bg, bg))
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    Box(
        Modifier
            .shadow(18.dp, RoundedCornerShape(22.dp), spotColor = MaterialTheme.colorScheme.primary)
            .clip(RoundedCornerShape(22.dp))
            .background(Kasa.colors.heroBrush),
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.size(62.dp),
        ) { Icon(Icons.Rounded.Add, "İşlem ekle", modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0),
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Kasa.colors.cardBorder, RoundedCornerShape(28.dp))
                .height(70.dp),
        ) {
            Tab.entries.forEach { t ->
                NavigationBarItem(
                    selected = selected == t,
                    onClick = { onSelect(t) },
                    icon = { Icon(t.icon, t.label) },
                    label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}
