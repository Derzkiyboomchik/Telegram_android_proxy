package com.tgws.proxy.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
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
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
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
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 16.dp),
    ) {
        // ── Page header ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Запуск",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Hero ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            ChatBackground(isActive = isActiveVisual, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlassToggle(
                    isActive = isActiveVisual,
                    isProcessing = isProcessing,
                    onToggle = onToggle,
                )

                Spacer(modifier = Modifier.height(36.dp))

                StatusLabel(
                    statusText = statusText,
                    isActive = isActiveVisual,
                    isProcessing = isProcessing,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Launch pills
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LaunchPill(
                        label = "Telegram",
                        enabled = isRunning,
                        modifier = Modifier.weight(1f),
                        onClick = { openTelegram(context, proxyUrl, "org.telegram.messenger") },
                    )
                    LaunchPill(
                        label = "Beta",
                        enabled = isRunning,
                        modifier = Modifier.weight(1f),
                        onClick = { openTelegram(context, proxyUrl, "org.telegram.messenger.beta") },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Bottom info cluster ─────────────────────────────────────────────
        InfoChipsRow(
            cfEnabled = savedCfEnabled,
            poolSize = savedPoolSize,
            port = savedPort,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProxyLinkCard(proxyUrl = proxyUrl, context = context)

        Spacer(modifier = Modifier.height(12.dp))

        SessionCard(isActive = isActiveVisual)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// GlassToggle — 200dp circular Liquid Glass button with Telegram logo
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun GlassToggle(
    isActive: Boolean,
    isProcessing: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.glass

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f),
        label = "press",
    )

    // Connecting pulse rings
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ringScale1 by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1",
    )
    val ringAlpha1 by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1a",
    )

    // Logo tint
    val tintColor by animateColorAsState(
        targetValue = if (isActive) Color.White else scheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "tint",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (isActive) 1.06f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 300f),
        label = "logo",
    )

    Box(
        modifier = Modifier
            .size(240.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isProcessing,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ambient glow — only when active (drawn first, sits at the
        // bottom of the z-stack; doesn't intercept clicks because the
        // clickable() lives on the outer Box, not on this child).
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tokens.telegramBlue.copy(alpha = 0.35f),
                                tokens.telegramBlue.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        // Pulse rings during connecting
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = ringScale1
                        scaleY = ringScale1
                        alpha = ringAlpha1
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tokens.connecting.copy(alpha = 0.40f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        // Drop shadow — decorative only, doesn't intercept clicks.
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(
                    elevation = if (isActive) 32.dp else 16.dp,
                    shape = CircleShape,
                    ambientColor = if (isActive) tokens.telegramBlue.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.15f),
                    spotColor = if (isActive) tokens.telegramBlue.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.20f),
                ),
        )

        // Glass body — translucent with a soft tint when active. The border
        // is drawn directly here (BorderStroke on Surface was intercepting
        // taps — see commit message for details).
        val bodyColor by animateColorAsState(
            targetValue = if (isActive) tokens.telegramBlue.copy(alpha = 0.88f)
                          else scheme.surface.copy(alpha = 0.70f),
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "body",
        )
        val rimColor by animateColorAsState(
            targetValue = if (isActive) Color.White.copy(alpha = 0.65f)
                          else tokens.borderLight.copy(alpha = tokens.borderAlpha),
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "rim",
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .border(1.dp, rimColor, CircleShape)
                .background(bodyColor)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                        center = androidx.compose.ui.geometry.Offset(0.4f, 0.3f),
                    ),
                )
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            contentAlignment = Alignment.Center,
        ) {
            // Telegram logo — the preserved visual center of the toggle
            Image(
                painter = painterResource(id = R.drawable.ic_telegram_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    },
                colorFilter = ColorFilter.tint(tintColor, BlendMode.SrcIn),
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// StatusLabel — large text + breathing dot
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun StatusLabel(
    statusText: String,
    isActive: Boolean,
    isProcessing: Boolean,
) {
    val tokens = AppTheme.glass
    val scheme = MaterialTheme.colorScheme

    val color = when {
        isProcessing -> tokens.connecting
        isActive -> tokens.connected
        else -> scheme.onSurfaceVariant
    }
    val colorState by animateColorAsState(targetValue = color, animationSpec = tween(400), label = "status")

    val pulse = rememberInfiniteTransition(label = "dot")
    val dotScale by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = if (isProcessing) dotScale else 1f
                    scaleY = if (isProcessing) dotScale else 1f
                }
                .clip(CircleShape)
                .background(colorState),
        )
        AnimatedContent(
            targetState = statusText,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "status",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colorState,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// LaunchPill — Telegram / Beta
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun LaunchPill(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.glass

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f),
        label = "scale",
    )

    val containerColor = if (enabled) {
        if (scheme.background.luminance() < 0.22f) {
            tokens.telegramBlue.copy(alpha = 0.22f)
        } else {
            tokens.telegramBlue.copy(alpha = 0.14f)
        }
    } else {
        scheme.surface.copy(alpha = 0.40f)
    }
    val contentColor = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.40f)
    val borderColor = if (enabled) Color.White.copy(alpha = 0.30f) else scheme.outlineVariant.copy(alpha = 0.30f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = AppShapes.Pill,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(0.5.dp, borderColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_telegram_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    if (enabled) tokens.telegramBlue else scheme.onSurface.copy(alpha = 0.4f),
                    BlendMode.SrcIn,
                ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// InfoChipsRow — CF / Pool / Port as soft pills
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun InfoChipsRow(
    cfEnabled: Boolean,
    poolSize: Int,
    port: String,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.glass

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoChip(
            label = if (cfEnabled) "CF" else "Прямое",
            value = null,
            accent = if (cfEnabled) tokens.telegramBlue else scheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f),
        )
        InfoChip(
            label = "Пул",
            value = "$poolSize",
            accent = scheme.primary,
            modifier = Modifier.weight(1.0f),
        )
        InfoChip(
            label = "Порт",
            value = port,
            accent = scheme.primary,
            modifier = Modifier.weight(1.3f),
        )
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.22f
    Surface(
        modifier = modifier.height(48.dp),
        shape = AppShapes.Pill,
        color = scheme.surface.copy(alpha = if (isDark) 0.35f else 0.55f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = if (isDark) 0.20f else 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurfaceVariant,
            )
            if (value != null) {
                Text(
                    text = "  $value",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ProxyLinkCard — tappable glass card with the proxy URL
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ProxyLinkCard(
    proxyUrl: String,
    context: Context,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.glass
    val isDark = scheme.background.luminance() < 0.22f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(150),
        label = "scale",
    )

    Surface(
        onClick = {
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cb.setPrimaryClip(android.content.ClipData.newPlainText("Proxy", proxyUrl))
            Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = AppShapes.Large,
        color = scheme.surface.copy(alpha = if (isDark) 0.45f else 0.65f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = if (isDark) 0.22f else 0.40f)),
        interactionSource = interactionSource,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = tokens.telegramBlue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = proxyUrl,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = scheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
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
// SessionCard — footer summary
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun SessionCard(isActive: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tokens = AppTheme.glass
    val isDark = scheme.background.luminance() < 0.22f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Large,
        color = scheme.surface.copy(alpha = if (isDark) 0.35f else 0.55f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = if (isDark) 0.18f else 0.30f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Stat("Соединение", if (isActive) "Активно" else "Ожидание",
                if (isActive) tokens.connected else scheme.onSurfaceVariant)
            Stat("Порт", "127.0.0.1", scheme.primary)
            Stat("Тип", "MTProto WS", tokens.telegramBlue)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, accent: Color) {
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

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
