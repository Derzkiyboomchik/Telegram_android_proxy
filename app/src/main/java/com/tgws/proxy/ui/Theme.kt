package com.tgws.proxy.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
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
// Shape system — Liquid Glass uses generous rounding
// ════════════════════════════════════════════════════════════════════════════
object AppShapes {
    val Small = RoundedCornerShape(14.dp)
    val Medium = RoundedCornerShape(20.dp)
    val Large = RoundedCornerShape(28.dp)
    val XLarge = RoundedCornerShape(36.dp)
    val Pill = RoundedCornerShape(50)
    val Capsule = RoundedCornerShape(50)
}

object AppElevation {
    val Level0 = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 12.dp
}

// ════════════════════════════════════════════════════════════════════════════
// LIQUID GLASS palettes
//
// Concept: a soft, neutral backdrop with one accent hue. Glass elements
// tint themselves from this accent. Three accents:
//   • Aurora — cyan / mint (cool)
//   • Sunset — coral / amber (warm)
//   • Graphite — neutral grey (monochrome)
//
// Cyber palette preserved for backwards compatibility (theme settings
// store still allows "cyber"), remapped to Aurora.
// ════════════════════════════════════════════════════════════════════════════

// — Aurora (cool cyan/mint) —
private val AuroraLight = lightColorScheme(
    primary = Color(0xFF0EA5B7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4EEF4),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Color(0xFF4A6470),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE9F4),
    onSecondaryContainer = Color(0xFF051F28),
    tertiary = Color(0xFF5555B7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1E0FF),
    onTertiaryContainer = Color(0xFF0F0B6B),
    background = Color(0xFFF5F7F8),
    onBackground = Color(0xFF161C1D),
    surface = Color(0xFFF5F7F8),
    onSurface = Color(0xFF161C1D),
    surfaceVariant = Color(0xFFD9E3E5),
    onSurfaceVariant = Color(0xFF3F494B),
    surfaceContainer = Color(0xFFE9EEEF),
    surfaceContainerHigh = Color(0xFFDDE3E5),
    surfaceContainerLow = Color(0xFFF1F4F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFD2D8DA),
    outline = Color(0xFF6F7A7C),
    outlineVariant = Color(0xFFBFC9CB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2B3132),
    inverseOnSurface = Color(0xFFECF1F2),
    inversePrimary = Color(0xFF6CD3E5),
    surfaceTint = Color(0xFF0EA5B7),
)

private val AuroraDark = darkColorScheme(
    primary = Color(0xFF6CD3E5),
    onPrimary = Color(0xFF003740),
    primaryContainer = Color(0xFF004F5C),
    onPrimaryContainer = Color(0xFFB4EEF4),
    secondary = Color(0xFFB1CCD7),
    onSecondary = Color(0xFF1B343F),
    secondaryContainer = Color(0xFF324B56),
    onSecondaryContainer = Color(0xFFCDE9F4),
    tertiary = Color(0xFFC2C0FF),
    onTertiary = Color(0xFF252279),
    tertiaryContainer = Color(0xFF3D3A90),
    onTertiaryContainer = Color(0xFFE1E0FF),
    background = Color(0xFF0B0E10),
    onBackground = Color(0xFFE2E3E4),
    surface = Color(0xFF0E1214),
    onSurface = Color(0xFFE2E3E4),
    surfaceVariant = Color(0xFF3F494B),
    onSurfaceVariant = Color(0xFFBFC9CB),
    surfaceContainer = Color(0xFF1A1F21),
    surfaceContainerHigh = Color(0xFF252B2D),
    surfaceContainerLow = Color(0xFF13171A),
    surfaceContainerLowest = Color(0xFF07090A),
    surfaceContainerHighest = Color(0xFF303638),
    outline = Color(0xFF899395),
    outlineVariant = Color(0xFF3F494B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE2E3E4),
    inverseOnSurface = Color(0xFF2B3132),
    inversePrimary = Color(0xFF0EA5B7),
    surfaceTint = Color(0xFF6CD3E5),
)

// — Sunset (warm coral/amber) —
private val SunsetLight = lightColorScheme(
    primary = Color(0xFFB5413B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFF775651),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF715C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCDFA6),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF221918),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF221918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainer = Color(0xFFFCEAE7),
    surfaceContainerHigh = Color(0xFFF6DFDC),
    surfaceContainerLow = Color(0xFFFFF2EF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF0D9D6),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BE),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    surfaceTint = Color(0xFFB5413B),
)

