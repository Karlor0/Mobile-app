package com.karlor.kasa.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.karlor.kasa.data.AppSettings
import com.karlor.kasa.notify.Notifier
import com.karlor.kasa.ui.components.GlassCard
import com.karlor.kasa.ui.components.IconBadge
import com.karlor.kasa.ui.components.SectionHeader
import com.karlor.kasa.ui.theme.Kasa
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(
    settings: AppSettings,
    lockSupported: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onTestReminders: () -> Unit,
    onExport: suspend (android.net.Uri) -> Boolean,
    onWipe: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Kasa.colors
    var confirmWipe by remember { mutableIntStateOf(0) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { onMessage(if (onExport(uri)) "Dışa aktarıldı" else "Dışa aktarma başarısız") }
    }
    val notifAllowed = Notifier.canPost(ctx)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 8.dp, bottom = contentPadding.calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
            Text("Ayarlar", style = MaterialTheme.typography.headlineMedium)
        }

        GlassCard {
            SectionHeader("Bildirimler")
            if (!notifAllowed) {
                Surface(color = c.warning.copy(alpha = 0.14f), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NotificationsOff, null, tint = c.warning)
                        Spacer(Modifier.width(10.dp))
                        Text("Bildirim izni kapalı. Hatırlatmalar gösterilemez.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) { Text("Aç") }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            ToggleRow(Icons.Rounded.NotificationsActive, "Ödeme hatırlatıcıları", "Ödendi işaretlenmeyen ödemeler için artan sıklıkta bildirim", settings.notificationsEnabled) { v ->
                onUpdate { it.copy(notificationsEnabled = v) }
            }
            ToggleRow(Icons.Rounded.Bedtime, "Sessiz saatler", "23:00 – 08:00 arası bildirim gönderme", settings.quietHours) { v ->
                onUpdate { it.copy(quietHours = v) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Hatırlatmaya başla: son ödemeden ${settings.leadDays} gün önce", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.leadDays.toFloat(),
                onValueChange = { v -> onUpdate { it.copy(leadDays = v.toInt()) } },
                valueRange = 1f..15f, steps = 13,
            )
            Text(
                "Sıklık: ${settings.leadDays}–4 gün kala günde 1 · 3–2 gün kala 12 saatte 1 · son gün öncesi 6 saatte 1 · son gün 3 saatte 1 · gecikmede 4 saatte 1",
                style = MaterialTheme.typography.labelSmall, color = c.muted,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onTestReminders(); onMessage("Hatırlatıcılar kontrol ediliyor…") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Şimdi kontrol et")
            }
        }

        GlassCard {
            SectionHeader("Güvenlik ve gizlilik")
            ToggleRow(
                Icons.Rounded.Fingerprint, "Uygulama kilidi",
                if (lockSupported) "Açılışta parmak izi / yüz / ekran kilidi iste" else "Cihazda ekran kilidi tanımlı değil",
                settings.appLock && lockSupported, enabled = lockSupported,
            ) { v -> onUpdate { it.copy(appLock = v) } }
            ToggleRow(Icons.Rounded.Screenshot, "Ekran görüntüsünü engelle", "Son uygulamalarda içeriği de gizler", settings.secureScreen) { v ->
                onUpdate { it.copy(secureScreen = v) }
            }
            ToggleRow(Icons.Rounded.VisibilityOff, "Tutarları gizle", "Tüm tutarları ₺ ••••• olarak göster", settings.hideAmounts) { v ->
                onUpdate { it.copy(hideAmounts = v) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Verileriniz yalnızca bu cihazda saklanır. Uygulamanın internet izni yoktur ve bulut yedeklemesi devre dışıdır.",
                style = MaterialTheme.typography.labelSmall, color = c.muted,
            )
        }

        GlassCard {
            SectionHeader("Görünüm")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Sistem", "Açık", "Koyu").forEachIndexed { i, l ->
                    SegmentedButton(selected = settings.themeMode == i, onClick = { onUpdate { it.copy(themeMode = i) } }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
                }
            }
        }

        GlassCard {
            SectionHeader("Veri")
            ActionRow(Icons.Rounded.FileDownload, "İşlemleri dışa aktar (CSV)", "Excel / Sheets ile açılabilir") {
                exporter.launch("kasa-islemler-${LocalDate.now()}.csv")
            }
            ActionRow(Icons.Rounded.DeleteForever, "Tüm verileri sil", "Geri alınamaz", danger = true) { confirmWipe = 1 }
        }
        Text("Kasa 1.0.0", style = MaterialTheme.typography.labelSmall, color = c.muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }

    if (confirmWipe > 0) {
        AlertDialog(
            onDismissRequest = { confirmWipe = 0 },
            title = { Text(if (confirmWipe == 1) "Tüm veriler silinsin mi?" else "Emin misiniz?") },
            text = { Text(if (confirmWipe == 1) "Tüm işlemler, sabit giderler, kartlar ve krediler kalıcı olarak silinecek." else "Bu işlem geri alınamaz. Son onay.") },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmWipe == 1) confirmWipe = 2 else { onWipe(); confirmWipe = 0; onMessage("Tüm veriler silindi") }
                }) { Text(if (confirmWipe == 1) "Devam" else "Kalıcı olarak sil", color = c.expense) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = 0 }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, MaterialTheme.colorScheme.primary, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Kasa.colors.muted)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, sub: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) Kasa.colors.expense else MaterialTheme.colorScheme.primary
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) color else MaterialTheme.colorScheme.onSurface)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = Kasa.colors.muted)
            }
        }
    }
}
