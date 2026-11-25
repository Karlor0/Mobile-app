package com.karlor.kasa.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karlor.kasa.MainActivity
import com.karlor.kasa.R
import com.karlor.kasa.data.ObligationKind
import com.karlor.kasa.domain.Money
import com.karlor.kasa.domain.Months
import com.karlor.kasa.domain.Obligation

object Notifier {
    const val CH_UPCOMING = "upcoming_payments"
    const val CH_OVERDUE = "overdue_payments"
    private const val GROUP = "kasa_payments"
    private const val SUMMARY_ID = 1

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_UPCOMING, "Yaklaşan ödemeler", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Son ödeme günü yaklaşan faturalar, kartlar ve krediler"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_OVERDUE, "Geciken ödemeler", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Son ödeme tarihi geçmiş ve ödendi olarak işaretlenmemiş ödemeler"
                enableVibration(true)
            },
        )
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun notificationId(key: String): Int = key.hashCode().let { if (it == SUMMARY_ID) it + 1 else it }

    @android.annotation.SuppressLint("MissingPermission")
    fun post(ctx: Context, o: Obligation, daysLeft: Long, hidden: Boolean) {
        if (!canPost(ctx)) return
        val overdue = daysLeft < 0
        val id = notificationId(o.key)
        val amountTxt = when {
            o.kind == ObligationKind.CARD && o.amount <= 0 -> "Dönem borcunu girin"
            else -> Money.format(o.amount, hidden)
        }
        val title = when {
            overdue -> "⚠️ Gecikti: ${o.title}"
            daysLeft == 0L -> "⏰ Bugün son gün: ${o.title}"
            daysLeft == 1L -> "Yarın son gün: ${o.title}"
            else -> "${o.title} · ${daysLeft} gün kaldı"
        }
        val body = buildString {
            append(o.subtitle)
            o.installmentLabel?.let { append(" · ").append(it).append(". taksit") }
            append("\n").append(amountTxt)
            append(" · Son ödeme ").append(Months.dayMonth(o.dueDate))
            if (overdue) append("\n").append(Months.relative(daysLeft))
        }

        val open = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(ctx, if (overdue) CH_OVERDUE else CH_UPCOMING)
            .setSmallIcon(R.drawable.ic_stat_kasa)
            .setColor(if (overdue) 0xFFFF5A6E.toInt() else 0xFF6D5DFC.toInt())
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(ctx, CH_UPCOMING)
                    .setSmallIcon(R.drawable.ic_stat_kasa)
                    .setContentTitle("Yaklaşan ödeme hatırlatması")
                    .build(),
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setGroup(GROUP)

        // "Ödedim" only when the amount is known; card payments need the amount entered in-app.
        if (!(o.kind == ObligationKind.CARD && o.amount <= 0)) {
            builder.addAction(0, "✓ Ödedim", ReminderActionReceiver.intent(ctx, ReminderActionReceiver.ACTION_PAID, o, id))
        }
        builder.addAction(0, "1 saat ertele", ReminderActionReceiver.intent(ctx, ReminderActionReceiver.ACTION_SNOOZE, o, id))

        NotificationManagerCompat.from(ctx).notify(id, builder.build())
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun postSummary(ctx: Context, count: Int, total: Long, hidden: Boolean) {
        if (!canPost(ctx) || count < 2) return
        val n = NotificationCompat.Builder(ctx, CH_UPCOMING)
            .setSmallIcon(R.drawable.ic_stat_kasa)
            .setContentTitle("$count bekleyen ödeme")
            .setContentText("Toplam ${Money.format(total, hidden)}")
            .setGroup(GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(SUMMARY_ID, n)
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)
    fun cancelFor(ctx: Context, key: String) = cancel(ctx, notificationId(key))
}
