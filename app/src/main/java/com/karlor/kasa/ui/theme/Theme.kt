package com.karlor.kasa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class KasaPalette(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val success: Color,
    val muted: Color,
    val cardBorder: Color,
    val heroGradient: List<Color>,
    val glass: Color,
    val chart: List<Color>,
    val isDark: Boolean,
) {
    val heroBrush: Brush get() = Brush.linearGradient(heroGradient)
}

private val chartColors = listOf(
    Color(0xFF7C6CFF), Color(0xFF2DD4BF), Color(0xFFFF7A90), Color(0xFFFFB547), Color(0xFF38BDF8),
    Color(0xFFA78BFA), Color(0xFF4ADE80), Color(0xFFF472B6), Color(0xFFFB923C), Color(0xFF94A3B8),
)

/** Gradients for credit card visuals. */
val CardGradients = listOf(
    listOf(Color(0xFF6D5DFC), Color(0xFF3B2FBF)),
    listOf(Color(0xFF0EA5A4), Color(0xFF0B5F7A)),
    listOf(Color(0xFFFF6B81), Color(0xFFB0306B)),
    listOf(Color(0xFF1F2937), Color(0xFF0B0F1A)),
    listOf(Color(0xFFF59E0B), Color(0xFFC2410C)),
    listOf(Color(0xFF22C55E), Color(0xFF166534)),
    listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)),
    listOf(Color(0xFFC084FC), Color(0xFF7E22CE)),
)

private val DarkPalette = KasaPalette(
    income = Color(0xFF34E0A1),
    expense = Color(0xFFFF6B81),
    warning = Color(0xFFFFB547),
    success = Color(0xFF34E0A1),
    muted = Color(0xFF8B93A7),
    cardBorder = Color(0x1AFFFFFF),
    heroGradient = listOf(Color(0xFF7B5CFF), Color(0xFF4F46E5), Color(0xFF1FA2C9)),
    glass = Color(0x14FFFFFF),
    chart = chartColors,
    isDark = true,
)

private val LightPalette = KasaPalette(
    income = Color(0xFF0FA97A),
    expense = Color(0xFFE5485F),
    warning = Color(0xFFE08A00),
    success = Color(0xFF0FA97A),
    muted = Color(0xFF6B7287),
    cardBorder = Color(0x14101828),
    heroGradient = listOf(Color(0xFF7B5CFF), Color(0xFF5145F0), Color(0xFF22B5D6)),
    glass = Color(0x0F101828),
    chart = chartColors,
    isDark = false,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8B7CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2463),
    onPrimaryContainer = Color(0xFFE3DFFF),
    secondary = Color(0xFF34E0A1),
    onSecondary = Color(0xFF00281A),
    tertiary = Color(0xFFFF6B81),
    background = Color(0xFF0A0E19),
    onBackground = Color(0xFFE9ECF5),
    surface = Color(0xFF111726),
    onSurface = Color(0xFFE9ECF5),
    surfaceVariant = Color(0xFF1A2133),
    onSurfaceVariant = Color(0xFFA7AEC2),
    surfaceContainer = Color(0xFF151C2D),
    surfaceContainerHigh = Color(0xFF1B2337),
    surfaceContainerHighest = Color(0xFF222B41),
    surfaceContainerLow = Color(0xFF0F1523),
    outline = Color(0xFF39425A),
    outlineVariant = Color(0xFF262E42),
    error = Color(0xFFFF6B81),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B4BF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E2FF),
    onPrimaryContainer = Color(0xFF1C1366),
    secondary = Color(0xFF0FA97A),
    tertiary = Color(0xFFE5485F),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF5B6275),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF7F8FC),
    surfaceContainerHighest = Color(0xFFEEF0F7),
    surfaceContainerLow = Color(0xFFFAFBFE),
    outline = Color(0xFFCDD2DF),
    outlineVariant = Color(0xFFE3E6EF),
    error = Color(0xFFE5485F),
)

private val base = Typography()
private val KasaTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
)

val MoneyStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)

private val KasaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val LocalKasa = staticCompositionLocalOf { DarkPalette }

object Kasa {
    val colors: KasaPalette @Composable get() = LocalKasa.current
}

@Composable
fun KasaTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalKasa provides if (dark) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = KasaTypography,
            shapes = KasaShapes,
            content = content,
        )
    }
}
