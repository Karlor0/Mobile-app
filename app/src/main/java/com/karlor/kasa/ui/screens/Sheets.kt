package com.karlor.kasa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.karlor.kasa.data.*
import com.karlor.kasa.domain.Catalog
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.ui.components.*
import com.karlor.kasa.ui.theme.CardGradients
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.MoneyStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasaSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: LocalDate, onChange: (LocalDate) -> Unit, label: String = "Tarih") {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = Months.full(date), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { Icon(Icons.Rounded.CalendarMonth, null) },
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)).clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("İptal") } },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { cat ->
            val v = CategoryVisuals.of(cat)
            val sel = cat == selected
            FilterChip(
                selected = sel, onClick = { onSelect(cat) },
                label = { Text(cat) },
                leadingIcon = { Icon(v.icon, null, tint = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else v.color, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

@Composable
private fun TypeSwitch(isIncome: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !isIncome, onClick = { onChange(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2),
            colors = SegmentedButtonDefaults.colors(activeContainerColor = Kasa.colors.expense.copy(alpha = 0.18f)),
        ) { Text("Gider") }
        SegmentedButton(
            selected = isIncome, onClick = { onChange(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2),
            colors = SegmentedButtonDefaults.colors(activeContainerColor = Kasa.colors.income.copy(alpha = 0.18f)),
        ) { Text("Gelir") }
    }
}

@Composable
private fun DeleteButton(label: String, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Kasa.colors.expense) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Silinsin mi?") },
            text = { Text("Bu kayıt ve ödeme geçmişi silinecek. Geçmiş işlemler korunur.") },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text("Sil", color = Kasa.colors.expense) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Transaction
// ---------------------------------------------------------------------------------------------

@Composable
fun TxSheet(initial: TxEntity?, defaultIncome: Boolean, onDismiss: () -> Unit, onSave: (TxEntity) -> Unit, onDelete: (TxEntity) -> Unit) {
    var isIncome by remember { mutableStateOf(initial?.type == TxType.INCOME || (initial == null && defaultIncome)) }
    var amount by remember { mutableStateOf(initial?.let { Money.toInput(it.amount) } ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var date by remember { mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.date) } ?: LocalDate.now()) }
    var tried by remember { mutableStateOf(false) }
    val cats = if (isIncome) Catalog.incomeCategories else Catalog.expenseCategories
    LaunchedEffect(isIncome) { if (category !in cats) category = cats.first() }
    val parsed = Money.parse(amount)
    val linked = initial?.linkKind != null

    KasaSheet(onDismiss) {
        SheetTitle(if (initial == null) "Yeni işlem" else "İşlemi düzenle", if (linked) "Bu işlem bir ödeme kaydına bağlı" else null)
        if (!linked) TypeSwitch(isIncome) { isIncome = it }
        AmountBig(amount, { amount = it }, isError = tried && (parsed == null || parsed == 0L), income = isIncome)
        KasaTextField(title, { title = it }, "Başlık (ör. Migros, Elektrik)")
        Text("Kategori", style = MaterialTheme.typography.labelLarge, color = Kasa.colors.muted)
        CategoryPicker(cats, category) { category = it }
        DateField(date, { date = it })
        KasaTextField(note, { note = it }, "Not (isteğe bağlı)", maxLength = 120, singleLine = false)
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Kaydet", onClick = {
            tried = true
            if (parsed != null && parsed > 0) {
                onSave(
                    (initial ?: TxEntity(type = TxType.EXPENSE, amount = 0, category = "", title = "", date = 0)).copy(
                        type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
                        amount = parsed, title = title.trim(), note = note.trim(), category = category,
                        date = date.toEpochDay(),
                    ),
                )
            }
        }, color = if (isIncome) Kasa.colors.income else MaterialTheme.colorScheme.primary)
        if (initial != null) DeleteButton(if (linked) "Sil ve ödemeyi geri al" else "İşlemi sil") { onDelete(initial) }
    }
}

@Composable
private fun AmountBig(value: String, onChange: (String) -> Unit, isError: Boolean, income: Boolean) {
    val c = Kasa.colors
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 18 && v.all { it.isDigit() || it == ',' || it == '.' }) onChange(v) },
        placeholder = { Text("0,00", style = MaterialTheme.typography.headlineMedium.merge(MoneyStyle)) },
        prefix = { Text("₺ ", style = MaterialTheme.typography.headlineMedium.merge(MoneyStyle)) },
        textStyle = MaterialTheme.typography.headlineMedium.merge(MoneyStyle).copy(color = if (income) c.income else MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        isError = isError,
        supportingText = if (isError) ({ Text("Geçerli bir tutar girin") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------------------------
// Recurring
// ---------------------------------------------------------------------------------------------

@Composable
fun RecurringSheet(initial: RecurringEntity?, onDismiss: () -> Unit, onSave: (RecurringEntity) -> Unit, onDelete: (Long) -> Unit) {
    var isIncome by remember { mutableStateOf(initial?.isIncome ?: false) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.let { Money.toInput(it.amount) } ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Fatura") }
    var provider by remember { mutableStateOf(initial?.provider ?: "") }
    var day by remember { mutableIntStateOf(initial?.dueDay ?: LocalDate.now().dayOfMonth) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var tried by remember { mutableStateOf(false) }
    val cats = if (isIncome) Catalog.incomeCategories else Catalog.expenseCategories
    LaunchedEffect(isIncome) { if (category !in cats) category = cats.first() }
    val parsed = Money.parse(amount)

    KasaSheet(onDismiss) {
        SheetTitle(if (initial == null) "Sabit kalem ekle" else "Sabit kalemi düzenle", "Her ay tekrar eden ödeme veya gelir")
        TypeSwitch(isIncome) { isIncome = it }
        KasaTextField(name, { name = it }, if (isIncome) "Ad (ör. Maaş)" else "Ad (ör. Kira, İnternet, Netflix)", isError = tried && name.isBlank())
        AmountField(amount, { amount = it }, "Aylık tutar", isError = tried && (parsed == null || parsed == 0L))
        DayField(day, { day = it }, if (isIncome) "Gelir günü" else "Son ödeme günü")
        KasaTextField(provider, { provider = it }, if (isIncome) "Kaynak (isteğe bağlı)" else "Kurum / banka (isteğe bağlı)")
        Text("Kategori", style = MaterialTheme.typography.labelLarge, color = Kasa.colors.muted)
        CategoryPicker(cats, category) { category = it }
        if (initial != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aktif", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(active, { active = it })
            }
        }
        PrimaryButton("Kaydet", onClick = {
            tried = true
            if (name.isNotBlank() && parsed != null && parsed > 0) {
                onSave(
                    RecurringEntity(
                        id = initial?.id ?: 0, name = name.trim(), category = category, amount = parsed, dueDay = day,
                        isIncome = isIncome, provider = provider.trim(), startYm = initial?.startYm ?: Months.current(),
                        active = active, createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                    ),
                )
            }
        })
        if (initial != null) DeleteButton("Sabit kalemi sil") { onDelete(initial.id) }
    }
}

// ---------------------------------------------------------------------------------------------
// Credit card
// ---------------------------------------------------------------------------------------------

@Composable
fun CardSheet(initial: CardEntity?, onDismiss: () -> Unit, onSave: (CardEntity) -> Unit, onDelete: (Long) -> Unit) {
    var bank by remember { mutableStateOf(initial?.bank ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var last4 by remember { mutableStateOf(initial?.last4 ?: "") }
    var limit by remember { mutableStateOf(initial?.let { Money.toInput(it.creditLimit) } ?: "") }
    var debt by remember { mutableStateOf(initial?.let { Money.toInput(it.currentDebt) } ?: "") }
    var stmt by remember { mutableStateOf(initial?.let { Money.toInput(it.statementDebt) } ?: "") }
    var minPay by remember { mutableStateOf(initial?.let { Money.toInput(it.minPayment) } ?: "") }
    var stmtDay by remember { mutableIntStateOf(initial?.statementDay ?: 1) }
    var dueDay by remember { mutableIntStateOf(initial?.dueDay ?: 11) }
    var color by remember { mutableIntStateOf(initial?.colorIndex ?: 0) }
    var tried by remember { mutableStateOf(false) }

    fun opt(s: String): Long? = if (s.isBlank()) 0L else Money.parse(s)
    val pLimit = opt(limit); val pDebt = opt(debt); val pStmt = opt(stmt); val pMin = opt(minPay)
    val valid = bank.isNotBlank() && name.isNotBlank() && pLimit != null && pDebt != null && pStmt != null && pMin != null

    KasaSheet(onDismiss) {
        SheetTitle(if (initial == null) "Kredi kartı ekle" else "Kartı düzenle")
        CreditCardVisual(
            CardEntity(bank = bank.ifBlank { "Banka" }, name = name.ifBlank { "Kart adı" }, last4 = last4, creditLimit = pLimit ?: 0, currentDebt = pDebt ?: 0,
                statementDebt = pStmt ?: 0, minPayment = pMin ?: 0, statementDay = stmtDay, dueDay = dueDay, colorIndex = color, startYm = 0),
            hidden = false, onClick = {},
        )
        ColorPicker(color) { color = it }
        ComboField(bank, { bank = it }, "Banka", Catalog.banks, isError = tried && bank.isBlank())
        KasaTextField(name, { name = it }, "Kart adı (ör. Bonus, World, Axess)", isError = tried && name.isBlank())
        KasaTextField(last4, { v -> if (v.length <= 4 && v.all(Char::isDigit)) last4 = v }, "Son 4 hane (isteğe bağlı)", keyboardType = KeyboardType.NumberPassword)
        AmountField(limit, { limit = it }, "Kart limiti", isError = tried && pLimit == null)
        AmountField(debt, { debt = it }, "Güncel toplam borç", isError = tried && pDebt == null)
        AmountField(stmt, { stmt = it }, "Dönem borcu (bu ay ödenecek)", isError = tried && pStmt == null)
        AmountField(minPay, { minPay = it }, "Asgari ödeme", isError = tried && pMin == null)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DayField(stmtDay, { stmtDay = it }, "Hesap kesim", Modifier.weight(1f))
            DayField(dueDay, { dueDay = it }, "Son ödeme", Modifier.weight(1f))
        }
        PrimaryButton("Kaydet", onClick = {
            tried = true
            if (valid) {
                onSave(
                    CardEntity(
                        id = initial?.id ?: 0, bank = bank.trim(), name = name.trim(), last4 = last4,
                        creditLimit = pLimit!!, currentDebt = maxOf(pDebt!!, pStmt!!), statementDebt = pStmt, minPayment = pMin!!,
                        statementDay = stmtDay, dueDay = dueDay, colorIndex = color,
                        startYm = initial?.startYm ?: Months.current(), active = initial?.active ?: true,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                    ),
                )
            }
        })
        if (initial != null) DeleteButton("Kartı sil") { onDelete(initial.id) }
    }
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        CardGradients.forEachIndexed { i, g ->
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(g))
                    .then(if (i == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { if (i == selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Loan
// ---------------------------------------------------------------------------------------------

@Composable
fun LoanSheet(initial: LoanEntity?, onDismiss: () -> Unit, onSave: (LoanEntity) -> Unit, onDelete: (Long) -> Unit) {
    val cur = Months.current()
    var bank by remember { mutableStateOf(initial?.bank ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.loanType ?: Catalog.loanTypes.first()) }
    var principal by remember { mutableStateOf(initial?.let { Money.toInput(it.principal) } ?: "") }
    var installment by remember { mutableStateOf(initial?.let { Money.toInput(it.installment) } ?: "") }
    var total by remember { mutableStateOf(initial?.totalInstallments?.toString() ?: "") }
    var paidBefore by remember { mutableStateOf(initial?.initialPaid?.toString() ?: "0") }
    var firstYm by remember { mutableIntStateOf(initial?.startYm ?: cur) }
    var day by remember { mutableIntStateOf(initial?.dueDay ?: 15) }
    var rate by remember { mutableStateOf(initial?.interestRate?.takeIf { it > 0 }?.toString()?.replace('.', ',') ?: "") }
    var tried by remember { mutableStateOf(false) }

    val pPrincipal = if (principal.isBlank()) 0L else Money.parse(principal)
    val pInst = Money.parse(installment)
    val pTotal = total.toIntOrNull()?.takeIf { it in 1..600 }
    val pPaid = paidBefore.ifBlank { "0" }.toIntOrNull()?.takeIf { it >= 0 && (pTotal == null || it <= pTotal) }
    val pRate = if (rate.isBlank()) 0.0 else rate.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..100.0 }
    val valid = bank.isNotBlank() && name.isNotBlank() && pPrincipal != null && pInst != null && pInst > 0 && pTotal != null && pPaid != null && pRate != null

    KasaSheet(onDismiss) {
        SheetTitle(if (initial == null) "Kredi ekle" else "Krediyi düzenle")
        ComboField(bank, { bank = it }, "Banka", Catalog.banks, isError = tried && bank.isBlank())
        KasaTextField(name, { name = it }, "Kredi adı (ör. Araba kredisi)", isError = tried && name.isBlank())
        ComboField(type, { type = it }, "Kredi türü", Catalog.loanTypes)
        AmountField(principal, { principal = it }, "Çekilen tutar (anapara)", isError = tried && pPrincipal == null)
        AmountField(installment, { installment = it }, "Aylık taksit", isError = tried && (pInst == null || pInst == 0L))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KasaTextField(total, { v -> if (v.length <= 3 && v.all(Char::isDigit)) total = v }, "Toplam taksit", Modifier.weight(1f), keyboardType = KeyboardType.Number, isError = tried && pTotal == null)
            KasaTextField(paidBefore, { v -> if (v.length <= 3 && v.all(Char::isDigit)) paidBefore = v }, "Ödenmiş taksit", Modifier.weight(1f), keyboardType = KeyboardType.Number, isError = tried && pPaid == null)
        }
        Text("İlk taksit ayı", style = MaterialTheme.typography.labelLarge, color = Kasa.colors.muted)
        MonthSwitcher(firstYm, { firstYm = it })
        DayField(day, { day = it }, "Taksit ödeme günü")
        KasaTextField(rate, { v -> if (v.length <= 6 && v.all { it.isDigit() || it == ',' || it == '.' }) rate = v }, "Aylık faiz oranı % (isteğe bağlı)", keyboardType = KeyboardType.Decimal, isError = tried && pRate == null)

        if (pInst != null && pTotal != null) {
            val remaining = (pTotal - (pPaid ?: 0)).coerceAtLeast(0)
            GlassCard(padding = 14.dp) {
                Row {
                    LabeledValue("Toplam geri ödeme", Money.format(pInst * pTotal), Modifier.weight(1f))
                    LabeledValue("Kalan", Money.format(pInst * remaining), alignEnd = true, valueColor = Kasa.colors.expense)
                }
                if (pPrincipal != null && pPrincipal > 0) {
                    Spacer(Modifier.height(8.dp))
                    LabeledValue("Toplam faiz + masraf", Money.format((pInst * pTotal - pPrincipal).coerceAtLeast(0)))
                }
                Spacer(Modifier.height(8.dp))
                Text("Son taksit: ${Months.label(firstYm + pTotal - 1)}", style = MaterialTheme.typography.labelMedium, color = Kasa.colors.muted)
            }
        }
        PrimaryButton("Kaydet", onClick = {
            tried = true
            if (valid) {
                onSave(
                    LoanEntity(
                        id = initial?.id ?: 0, bank = bank.trim(), name = name.trim(), loanType = type.trim().ifBlank { "Diğer" },
                        principal = pPrincipal!!, installment = pInst!!, totalInstallments = pTotal!!, initialPaid = pPaid!!,
                        startYm = firstYm, dueDay = day, interestRate = pRate!!, active = initial?.active ?: true,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                    ),
                )
            }
        })
        if (initial != null) DeleteButton("Krediyi sil") { onDelete(initial.id) }
    }
}

// ---------------------------------------------------------------------------------------------
// Pay
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaySheet(o: Obligation, card: CardEntity?, onDismiss: () -> Unit, onConfirm: (Long, LocalDate) -> Unit) {
    var amount by remember { mutableStateOf(Money.toInput(o.amount)) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var tried by remember { mutableStateOf(false) }
    val parsed = if (amount.isBlank()) 0L else Money.parse(amount)
    KasaSheet(onDismiss) {
        SheetTitle(
            if (o.isIncome) "Gelir alındı" else "Ödeme yapıldı",
            "${o.title} · ${o.subtitle}" + (o.installmentLabel?.let { " · $it. taksit" } ?: ""),
        )
        GlassCard(padding = 14.dp) {
            Row {
                LabeledValue("Son ödeme", Months.full(o.dueDate), Modifier.weight(1f))
                LabeledValue("Ay", Months.label(o.ym), alignEnd = true)
            }
        }
        if (card != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (card.statementDebt > 0) AssistChip(onClick = { amount = Money.toInput(card.statementDebt) }, label = { Text("Dönem borcu ${Money.format(card.statementDebt)}") })
                if (card.minPayment > 0) AssistChip(onClick = { amount = Money.toInput(card.minPayment) }, label = { Text("Asgari ${Money.format(card.minPayment)}") })
                if (card.currentDebt > 0) AssistChip(onClick = { amount = Money.toInput(card.currentDebt) }, label = { Text("Tümü ${Money.format(card.currentDebt)}") })
            }
        }
        AmountField(amount, { amount = it }, if (o.isIncome) "Alınan tutar" else "Ödenen tutar", isError = tried && parsed == null)
        DateField(date, { date = it }, if (o.isIncome) "Alındığı tarih" else "Ödeme tarihi")
        if (card != null && parsed == 0L) {
            Text("Tutar 0 girilirse bu ay için \"borç yok\" olarak işaretlenir.", style = MaterialTheme.typography.labelSmall, color = Kasa.colors.muted)
        }
        PrimaryButton(if (o.isIncome) "Alındı olarak işaretle" else "Ödendi olarak işaretle", onClick = {
            tried = true
            if (parsed != null) onConfirm(parsed, date)
        }, color = Kasa.colors.success)
    }
}
