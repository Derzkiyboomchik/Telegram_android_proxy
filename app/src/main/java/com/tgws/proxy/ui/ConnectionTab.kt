package com.tgws.proxy.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgws.proxy.ProxyController
import com.tgws.proxy.ProxyService
import com.tgws.proxy.SettingsStore
import com.tgws.proxy.R
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
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
        return
    }

    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ProxyService.isRunning.collect { running ->
            if (running) {
                delay(600)
            }
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
                        showInvalidPortToast = true
                    )
                    if (!started) {
                        isProcessing = false
                    }
                }
            }
        }
    }

    val isActiveVisual = isRunning || isProcessing
    val logoScale by animateFloatAsState(
        targetValue = if (isActiveVisual) 1.12f else 0.94f,
        animationSpec = tween(durationMillis = 650, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "logo_scale"
    )
    val tintColor by animateColorAsState(
        targetValue = if (isActiveVisual) AppColors.telegramBlue else Color(0xFF808080),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "logo_tint"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (isActiveVisual) 1f else 0.45f,
        animationSpec = tween(durationMillis = 700),
        label = "logo_alpha"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScaleX by animateFloatAsState(
        targetValue = if (isPressed) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "press_scale_x"
    )
    val pressScaleY by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "press_scale_y"
    )
    // Zero-gravity droplet floating animation
    val zeroG = rememberInfiniteTransition(label = "zero_g")
    val zeroGScaleX by zeroG.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "zg_x"
    )
    val zeroGScaleY by zeroG.animateFloat(
        initialValue = 1.03f, targetValue = 0.97f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "zg_y"
    )
    val zeroGRotZ by zeroG.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "zg_rz"
    )
    val zeroGRotX by zeroG.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "zg_rx"
    )
    val zeroGRotY by zeroG.animateFloat(
        initialValue = 6f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse),
        label = "zg_ry"
    )
    val statusColor by animateColorAsState(
        targetValue = if (isActiveVisual) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "connection_status_color"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 0.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Запуск",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ChatBackground(
                isActive = isActiveVisual,
                modifier = Modifier.fillMaxSize()
            )
            AppSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActiveVisual) {
                            val glowAlpha = 0.35f + (zeroGScaleX - 0.97f) * 8f // 0.27..0.43
                            Box(
                                modifier = Modifier
                                    .size(280.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AppColors.telegramBlue.copy(alpha = glowAlpha),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AppColors.telegramBlue.copy(alpha = glowAlpha * 0.6f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                        val surfaceColor = MaterialTheme.colorScheme.surface
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape)
                                .background(surfaceColor, CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = !isProcessing,
                                    onClick = onToggle
                                )
                                .graphicsLayer {
                                    scaleX = logoScale * pressScaleX * (if (isActiveVisual) zeroGScaleX else 1f)
                                    scaleY = logoScale * pressScaleY * (if (isActiveVisual) zeroGScaleY else 1f)
                                    cameraDistance = 18f * density
                                    rotationZ = if (isActiveVisual) zeroGRotZ else 0f
                                    rotationX = if (isPressed) 14f else if (isActiveVisual) zeroGRotX else 0f
                                    rotationY = if (isPressed) -10f else if (isActiveVisual) zeroGRotY else 0f
                                    shape = CircleShape
                                    clip = true
                                    if (isActiveVisual) {
                                        shadowElevation = 28f
                                        spotShadowColor = AppColors.telegramBlue.copy(alpha = 0.55f)
                                        ambientShadowColor = Color.Transparent
                                    }
                                    alpha = logoAlpha
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_telegram_logo),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                colorFilter = ColorFilter.tint(tintColor, BlendMode.SrcIn)
                            )
                        }
                    }
                    val statusContainerColor = if (isActiveVisual) {
                        AppColors.telegramBlue.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = statusContainerColor,
                        border = BorderStroke(
                            1.dp,
                            if (isActiveVisual) AppColors.telegramBlue.copy(alpha = 0.30f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.fillMaxWidth(0.60f).height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Telegram buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TelegramButton(
                                label = "Telegram",
                                enabled = isRunning,
                                modifier = Modifier.weight(1f),
                                onClick = { openTelegram(context, proxyUrl, "org.telegram.messenger") }
                            )
                            TelegramButton(
                                label = "Beta",
                                enabled = isRunning,
                                modifier = Modifier.weight(1f),
                                onClick = { openTelegram(context, proxyUrl, "org.telegram.messenger.beta") }
                            )
                        }

                        ProxyStatusPanel(
                            cfEnabled = savedCfEnabled,
                            poolSize = savedPoolSize,
                            port = savedPort
                        )

                        Surface(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cb.setPrimaryClip(android.content.ClipData.newPlainText("Proxy", proxyUrl))
                                Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = proxyUrl,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDark = remember(colors.background) { colors.background.luminance() < 0.22f }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = remember(enabled, isDark, colors.primaryContainer, colors.primary) {
                if (enabled && isDark) lerp(colors.primaryContainer, colors.primary, 0.15f)
                else colors.primaryContainer
            },
            contentColor = colors.onPrimaryContainer,
            disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.25f),
            disabledContentColor = colors.onSurface.copy(alpha = 0.25f)
        ),
        border = if (enabled && isDark) BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)) else null
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun openTelegram(context: Context, proxyUrl: String, packageName: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(proxyUrl))
    intent.setPackage(packageName)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Приложение не установлено", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ProxyStatusPanel(
    cfEnabled: Boolean,
    poolSize: Int,
    port: String,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = remember(colors.background) { colors.background.luminance() < 0.22f }
    val panelBrush = remember(isDark, colors.surface, colors.primary, colors.surfaceVariant) {
        if (isDark) {
            Brush.horizontalGradient(
                colors = listOf(
                    colors.surface,
                    lerp(colors.surface, colors.primary, 0.06f),
                    colors.surface
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    colors.surface,
                    lerp(colors.surface, colors.surfaceVariant, 0.35f),
                    colors.surface
                )
            )
        }
    }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isDark) lerp(colors.outlineVariant, colors.primary, 0.30f).copy(alpha = 0.50f)
            else colors.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(panelBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProxyStatusItem(
                    text = if (cfEnabled) "CF" else "Прямое",
                    modifier = Modifier
                        .weight(0.9f)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
                ProxyStatusDivider()
                ProxyStatusItem(
                    text = "Пул x$poolSize",
                    modifier = Modifier
                        .weight(1.05f)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
                ProxyStatusDivider()
                ProxyStatusItem(
                    text = "Порт $port",
                    modifier = Modifier
                        .weight(1.35f)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ProxyStatusItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProxyStatusDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}