private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF8C2A21),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB6),
    onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF5D3F3B),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFE1C388),
    onTertiary = Color(0xFF402D05),
    tertiaryContainer = Color(0xFF584419),
    onTertiaryContainer = Color(0xFFFCDFA6),
    background = Color(0xFF100806),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF140B0A),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    surfaceContainer = Color(0xFF221816),
    surfaceContainerHigh = Color(0xFF2E2321),
    surfaceContainerLow = Color(0xFF181010),
    surfaceContainerLowest = Color(0xFF0A0504),
    surfaceContainerHighest = Color(0xFF3A2E2B),
    outline = Color(0xFFA08C89),
    outlineVariant = Color(0xFF534341),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    surfaceTint = Color(0xFFFFB4A8),
)

// — Graphite (monochrome) —
private val GraphiteLight = lightColorScheme(
    primary = Color(0xFF5C5F62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE1E3E5),
    onPrimaryContainer = Color(0xFF1F1C1C),
    secondary = Color(0xFF5C5F62),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E3E5),
    onSecondaryContainer = Color(0xFF1F1C1C),
    tertiary = Color(0xFF8C9199),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDF0F6),
    onTertiaryContainer = Color(0xFF252A33),
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2E4),
    onSurfaceVariant = Color(0xFF43474B),
    surfaceContainer = Color(0xFFEFF1F3),
    surfaceContainerHigh = Color(0xFFE5E7E9),
    surfaceContainerLow = Color(0xFFF8FAFB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFDBDDDF),
    outline = Color(0xFF73777B),
    outlineVariant = Color(0xFFC3C7CB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    surfaceTint = Color(0xFF5C5F62),
)

