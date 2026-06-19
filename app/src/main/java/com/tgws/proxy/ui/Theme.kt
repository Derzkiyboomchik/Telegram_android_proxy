package com.tgws.proxy.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.tgws.proxy.R

// ════════════════════════════════════════════════════════════════════════════
// Inter Font Family
// ════════════════════════════════════════════════════════════════════════════
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// ════════════════════════════════════════════════════════════════════════════
// Typography — Inter-based, Material 3 scale
// ════════════════════════════════════════════════════════════════════════════
val TgWsProxyTypography = Typography(
    displayLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ════════════════════════════════════════════════════════════════════════════
// Shape system — Material 3 tokenized corner radii
// ════════════════════════════════════════════════════════════════════════════
object AppShapes {
    val Small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val Medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val Large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val XLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val Pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
}

// ════════════════════════════════════════════════════════════════════════════
// Elevation system — tonal elevation levels
// ════════════════════════════════════════════════════════════════════════════
object AppElevation {
    val Level0 = 0.dp
    val Level1 = 2.dp
    val Level2 = 4.dp
    val Level3 = 8.dp
    val Level4 = 12.dp
}

// ════════════════════════════════════════════════════════════════════════════
// Color palettes
// ════════════════════════════════════════════════════════════════════════════

// — Espresso — warm brown / cream
private val EspressoLightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7CCC8),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF8D6E63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFEBE9),
    onSecondaryContainer = Color(0xFF4E342E),
    tertiary = Color(0xFF795548),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCAAA4),
    onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFF5F0EB),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEFEBE9),
    onSurfaceVariant = Color(0xFF5D4037),
    surfaceContainer = Color(0xFFEFE9E3),
    surfaceContainerHigh = Color(0xFFE9E2DB),
    surfaceContainerLow = Color(0xFFFAF5EE),
    outline = Color(0xFFBCAAA4),
    outlineVariant = Color(0xFFD7CCC8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF322F2D),
    inverseOnSurface = Color(0xFFF5F0EB),
    inversePrimary = Color(0xFFD7CCC8),
    surfaceTint = Color(0xFF6D4C41),
)

private val EspressoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD7CCC8),
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFEFEBE9),
    secondary = Color(0xFFBCAAA4),
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFF4E342E),
    onSecondaryContainer = Color(0xFFEFEBE9),
    tertiary = Color(0xFFA1887F),
    onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFF5D4037),
    onTertiaryContainer = Color(0xFFEFEBE9),
    background = Color(0xFF1A1614),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF211D1B),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF2C2624),
    onSurfaceVariant = Color(0xFFD7CCC8),
    surfaceContainer = Color(0xFF2B2622),
    surfaceContainerHigh = Color(0xFF35302C),
    surfaceContainerLow = Color(0xFF1E1A18),
    outline = Color(0xFF8D6E63),
    outlineVariant = Color(0xFF4E342E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF322F2D),
    inversePrimary = Color(0xFF6D4C41),
    surfaceTint = Color(0xFFD7CCC8),
)

// — Indigo — soft violet
private val IndigoLightColorScheme = lightColorScheme(
    primary = Color(0xFF5B588D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2DFFF),
    onPrimaryContainer = Color(0xFF1A1744),
    secondary = Color(0xFF5B588D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2DFFF),
    onSecondaryContainer = Color(0xFF1A1744),
    tertiary = Color(0xFF7B61FF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DEFF),
    onTertiaryContainer = Color(0xFF2200CC),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF6F3FA),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceContainer = Color(0xFFEDEAF4),
    surfaceContainerHigh = Color(0xFFE4E1EC),
    surfaceContainerLow = Color(0xFFF9F7FD),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val IndigoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC4C0FF),
    onPrimary = Color(0xFF2D2A5B),
    primaryContainer = Color(0xFF434073),
    onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFFC4C0FF),
    onSecondary = Color(0xFF2D2A5B),
    secondaryContainer = Color(0xFF434073),
    onSecondaryContainer = Color(0xFFE2DFFF),
    tertiary = Color(0xFFCFBCFF),
    onTertiary = Color(0xFF3A1C99),
    tertiaryContainer = Color(0xFF5336C5),
    onTertiaryContainer = Color(0xFFE8DEFF),
    background = Color(0xFF131316),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainer = Color(0xFF26242C),
    surfaceContainerHigh = Color(0xFF322F3A),
    surfaceContainerLow = Color(0xFF18171A),
    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF47464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

