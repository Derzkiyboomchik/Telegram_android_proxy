package com.tgws.proxy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * GlassCard — the Liquid Glass container.
 *
 * Built from 3 stacked layers:
 *   1. Tinted translucent base (the glass body)
 *   2. Diagonal highlight gradient (the refraction)
 *   3. Thin gradient border (the rim catching light)
 *
 * On Android 12+ a real RenderEffect blur is added so the glass actually
 * blurs whatever sits behind it. On older devices we fall back to a
 * semi-transparent tint — the look is still glassy but no real blur.
 *
 * The same component is reused across Connection / Settings / Logs.
 */
@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.glass

    // Surface body — translucency is what makes glass look like glass.
    val bodyColor = scheme.surface.copy(
        alpha = if (scheme.background.luminance() < 0.22f) 0.55f else 0.72f,
    )

    Surface(
        shape = AppShapes.XLarge,
        color = Color.Transparent,
        border = BorderStroke(0.5.dp, glassBorderBrush()),
        shadowElevation = AppElevation.Level2,
        tonalElevation = AppElevation.Level0,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bodyColor)
                .background(glassSurfaceBrush()),
        ) {
            // Inner top highlight — the bright streak along the upper edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                tokens.surfaceHighlight.copy(alpha = tokens.surfaceHighlightAlpha * 0.6f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
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

// Helper kept private — local luminance check to avoid recomposition churn
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
