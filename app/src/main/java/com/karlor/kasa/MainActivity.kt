package com.karlor.kasa

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.karlor.kasa.notify.ReminderScheduler
import com.karlor.kasa.security.AppLock
import com.karlor.kasa.ui.AppRoot
import com.karlor.kasa.ui.MainViewModel
import com.karlor.kasa.ui.theme.Kasa
import com.karlor.kasa.ui.theme.KasaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var locked by mutableStateOf(false)
    private var lastBackground = 0L
    private var authInFlight = false

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        ReminderScheduler.runNow(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = kasa.settings
        locked = settings.state.value.appLock && AppLock.isSupported(this) && savedInstanceState == null

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                settings.state.collect { s ->
                    if (s.secureScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 33 && savedInstanceState == null) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderScheduler.runNow(this)

        setContent {
            val s by settings.state.collectAsState()
            KasaTheme(s.themeMode) {
                if (locked) LockScreen { unlock() } else AppRoot(vm, AppLock.isSupported(this))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val lockEnabled = kasa.settings.state.value.appLock && AppLock.isSupported(this)
        // Re-lock after 30 s in background.
        if (lockEnabled && lastBackground > 0 && SystemClock.elapsedRealtime() - lastBackground > 30_000) locked = true
        if (locked) unlock()
    }

    override fun onStop() {
        super.onStop()
        if (!authInFlight) lastBackground = SystemClock.elapsedRealtime()
    }

    private fun unlock() {
        if (authInFlight) return
        authInFlight = true
        AppLock.authenticate(
            this,
            onSuccess = { authInFlight = false; locked = false; lastBackground = 0 },
            onFail = { authInFlight = false },
        )
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(Kasa.colors.heroBrush),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(44.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Kasa kilitli", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("Devam etmek için kimliğinizi doğrulayın", color = Kasa.colors.muted)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock, shape = RoundedCornerShape(18.dp)) { Text("Kilidi aç") }
        }
    }
}