// — Forest — slate green
private val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF5F5D68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E0F0),
    onPrimaryContainer = Color(0xFF1C1A23),
    secondary = Color(0xFF5F5D68),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E0F0),
    onSecondaryContainer = Color(0xFF1C1A23),
    tertiary = Color(0xFF3F6B5C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFF7F2FA),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFF48454E),
    surfaceContainer = Color(0xFFEEEAF1),
    surfaceContainerHigh = Color(0xFFE6E0E9),
    surfaceContainerLow = Color(0xFFF9F5FD),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC8C4D3),
    onPrimary = Color(0xFF312F38),
    primaryContainer = Color(0xFF474550),
    onPrimaryContainer = Color(0xFFE5E0F0),
    secondary = Color(0xFFC8C4D3),
    onSecondary = Color(0xFF312F38),
    secondaryContainer = Color(0xFF474550),
    onSecondaryContainer = Color(0xFFE5E0F0),
    tertiary = Color(0xFFA1D3BF),
    onTertiary = Color(0xFF063828),
    background = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFCAC4D0),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF27252B),
    surfaceContainerHigh = Color(0xFF333039),
    surfaceContainerLow = Color(0xFF19171C),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF48454E),
)

// — Cyber — Telegram neon dark-only
private val CyberDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2AABEE),
    onPrimary = Color(0xFF00121F),
    primaryContainer = Color(0xFF0D2538),
    onPrimaryContainer = Color(0xFFB8E5FF),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF001A1F),
    secondaryContainer = Color(0xFF0A2E33),
    onSecondaryContainer = Color(0xFFB3F5FF),
    tertiary = Color(0xFF7B61FF),
    onTertiary = Color(0xFF120033),
    tertiaryContainer = Color(0xFF1E1040),
    onTertiaryContainer = Color(0xFFD9CCFF),
    background = Color(0xFF05070A),
    onBackground = Color(0xFFE0E8F0),
    surface = Color(0xFF0A0D14),
    onSurface = Color(0xFFC8D4E0),
    surfaceVariant = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF8A9AAF),
    surfaceContainer = Color(0xFF12161F),
    surfaceContainerHigh = Color(0xFF1A2030),
    surfaceContainerLow = Color(0xFF080A10),
    outline = Color(0xFF1F3A55),
    outlineVariant = Color(0xFF162536),
    error = Color(0xFFFF5277),
    onError = Color(0xFF33000A),
    errorContainer = Color(0xFF4A0012),
    onErrorContainer = Color(0xFFFFB8C8),
    inverseSurface = Color(0xFFE0E8F0),
    inverseOnSurface = Color(0xFF05070A),
    inversePrimary = Color(0xFF005F99),
    surfaceTint = Color(0xFF2AABEE),
)

