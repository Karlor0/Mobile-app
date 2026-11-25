package com.karlor.kasa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.karlor.kasa.data.ObligationKind
import com.karlor.kasa.data.TxEntity
import com.karlor.kasa.data.TxType
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.ui.components.CategoryVisuals
import com.karlor.kasa.ui.components.IconBadge
import com.karlor.kasa.ui.components.Pill
import com.karlor.kasa.ui.components.UrgencyChip
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.MoneyStyle
import java.time.LocalDate

@Composable
fun ObligationRow(
    o: Obligation,
    today: LocalDate,
    hidden: Boolean,
    onPay: () -> Unit,
    onUndo: () -> Unit,
    titleOverride: String? = null,
    subtitleOverride: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val visual = CategoryVisuals.of(o.category)
    val icon = when (o.kind) {
        ObligationKind.CARD -> Icons.Rounded.CreditCard
        ObligationKind.LOAN -> Icons.Rounded.AccountBalance
        else -> visual.icon
    }
    val c = Kasa.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, if (o.isIncome) c.income else visual.color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titleOverride ?: o.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitleOverride ?: buildString {
                    append(o.subtitle)
                    o.installmentLabel?.let { append(" · ").append(it) }
                },
                style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    o.isIncome && o.paid -> Pill("Alındı", c.income)
                    o.isIncome -> Pill(Months.dayMonth(o.dueDate), c.muted)
                    o.nothingDue -> Pill("Borç yok", c.muted)
                    else -> UrgencyChip(o.daysLeft(today), o.paid)
                }
                if (!o.paid && !o.isIncome) {
                    Spacer(Modifier.width(6.dp))
                    Text(Months.dayMonth(o.dueDate), style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val amt = o.payment?.amount ?: o.amount
            Text(
                if (o.kind == ObligationKind.CARD && amt <= 0 && !o.paid) "—" else Money.format(amt, hidden),
                style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
                color = if (o.isIncome) c.income else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            if (o.paid) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(c.success.copy(alpha = 0.16f)).clickable(onClick = onUndo),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, "Ödendi – geri al", tint = c.success, modifier = Modifier.size(18.dp)) }
            } else if (!o.nothingDue) {
                FilledTonalButton(
                    onClick = onPay,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = (if (o.isIncome) c.income else MaterialTheme.colorScheme.primary).copy(alpha = 0.18f),
                        contentColor = if (o.isIncome) c.income else MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(if (o.isIncome) "Alındı" else "Ödedim", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
fun TxRow(tx: TxEntity, hidden: Boolean, onClick: () -> Unit) {
    val visual = CategoryVisuals.of(tx.category)
    val c = Kasa.colors
    val income = tx.type == TxType.INCOME
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(visual.icon, if (income) c.income else visual.color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title.ifBlank { tx.category }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                tx.category + if (tx.note.isNotBlank()) " · ${tx.note}" else "",
                style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Money.format(if (income) tx.amount else -tx.amount, hidden, signed = true),
            style = MaterialTheme.typography.titleSmall.merge(MoneyStyle),
            color = if (income) c.income else MaterialTheme.colorScheme.onSurface,
        )
    }
}
