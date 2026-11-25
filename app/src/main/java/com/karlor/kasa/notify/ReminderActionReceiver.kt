package com.karlor.kasa.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.karlor.kasa.data.ObligationKind
import com.karlor.kasa.domain.Obligation
import com.karlor.kasa.kasa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles notification actions. The receiver is not exported and only reachable through
 * immutable explicit PendingIntents. The amount is NOT taken from the intent: it is re-read
 * from the database so a tampered intent cannot record an arbitrary amount.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_PAID && action != ACTION_SNOOZE) return
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { k -> ObligationKind.entries.firstOrNull { it.name == k } } ?: return
        val refId = intent.getLongExtra(EXTRA_REF, -1).takeIf { it > 0 } ?: return
        val ym = intent.getIntExtra(EXTRA_YM, -1).takeIf { it > 0 } ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF, 0)
        val app = context.kasa
        val key = "${kind.name}_${refId}_$ym"

        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    ACTION_PAID -> {
                        val amount = currentAmount(app, kind, refId)
                        if (amount != null) app.repository.markPaid(kind, refId, ym, amount)
                    }
                    ACTION_SNOOZE -> app.settings.snooze(key, System.currentTimeMillis() + 60 * 60 * 1000L)
                }
                Notifier.cancel(context, notifId)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun currentAmount(app: com.karlor.kasa.KasaApp, kind: ObligationKind, refId: Long): Long? {
        val db = app.database
        return when (kind) {
            ObligationKind.RECURRING -> db.recurring().get(refId)?.amount
            ObligationKind.CARD -> db.cards().get(refId)?.statementDebt?.takeIf { it > 0 }
            ObligationKind.LOAN -> db.loans().get(refId)?.installment
        }
    }

    companion object {
        const val ACTION_PAID = "com.karlor.kasa.action.PAID"
        const val ACTION_SNOOZE = "com.karlor.kasa.action.SNOOZE"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_REF = "ref"
        private const val EXTRA_YM = "ym"
        private const val EXTRA_NOTIF = "notif"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun intent(ctx: Context, action: String, o: Obligation, notifId: Int): PendingIntent {
            val i = Intent(ctx, ReminderActionReceiver::class.java).setAction(action)
                .putExtra(EXTRA_KIND, o.kind.name)
                .putExtra(EXTRA_REF, o.refId)
                .putExtra(EXTRA_YM, o.ym)
                .putExtra(EXTRA_NOTIF, notifId)
            val req = 31 * notifId + action.hashCode()
            return PendingIntent.getBroadcast(ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
