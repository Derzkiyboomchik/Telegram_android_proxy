package com.tgws.proxy.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgws.proxy.ProxyController
import com.tgws.proxy.ProxyService
import com.tgws.proxy.R
import com.tgws.proxy.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConnectionTab(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val isRunning by ProxyService.isRunning.collectAsStateWithLifecycle()

    val isReady by settingsStore.isReady.collectAsStateWithLifecycle(initialValue = false)
    val savedPort by settingsStore.port.collectAsStateWithLifecycle(initialValue = "1443")
    val savedCfEnabled by settingsStore.cfproxyEnabled.collectAsStateWithLifecycle(initialValue = true)
    val savedPoolSize by settingsStore.poolSize.collectAsStateWithLifecycle(initialValue = 4)
    val savedSecretKey by settingsStore.secretKey.collectAsStateWithLifecycle(initialValue = "LOADING")

    val scope = rememberCoroutineScope()

    if (!isReady || savedSecretKey == "LOADING") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        }
        return
    }

    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ProxyService.isRunning.collect { running ->
            if (running) delay(600)
            isProcessing = false
        }
    }

    val statusText = when {
        isProcessing -> "Подключение"
        isRunning -> "Подключено"
        else -> "Отключено"
    }

    val port = savedPort.toIntOrNull() ?: 1443
    val secretForUrl = remember(savedSecretKey) {
        val raw = savedSecretKey.trim()
        if (raw.isNotEmpty() && raw != "LOADING") raw else "00000000000000000000000000000000"
    }
    val secretPrefix = when {
        secretForUrl.startsWith("dd", ignoreCase = true) || secretForUrl.startsWith("ee", ignoreCase = true) -> ""
        else -> "dd"
    }
    val proxyUrl = "https://t.me/proxy?server=127.0.0.1&port=$port&secret=${secretPrefix}$secretForUrl"

    val onToggle = {
        if (!isProcessing) {
            isProcessing = true
            if (isRunning) {
                ProxyController.stop(context)
            } else {
                scope.launch {
                    val started = ProxyController.startFromSavedSettings(
                        context = context,
                        showInvalidPortToast = true,
                    )
                    if (!started) isProcessing = false
                }
            }
        }
    }

    val isActiveVisual = isRunning || isProcessing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 0.dp, bottom = 16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Запуск",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hero — toggle button + status
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Subtle decorative dot grid background — replaces the previous
            // animated chat bubbles. Sits behind the toggle area.
            ChatBackground(
                isActive = isActiveVisual,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PowerToggle(
                    isActive = isActiveVisual,
                    isProcessing = isProcessing,
                    onToggle = onToggle,
                )

                Spacer(modifier = Modifier.height(28.dp))

                StatusIndicator(
                    statusText = statusText,
                    isActive = isActiveVisual,
                    isProcessing = isProcessing,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Telegram launch pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TelegramPill(
                        label = "Telegram",
                        enabled = isRunning,
                        modifier = Modifier.weight(1f),
                        onClick = { openTelegram(context, proxyUrl, "org.telegram.messenger") },
                    )
                    TelegramPill(
                        label = "Beta",
                        enabled = isRunning,
                        modifier = Modifier.weight(1f),
                        onClick = { openTelegram(context, proxyUrl, "org.telegram.messenger.beta") },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compact chips row: CF / Pool / Port
        ConfigChipsRow(
            cfEnabled = savedCfEnabled,
            poolSize = savedPoolSize,
            port = savedPort,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tappable proxy link card
        ProxyLinkCard(proxyUrl = proxyUrl, context = context)

        Spacer(modifier = Modifier.height(12.dp))

        // Live stats footer
        LiveStatsRow(isActive = isActiveVisual)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Power toggle — 180dp circular button with Telegram logo, glow + spring squash
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun PowerToggle(
    isActive: Boolean,
    isProcessing: Boolean,
    onToggle: () -> Unit,
) {
    val tokens = AppTheme.colors
    val scheme = MaterialTheme.colorScheme

    // Press squash
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScaleX by animateFloatAsState(
        targetValue = if (isPressed) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "press_x",
    )
    val pressScaleY by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "press_y",
    )

    // Active scale & tint
    val logoScale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 0.94f,
        animationSpec = tween(durationMillis = 650, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "scale",
    )
    val tintColor by animateColorAsState(
        targetValue = if (isActive) tokens.telegramBlue else scheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "tint",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.55f,
        animationSpec = tween(durationMillis = 700),
        label = "logo_alpha",
    )

    // Connecting pulse ring
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_alpha",
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow (radial gradient) — only when active
        if (isActive) {
            val glowAlpha = if (isProcessing) 0.55f else 0.40f
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.telegramBlue.copy(alpha = glowAlpha),
                                tokens.neonViolet.copy(alpha = glowAlpha * 0.35f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        // Connecting pulse ring
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.connecting.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        // Static drop shadow under button — gives physical depth
        Box(
            modifier = Modifier
                .size(180.dp)
                .shadow(
                    elevation = if (isActive) 28.dp else 12.dp,
                    shape = CircleShape,
                    ambientColor = if (isActive) tokens.telegramBlue.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.20f),
                    spotColor = if (isActive) tokens.telegramBlue.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.25f),
                ),
        )

        // Button body
        val buttonColor by animateColorAsState(
            targetValue = if (isActive) lerp(tokens.telegramBlue, scheme.primary, 0.05f)
                          else scheme.surface,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "button_color",
        )
        val borderColor by animateColorAsState(
            targetValue = if (isActive) tokens.neonCyan.copy(alpha = 0.55f)
                          else scheme.outlineVariant.copy(alpha = 0.40f),
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "border",
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(buttonColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = !isProcessing,
                    onClick = onToggle,
                )
                .graphicsLayer {
                    scaleX = logoScale * pressScaleX
                    scaleY = logoScale * pressScaleY
                    cameraDistance = 18f * density
                    alpha = logoAlpha
                },
            contentAlignment = Alignment.Center,
        ) {
            // Outer ring (subtle border) — gradient when active
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    tokens.telegramBlue.copy(alpha = 0.0f),
                                    tokens.neonCyan.copy(alpha = 0.40f),
                                    tokens.neonViolet.copy(alpha = 0.35f),
                                    tokens.telegramBlue.copy(alpha = 0.0f),
                                ),
                            ),
                        ),
                )
            }
            // Telegram logo — preserved as the visual center of the toggle
            Image(
                painter = painterResource(id = R.drawable.ic_telegram_logo),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                colorFilter = ColorFilter.tint(tintColor, BlendMode.SrcIn),
            )
        }

        // Border ring on top (so it stays crisp above the gradient overlay)
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .then(
                    if (isActive) {
                        Modifier.graphicsLayer {
                            // No-op: keep border crisp via overlay below
                        }
                    } else Modifier,
                ),
        )
        // Visible border drawn via Surface border
        Surface(
            color = Color.Transparent,
            shape = CircleShape,
            border = BorderStroke(
                width = if (isActive) 1.5.dp else 1.dp,
                color = borderColor,
            ),
            modifier = Modifier.size(180.dp),
        ) {}
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Status indicator — large text + animated dot
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun StatusIndicator(
    statusText: String,
    isActive: Boolean,
    isProcessing: Boolean,
) {
    val tokens = AppTheme.colors
    val scheme = MaterialTheme.colorScheme

    val color = when {
        isProcessing -> tokens.connecting
        isActive -> tokens.connected
        else -> scheme.onSurfaceVariant
    }
    val colorState by animateColorAsState(targetValue = color, animationSpec = tween(400), label = "status_color")

    // Pulse for the indicator dot when connecting
    val pulse = rememberInfiniteTransition(label = "status_pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colorState.copy(alpha = if (isProcessing) dotAlpha else 1f)),
        )
        AnimatedContent(
            targetState = statusText,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "status_text",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colorState,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TelegramPill — pill-shaped launch buttons for Telegram / Beta
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun TelegramPill(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.colors

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.surfaceVariant.copy(alpha = 0.4f)
            else -> lerp(scheme.primaryContainer, tokens.telegramBlue, 0.10f)
        },
        animationSpec = tween(300),
        label = "pill_container",
    )
    val contentColor = if (enabled) scheme.onPrimaryContainer else scheme.onSurface.copy(alpha = 0.4f)
    val borderColor = if (enabled) tokens.telegramBlue.copy(alpha = 0.35f) else scheme.outline.copy(alpha = 0.2f)

    // Press squash
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f),
        label = "pill_scale",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_telegram_logo),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(contentColor, BlendMode.SrcIn),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ConfigChipsRow — 3 small chips: CF / Pool / Port
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ConfigChipsRow(
    cfEnabled: Boolean,
    poolSize: Int,
    port: String,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConfigChip(
            label = if (cfEnabled) "CF" else "Прямое",
            modifier = Modifier.weight(0.9f),
            accentColor = if (cfEnabled) tokens.telegramBlue else scheme.onSurfaceVariant,
        )
        ConfigChip(
            label = "Пул ×$poolSize",
            modifier = Modifier.weight(1.05f),
            accentColor = scheme.primary,
        )
        ConfigChip(
            label = "Порт $port",
            modifier = Modifier.weight(1.35f),
            accentColor = scheme.primary,
        )
    }
}

