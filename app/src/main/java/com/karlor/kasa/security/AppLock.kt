package com.karlor.kasa.security

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/** Device-credential / biometric gate built on the platform BiometricPrompt (API 28+). */
object AppLock {
    fun isSupported(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val km = activity.getSystemService(KeyguardManager::class.java) ?: return false
        return km.isDeviceSecure
    }

    fun authenticate(activity: Activity, onSuccess: () -> Unit, onFail: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) { onSuccess(); return }
        val executor = ContextCompat.getMainExecutor(activity)
        val builder = BiometricPrompt.Builder(activity)
            .setTitle("Kasa kilidi")
            .setSubtitle("Devam etmek için kimliğinizi doğrulayın")
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                @Suppress("DEPRECATION") builder.setDeviceCredentialAllowed(true)
            else -> builder.setNegativeButton("İptal", executor) { _, _ -> onFail("İptal edildi") }
        }
        builder.build().authenticate(
            CancellationSignal(), executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) =
                    onFail(errString?.toString() ?: "Doğrulama başarısız")
            },
        )
    }
}
