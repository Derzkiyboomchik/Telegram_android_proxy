package com.tgws.proxy.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgws.proxy.R
import kotlin.math.roundToInt

@Composable
fun FloatingToolbar(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    isDynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    currentPalette: String,
    onPaletteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.22f

    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }

    var offsetY by rememberSaveable { mutableFloatStateOf(-1f) }
    var isRightSide by rememberSaveable { mutableStateOf(true) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var tabHeightPx by remember { mutableFloatStateOf(0f) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }

    val tabWidthDp = 44.dp
    val tabHeightDp = 56.dp
    val panelWidthDp = 224.dp

    val tabWidthPx = remember(density) { with(density) { tabWidthDp.toPx() } }
    val fallbackTabHeightPx = remember(density) { with(density) { tabHeightDp.toPx() } }
    val edgePaddingPx = remember(density) { with(density) { 8.dp.toPx() } }
    val safeDrawing = WindowInsets.safeDrawing
    val safeTopPx = with(density) { safeDrawing.getTop(density).toFloat() }
    val safeBottomPx = with(density) { safeDrawing.getBottom(density).toFloat() }
    val effectiveTabHeightPx = maxOf(tabHeightPx, fallbackTabHeightPx)
    val floatingHeightPx = if (isExpanded && panelHeightPx > 0f) {
        maxOf(effectiveTabHeightPx, panelHeightPx)
    } else {
        effectiveTabHeightPx
    }
    val minOffsetY = safeTopPx + edgePaddingPx
    val maxOffsetY = (screenHeightPx - safeBottomPx - floatingHeightPx - edgePaddingPx)
        .coerceAtLeast(minOffsetY)
    val defaultOffsetY = (screenHeightPx * 0.24f).coerceIn(minOffsetY, maxOffsetY)

    val targetXPx = if (isRightSide) screenWidthPx - tabWidthPx else 0f
    val animatedTabXPx by animateFloatAsState(
        targetValue = targetXPx,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tab_shift",
    )

    LaunchedEffect(minOffsetY, maxOffsetY) {
        offsetY = if (offsetY < 0f) defaultOffsetY else offsetY.coerceIn(minOffsetY, maxOffsetY)
    }

    val tabInteraction = remember { MutableInteractionSource() }
    val isTabPressed by tabInteraction.collectIsPressedAsState()
    val tabScale by animateFloatAsState(
        targetValue = if (isTabPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "tab_scale",
    )

    val tabBodyColor = scheme.surface.copy(alpha = if (isDark) 0.55f else 0.75f)
    val tabBorderColor = Color.White.copy(alpha = if (isDark) 0.30f else 0.50f)

    Box(modifier = modifier.fillMaxSize()) {
        // ── Tab handle ──
        Surface(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .offset { IntOffset(animatedTabXPx.roundToInt(), offsetY.roundToInt()) }
                .onGloballyPositioned { coordinates ->
                    tabHeightPx = coordinates.size.height.toFloat()
                }
                .graphicsLayer {
                    scaleX = tabScale
                    scaleY = tabScale
                }
                .pointerInput(minOffsetY, maxOffsetY) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY = (offsetY + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                        },
                    )
                },
            shape = if (isRightSide)
                RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
            else
                RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
            color = tabBodyColor,
            border = BorderStroke(0.5.dp, tabBorderColor),
            shadowElevation = AppElevation.Level3,
            tonalElevation = AppElevation.Level0,
            interactionSource = tabInteraction,
        ) {
            Box(
                modifier = Modifier.size(tabWidthDp, tabHeightDp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_palette),
                    contentDescription = "Тема",
                    modifier = Modifier.size(22.dp),
                    tint = scheme.onSurface,
                )
            }
        }

        // ── Expanded panel ──
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.offset {
                val panelWidthPx = with(density) { panelWidthDp.toPx() }
                val gap = with(density) { 8.dp.toPx() }
                val panelX = if (isRightSide) {
                    (targetXPx - panelWidthPx - gap).roundToInt()
                } else {
                    (tabWidthPx + gap).roundToInt()
                }
                IntOffset(panelX, offsetY.roundToInt())
            },
        ) {
            Surface(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    panelHeightPx = coordinates.size.height.toFloat()
                },
                shape = AppShapes.XLarge,
                color = scheme.surface.copy(alpha = if (isDark) 0.65f else 0.85f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = if (isDark) 0.22f else 0.45f)),
                shadowElevation = AppElevation.Level3,
                tonalElevation = AppElevation.Level0,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp).width(panelWidthDp - 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Тема",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )

                    ThemeOption(
                        icon = R.drawable.ic_auto,
                        label = "Системная",
                        selected = currentTheme == "system",
                        onClick = { onThemeChange("system"); isExpanded = false },
                    )
                    ThemeOption(
                        icon = R.drawable.ic_light_mode,
                        label = "Светлая",
                        selected = currentTheme == "light",
                        onClick = { onThemeChange("light"); isExpanded = false },
                    )
                    ThemeOption(
                        icon = R.drawable.ic_dark_mode,
                        label = "Тёмная",
                        selected = currentTheme == "dark",
                        onClick = { onThemeChange("dark"); isExpanded = false },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = scheme.outlineVariant.copy(alpha = 0.50f),
                    )

                    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val showDynamicColorOn = isDynamicColor && supportsDynamicColor
                    val showPalettes = !showDynamicColorOn

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Динамические цвета",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (supportsDynamicColor) scheme.onSurfaceVariant
                                    else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Switch(
                            checked = showDynamicColorOn,
                            onCheckedChange = { onDynamicColorChange(it) },
                            enabled = supportsDynamicColor,
                            modifier = Modifier.scale(0.8f),
                        )
                    }

                    AnimatedVisibility(visible = showPalettes) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = scheme.outlineVariant.copy(alpha = 0.50f),
                            )
                            Text(
                                "Палитра",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                PaletteCircle("aurora", 0xFF0EA5B7, currentPalette, onPaletteChange)
                                PaletteCircle("sunset", 0xFFB5413B, currentPalette, onPaletteChange)
                                PaletteCircle("graphite", 0xFF5C5F62, currentPalette, onPaletteChange)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "theme_opt_scale",
    )
    val isDark = scheme.background.luminance() < 0.22f

    Surface(
        onClick = onClick,
        shape = AppShapes.Large,
        color = if (selected) scheme.primary.copy(alpha = if (isDark) 0.30f else 0.18f)
                else Color.Transparent,
        border = if (selected) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.45f))
                 else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
        interactionSource = interaction,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) scheme.primary else scheme.onSurface,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun PaletteCircle(
    paletteId: String,
    colorHex: Long,
    selectedId: String,
    onClick: (String) -> Unit,
) {
    val isSelected = paletteId == selectedId
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f),
        label = "palette_scale",
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color(colorHex))
            .clickable(interactionSource = interaction, indication = null) { onClick(paletteId) }
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = Color.White,
                    shape = CircleShape,
                ) else Modifier,
            ),
    )
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
