package com.tgws.proxy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * AppSectionCard — the unified container for grouped content across the app.
 *
 * Visual treatment:
 * - Subtle vertical gradient surface (lighter top, deeper bottom)
 * - Glow border that adapts to the active palette (cyber gets a stronger neon edge)
 * - Tonal elevation keeps the card lifted above the backdrop
 *
 * The same component is reused by Connection / Settings / Logs to give the
 * whole app a single visual rhythm.
 */
@Composable
private fun appSectionCardBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    val isDark = remember(colors.background) { colors.background.luminance() < 0.22f }
    return remember(isDark, colors.surface, colors.primary, colors.background, colors.surfaceVariant) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    lerp(colors.surface, colors.primary, 0.06f),
                    colors.surface,
                    lerp(colors.surface, colors.background, 0.30f),
                ),
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    lerp(colors.surface, colors.surfaceVariant, 0.35f),
                    colors.surface,
                ),
            )
        }
    }
}

@Composable
private fun appSectionCardBorderBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    val tokens = AppTheme.colors
    val isDark = remember(colors.background) { colors.background.luminance() < 0.22f }
    return remember(isDark, colors.outlineVariant, colors.outline, colors.primary, colors.surfaceVariant) {
        Brush.horizontalGradient(tokens.cardBorderGradient)
    }
}

@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = AppShapes.XLarge,
        color = Color.Transparent,
        border = BorderStroke(1.dp, appSectionCardBorderBrush()),
        shadowElevation = AppElevation.Level3,
        tonalElevation = AppElevation.Level1,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(appSectionCardBrush()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}