@Composable
private fun ConfigChip(
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(50),
        color = scheme.surfaceContainerHigh.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ProxyLinkCard — tappable card with link icon, truncated URL, copy action
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ProxyLinkCard(
    proxyUrl: String,
    context: Context,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.colors

    // Press squash
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(150),
        label = "link_scale",
    )

    Surface(
        onClick = {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cb.setPrimaryClip(android.content.ClipData.newPlainText("Proxy", proxyUrl))
            Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = AppShapes.Large,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
        interactionSource = interactionSource,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = tokens.telegramBlue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = proxyUrl,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Скопировать",
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// LiveStatsRow — placeholder for real-time up/down bytes & active connections
// The proxy service exposes status via isRunning only; we surface that plus
// a small "live" hint. When the service later exposes per-session counters,
// this row can be upgraded to read them.
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun LiveStatsRow(isActive: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Medium,
        color = scheme.surfaceContainerLow.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(
                label = "Соединение",
                value = if (isActive) "Активно" else "Ожидание",
                accent = if (isActive) tokens.connected else scheme.onSurfaceVariant,
            )
            StatItem(
                label = "Порт",
                value = "127.0.0.1",
                accent = scheme.primary,
            )
            StatItem(
                label = "Тип",
                value = "MTProto WS",
                accent = tokens.telegramBlue,
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Helpers
// ════════════════════════════════════════════════════════════════════════════
private fun openTelegram(context: Context, proxyUrl: String, packageName: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(proxyUrl))
    intent.setPackage(packageName)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Приложение не установлено", Toast.LENGTH_SHORT).show()
    }
}

// Local lerp — avoids a wide import just for one usage
private fun lerp(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
