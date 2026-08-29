package com.tgws.proxy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * AppSectionCard — Telegram settings-style group container.
 *
 * Flat opaque card (white in light theme, elevated surface in dark),
 * 14dp rounded corners, a hairline separator outline and no glass
 * layers, gradients or highlights. Reused across Connection / Settings / Logs.
 */
@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.22f

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) scheme.surfaceContainerHigh else Color.White,
        border = BorderStroke(0.5.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
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

// Helper kept private — local luminance check to avoid recomposition churn
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