// Cyber light variant — used when user picks "cyber" + system light
private val CyberLightColorScheme = lightColorScheme(
    primary = Color(0xFF006D9C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E6FF),
    onPrimaryContainer = Color(0xFF001E2F),
    secondary = Color(0xFF006874),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9CECFF),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = Color(0xFF5A40C8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DEFF),
    onTertiaryContainer = Color(0xFF180068),
    background = Color(0xFFF5FAFE),
    onBackground = Color(0xFF001F2A),
    surface = Color(0xFFF5FAFE),
    onSurface = Color(0xFF001F2A),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    surfaceContainer = Color(0xFFE7EEF5),
    surfaceContainerHigh = Color(0xFFDDE5EC),
    surfaceContainerLow = Color(0xFFFAFCFE),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private fun getAppColorScheme(palette: String, isDark: Boolean): androidx.compose.material3.ColorScheme {
    return when (palette) {
        "espresso" -> if (isDark) EspressoDarkColorScheme else EspressoLightColorScheme
        "forest" -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
        "cyber" -> if (isDark) CyberDarkColorScheme else CyberLightColorScheme
        else -> if (isDark) IndigoDarkColorScheme else IndigoLightColorScheme
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Extended design tokens — status colors, gradients, brand constants.
// All theme-aware: callers read them through LocalAppColors at composition time.
// ════════════════════════════════════════════════════════════════════════════
data class AppColorTokens(
    // Status — connected / connecting / disconnected / error
    val connected: Color,
    val connectedContainer: Color,
    val onConnected: Color,
    val connecting: Color,
    val connectingContainer: Color,
    val onConnecting: Color,
    val disconnected: Color,
    val disconnectedContainer: Color,
    val onDisconnected: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,

    // Log priorities — theme-aware, not hardcoded
    val logDebug: Color,
    val logInfo: Color,
    val logWarn: Color,
    val logError: Color,
    val logCounter: Color,

    // Gradients — for glow / shimmer / accent surfaces
    val brandPrimaryGradient: List<Color>,
    val brandAccentGradient: List<Color>,
    val glowGradient: List<Color>,
    val surfaceGradient: List<Color>,
    val cardBorderGradient: List<Color>,

    // Brand
    val telegramBlue: Color,
    val neonCyan: Color,
    val neonViolet: Color,
    val github: Color,
    val donate: Color,
)

val LocalAppColors = staticCompositionLocalOf<AppColorTokens> {
    error("AppColorTokens not provided")
}

@Composable
@ReadOnlyComposable
private fun lightAppTokens(scheme: androidx.compose.material3.ColorScheme): AppColorTokens {
    return AppColorTokens(
        connected = Color(0xFF2E7D32),
        connectedContainer = Color(0xFFB8E6B8),
        onConnected = Color(0xFF0A3D0F),
        connecting = scheme.primary,
        connectingContainer = scheme.primaryContainer,
        onConnecting = scheme.onPrimaryContainer,
        disconnected = scheme.onSurfaceVariant,
        disconnectedContainer = scheme.surfaceVariant,
        onDisconnected = scheme.onSurface,
        error = scheme.error,
        errorContainer = scheme.errorContainer,
        onError = scheme.onError,
        warning = Color(0xFFB85F00),
        warningContainer = Color(0xFFFFD9A8),
        onWarning = Color(0xFF3B1F00),
        logDebug = scheme.tertiary,
        logInfo = scheme.primary,
        logWarn = Color(0xFFB85F00),
        logError = scheme.error,
        logCounter = scheme.primary,
        brandPrimaryGradient = listOf(scheme.primary, scheme.tertiary),
        brandAccentGradient = listOf(scheme.secondary, scheme.primary),
        glowGradient = listOf(scheme.primary.copy(alpha = 0.32f), Color.Transparent),
        surfaceGradient = listOf(
            lerp(scheme.surface, scheme.surfaceVariant, 0.35f),
            scheme.surface,
        ),
        cardBorderGradient = listOf(
            scheme.outline.copy(alpha = 0.30f),
            scheme.outlineVariant.copy(alpha = 0.18f),
        ),
        telegramBlue = Color(0xFF2AABEE),
        neonCyan = Color(0xFF00BBD4),
        neonViolet = Color(0xFF7B61FF),
        github = Color(0xFF24292F),
        donate = Color(0xFF7B61FF),
    )
}

@Composable
@ReadOnlyComposable
private fun darkAppTokens(scheme: androidx.compose.material3.ColorScheme, isCyber: Boolean): AppColorTokens {
    val connected = if (isCyber) Color(0xFF34D399) else Color(0xFF6FCF7A)
    val connectedContainer = connected.copy(alpha = 0.18f)
    val connecting = if (isCyber) scheme.primary else scheme.primary
    val connectingContainer = scheme.primaryContainer.copy(alpha = 0.5f)
    val warning = if (isCyber) Color(0xFFFFB020) else Color(0xFFFFCC80)
    return AppColorTokens(
        connected = connected,
        connectedContainer = connectedContainer,
        onConnected = Color(0xFFD8F5DC),
        connecting = connecting,
        connectingContainer = connectingContainer,
        onConnecting = scheme.onPrimaryContainer,
        disconnected = scheme.onSurfaceVariant,
        disconnectedContainer = scheme.surfaceVariant.copy(alpha = 0.5f),
        onDisconnected = scheme.onSurface,
        error = scheme.error,
        errorContainer = scheme.errorContainer,
        onError = scheme.onError,
        warning = warning,
        warningContainer = warning.copy(alpha = 0.16f),
        onWarning = Color(0xFFFFE0B0),
        logDebug = scheme.tertiary,
        logInfo = if (isCyber) Color(0xFF7DD3FC) else scheme.primary,
        logWarn = warning,
        logError = scheme.error,
        logCounter = if (isCyber) Color(0xFF00E5FF) else scheme.primary,
        brandPrimaryGradient = if (isCyber) listOf(Color(0xFF2AABEE), Color(0xFF7B61FF)) else listOf(scheme.primary, scheme.tertiary),
        brandAccentGradient = if (isCyber) listOf(Color(0xFF00E5FF), Color(0xFF2AABEE)) else listOf(scheme.secondary, scheme.primary),
        glowGradient = if (isCyber) listOf(Color(0xFF2AABEE).copy(alpha = 0.40f), Color(0xFF7B61FF).copy(alpha = 0.18f), Color.Transparent)
                       else listOf(scheme.primary.copy(alpha = 0.30f), Color.Transparent),
        surfaceGradient = listOf(
            lerp(scheme.surface, scheme.primary, 0.06f),
            scheme.surface,
            lerp(scheme.surface, scheme.background, 0.30f),
        ),
        cardBorderGradient = if (isCyber) listOf(Color(0xFF2AABEE).copy(alpha = 0.55f), Color(0xFF7B61FF).copy(alpha = 0.25f))
                             else listOf(lerp(scheme.outlineVariant, scheme.primary, 0.35f).copy(alpha = 0.55f), scheme.outlineVariant.copy(alpha = 0.30f)),
        telegramBlue = Color(0xFF2AABEE),
        neonCyan = Color(0xFF00E5FF),
        neonViolet = Color(0xFF7B61FF),
        github = Color(0xFF333C47),
        donate = Color(0xFF8B3FFD),
    )
}

// ════════════════════════════════════════════════════════════════════════════
// Backwards-compatible object — exposes brand constants to non-composable call
// sites. Status / theme-aware values should be read via `LocalAppColors.current`
// inside composables; this object keeps existing references (e.g. AppColors
// .telegramBlue) working during the migration.
// ════════════════════════════════════════════════════════════════════════════
object AppColors {
    val telegramBlue: Color = Color(0xFF2AABEE)
    val neonCyan: Color = Color(0xFF00E5FF)
    val neonViolet: Color = Color(0xFF7B61FF)
    val github: Color = Color(0xFF24292F)
    val donate: Color = Color(0xFF8B3FFD)

    // Status defaults (light theme). Prefer LocalAppColors.current.* inside @Composable.
    val connected: Color = Color(0xFF2E7D32)
    val connectedDark: Color = Color(0xFF6FCF7A)
    val warning: Color = Color(0xFFFFA726)
    val warningDark: Color = Color(0xFFFFCC80)
}

// ════════════════════════════════════════════════════════════════════════════
// Theme entry point
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun TgWsProxyTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    themePalette: String = "indigo",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getAppColorScheme(themePalette, darkTheme)
    }

    val appTokens = if (darkTheme) darkAppTokens(colorScheme, isCyber = themePalette == "cyber")
                    else lightAppTokens(colorScheme)

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val navigationBarColor = if (darkTheme) {
                Color.Transparent
            } else {
                lerp(colorScheme.background, colorScheme.surface, 0.55f)
            }
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = navigationBarColor.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAppColors provides appTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TgWsProxyTypography,
            content = content,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Convenience accessor — use inside composables instead of AppColors.* for
// theme-aware status / gradient colors.
// ════════════════════════════════════════════════════════════════════════════
object AppTheme {
    val colors: AppColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// Helpers for building reusable gradient brushes from the active token set.
fun linearGlowBrush(tokens: AppColorTokens, angle: Boolean = false): Brush =
    if (angle) Brush.verticalGradient(tokens.glowGradient)
    else Brush.horizontalGradient(tokens.glowGradient)

fun radialGlowBrush(tokens: AppColorTokens): Brush =
    Brush.radialGradient(tokens.glowGradient)
