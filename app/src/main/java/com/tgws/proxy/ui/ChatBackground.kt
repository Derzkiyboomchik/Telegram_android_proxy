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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * LiquidBackground — soft, slowly-drifting light orbs on a flat backdrop.
 *
 * The previous ChatBubble and DotGrid animations are gone. In their place
 * we render three blurred radial gradients ("orbs") that breathe and
 * drift across the screen — the same visual language used by Apple's
 * Liquid Glass backdrops and modern macOS Sonoma wallpapers.
 *
 * Kept under the public name ChatBackground(...) so ConnectionTab keeps
 * compiling without API changes.
 */
@Composable
fun ChatBackground(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val tokens = AppTheme.glass
    val isDark = scheme.background.luminance() < 0.22f

    // Three orbs, each with its own phase
    val t = rememberInfiniteTransition(label = "liquid")
    val p1 by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (isActive) 18000 else 30000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "p1",
    )
    val p2 by t.animateFloat(
        initialValue = 0.33f, targetValue = 1.33f,
        animationSpec = infiniteRepeatable(
            tween(if (isActive) 22000 else 36000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "p2",
    )
    val p3 by t.animateFloat(
        initialValue = 0.66f, targetValue = 1.66f,
        animationSpec = infiniteRepeatable(
            tween(if (isActive) 26000 else 42000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "p3",
    )

    // Activity pulse — brightens the orbs slightly when active
    val pulse by t.animateFloat(
        initialValue = if (isActive) 0.85f else 0.55f,
        targetValue = if (isActive) 1f else 0.7f,
        animationSpec = infiniteRepeatable(
            tween(3500, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawOrb(tokens.orb1, p1, w, h, 0.18f, 0.22f, 0.65f, pulse, isDark)
            drawOrb(tokens.orb2, p2, w, h, 0.82f, 0.30f, 0.55f, pulse, isDark)
            drawOrb(tokens.orb3, p3, w, h, 0.50f, 0.80f, 0.75f, pulse, isDark)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrb(
    color: Color,
    phase: Float,
    w: Float,
    h: Float,
    baseX: Float,
    baseY: Float,
    sizeFactor: Float,
    pulse: Float,
    isDark: Boolean,
) {
    // Two Lissajous-like orbits — feels organic without being literal.
    val angle = phase * 2f * Math.PI.toFloat()
    val cx = w * baseX + (Math.cos(angle.toDouble()).toFloat() * w * 0.10f)
    val cy = h * baseY + (Math.sin(angle.toDouble() * 1.3f).toFloat() * h * 0.08f)
    val radius = (minOf(w, h) * sizeFactor) * (0.9f + 0.1f * pulse)

    val a = (if (isDark) 1f else 0.85f) * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = a),
                color.copy(alpha = a * 0.45f),
                Color.Transparent,
            ),
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            radius = radius,
        ),
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        radius = radius,
    )
}