private val GraphiteDark = darkColorScheme(
    primary = Color(0xFFC5C7C9),
    onPrimary = Color(0xFF2C2F32),
    primaryContainer = Color(0xFF3C3F42),
    onPrimaryContainer = Color(0xFFE1E3E5),
    secondary = Color(0xFFC5C7C9),
    onSecondary = Color(0xFF2C2F32),
    secondaryContainer = Color(0xFF3C3F42),
    onSecondaryContainer = Color(0xFFE1E3E5),
    tertiary = Color(0xFFB7BAC3),
    onTertiary = Color(0xFF232732),
    tertiaryContainer = Color(0xFF393D49),
    onTertiaryContainer = Color(0xFFEDF0F6),
    background = Color(0xFF080808),
    onBackground = Color(0xFFE2E2E4),
    surface = Color(0xFF0B0B0C),
    onSurface = Color(0xFFE2E2E4),
    surfaceVariant = Color(0xFF43474B),
    onSurfaceVariant = Color(0xFFC3C7CB),
    surfaceContainer = Color(0xFF161617),
    surfaceContainerHigh = Color(0xFF222224),
    surfaceContainerLow = Color(0xFF101011),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerHighest = Color(0xFF2D2D2F),
    outline = Color(0xFF8D9195),
    outlineVariant = Color(0xFF43474B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    surfaceTint = Color(0xFFC5C7C9),
)

private fun getAppColorScheme(palette: String, isDark: Boolean): androidx.compose.material3.ColorScheme {
    return when (palette) {
        "sunset" -> if (isDark) SunsetDark else SunsetLight
        "graphite" -> if (isDark) GraphiteDark else GraphiteLight
        "aurora", "cyber", "indigo", "forest", "espresso" -> if (isDark) AuroraDark else AuroraLight
        else -> if (isDark) AuroraDark else AuroraLight
    }
}

// ════════════════════════════════════════════════════════════════════════════
// GlassTokens — the heart of the Liquid Glass system.
//
// These describe the translucent layers: tints, highlights, borders,
// shadows, refraction. All values are theme-aware.
// ════════════════════════════════════════════════════════════════════════════
data class GlassTokens(
    // Base glass surface — applied as a translucent overlay over the backdrop
    val surfaceTint: Color,
    val surfaceTintAlpha: Float,
    val surfaceHighlight: Color,
    val surfaceHighlightAlpha: Float,

    // Border — thin gradient outline that catches light
    val borderLight: Color,
    val borderDark: Color,
    val borderAlpha: Float,

    // Inner glow — for "lit" glass elements (active state)
    val innerGlow: Color,
    val innerGlowAlpha: Float,

    // Status colors — soft, low-saturation
    val connected: Color,
    val connecting: Color,
    val disconnected: Color,
    val error: Color,
    val warning: Color,

    // Log priority colors
    val logDebug: Color,
    val logInfo: Color,
    val logWarn: Color,
    val logError: Color,
    val logCounter: Color,

    // Backdrop orbs — for the floating light spheres in LiquidBackground
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,

    // Brand
    val telegramBlue: Color,
    val accent: Color,
)

val LocalGlassTokens = staticCompositionLocalOf<GlassTokens> {
    error("GlassTokens not provided")
}

@Composable
@ReadOnlyComposable
private fun lightGlassTokens(scheme: androidx.compose.material3.ColorScheme): GlassTokens {
    return GlassTokens(
        surfaceTint = scheme.primary,
        surfaceTintAlpha = 0.06f,
        surfaceHighlight = Color.White,
        surfaceHighlightAlpha = 0.55f,
        borderLight = Color.White,
        borderDark = scheme.outline,
        borderAlpha = 0.65f,
        innerGlow = scheme.primary,
        innerGlowAlpha = 0.18f,
        connected = Color(0xFF2E7D32),
        connecting = scheme.primary,
        disconnected = scheme.onSurfaceVariant,
        error = scheme.error,
        warning = Color(0xFFB85F00),
        logDebug = scheme.tertiary,
        logInfo = scheme.primary,
        logWarn = Color(0xFFB85F00),
        logError = scheme.error,
        logCounter = scheme.primary,
        orb1 = scheme.primary.copy(alpha = 0.28f),
        orb2 = scheme.tertiary.copy(alpha = 0.22f),
        orb3 = scheme.secondary.copy(alpha = 0.20f),
        telegramBlue = Color(0xFF2AABEE),
        accent = scheme.primary,
    )
}

@Composable
@ReadOnlyComposable
private fun darkGlassTokens(scheme: androidx.compose.material3.ColorScheme): GlassTokens {
    return GlassTokens(
        surfaceTint = scheme.primary,
        surfaceTintAlpha = 0.10f,
        surfaceHighlight = Color.White,
        surfaceHighlightAlpha = 0.18f,
        borderLight = Color.White,
        borderDark = scheme.outlineVariant,
        borderAlpha = 0.45f,
        innerGlow = scheme.primary,
        innerGlowAlpha = 0.32f,
        connected = Color(0xFF6FCF7A),
        connecting = scheme.primary,
        disconnected = scheme.onSurfaceVariant,
        error = scheme.error,
        warning = Color(0xFFFFCC80),
        logDebug = scheme.tertiary,
        logInfo = scheme.primary,
        logWarn = Color(0xFFFFCC80),
        logError = scheme.error,
        logCounter = scheme.primary,
        orb1 = scheme.primary.copy(alpha = 0.42f),
        orb2 = scheme.tertiary.copy(alpha = 0.32f),
        orb3 = scheme.secondary.copy(alpha = 0.28f),
        telegramBlue = Color(0xFF2AABEE),
        accent = scheme.primary,
    )
}

// Backwards-compat shim
object AppColors {
    val telegramBlue: Color = Color(0xFF2AABEE)
    val neonCyan: Color = Color(0xFF00E5FF)
    val neonViolet: Color = Color(0xFF7B61FF)
    val connected: Color = Color(0xFF4CAF50)
    val connectedDark: Color = Color(0xFF81C784)
    val warning: Color = Color(0xFFFFA726)
    val warningDark: Color = Color(0xFFFFCC80)
}

object AppTheme {
    val glass: GlassTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassTokens.current
}

// ════════════════════════════════════════════════════════════════════════════
// Theme entry point
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun TgWsProxyTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = false,
    themePalette: String = "aurora",
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

    val glassTokens = if (darkTheme) darkGlassTokens(colorScheme) else lightGlassTokens(colorScheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
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

    androidx.compose.runtime.CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TgWsProxyTypography,
            content = content,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Glass brushes — reusable gradients for glass surfaces
// ════════════════════════════════════════════════════════════════════════════
@Composable
@ReadOnlyComposable
fun glassSurfaceBrush(): Brush {
    val tokens = LocalGlassTokens.current
    return Brush.linearGradient(
        colors = listOf(
            tokens.surfaceHighlight.copy(alpha = tokens.surfaceHighlightAlpha),
            tokens.surfaceTint.copy(alpha = tokens.surfaceTintAlpha),
            Color.Transparent,
        ),
    )
}

@Composable
@ReadOnlyComposable
fun glassBorderBrush(): Brush {
    val tokens = LocalGlassTokens.current
    return Brush.linearGradient(
        colors = listOf(
            tokens.borderLight.copy(alpha = tokens.borderAlpha),
            tokens.borderDark.copy(alpha = 0.0f),
            tokens.borderLight.copy(alpha = tokens.borderAlpha * 0.6f),
        ),
    )
}

@Composable
@ReadOnlyComposable
fun glassGlowBrush(): Brush {
    val tokens = LocalGlassTokens.current
    return Brush.radialGradient(
        colors = listOf(
            tokens.innerGlow.copy(alpha = tokens.innerGlowAlpha),
            Color.Transparent,
        ),
    )
}
