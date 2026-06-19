package com.tgws.proxy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * DotGridPattern — a minimal decorative background that replaces the previous
 * animated chat bubbles. Renders a subtle dot grid that gently fades in when
 * the proxy becomes active and slowly drifts to add depth without distracting
 * from the main toggle.
 *
 * The previous ChatBubble-based [ChatBackground] composable is intentionally
 * removed; this [Box]-friendly [Composable] keeps the same public name so
 * ConnectionTab can call ChatBackground(...) without changes.
 */
@Composable
fun ChatBackground(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.colors
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.22f

    val dotColor = if (isDark) {
        if (isActive) tokens.telegramBlue.copy(alpha = 0.30f) else colors.onSurface.copy(alpha = 0.10f)
    } else {
        if (isActive) colors.primary.copy(alpha = 0.22f) else colors.onSurface.copy(alpha = 0.08f)
    }

    val transition = rememberInfiniteTransition(label = "dot_grid")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 9000 else 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )

    val alpha by transition.animateFloat(
        initialValue = if (isActive) 0.85f else 0.55f,
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha_pulse",
    )

    val spacingPx = with(LocalDensity.current) { 28.dp.toPx() }
    val dotRadiusPx = with(LocalDensity.current) { 1.6.dp.toPx() }
    val driftOffset = (drift * spacingPx) - spacingPx

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val w = size.width
            val h = size.height
            val cols = (w / spacingPx).toInt() + 2
            val rows = (h / spacingPx).toInt() + 2

            // Radial vignette: stronger in the center, fades at the edges
            val cx = w * 0.5f
            val cy = h * 0.5f
            val maxR = kotlin.math.sqrt(w * w + h * h) * 0.5f

            var row = 0
            while (row < rows) {
                val y = row * spacingPx + driftOffset
                var col = 0
                while (col < cols) {
                    val x = col * spacingPx + driftOffset
                    val dx = x - cx
                    val dy = y - cy
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    val fade = (1f - (dist / maxR)).coerceIn(0f, 1f)
                    val a = alpha * fade
                    if (a > 0.02f) {
                        drawCircle(
                            color = dotColor.copy(alpha = a),
                            radius = dotRadiusPx,
                            center = Offset(x, y),
                        )
                    }
                    col++
                }
                row++
            }
        }
    }
}
