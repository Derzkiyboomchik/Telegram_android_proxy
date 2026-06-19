package com.tgws.proxy.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
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
    val tokens = AppTheme.colors

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
        offsetY = if (offsetY < 0f) {
            defaultOffsetY
        } else {
            offsetY.coerceIn(minOffsetY, maxOffsetY)
        }
    }

    // Press feedback for the tab handle
    val tabInteraction = remember { MutableInteractionSource() }
    val isTabPressed by tabInteraction.collectIsPressedAsState()
    val tabScale by animateFloatAsState(
        targetValue = if (isTabPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "tab_scale",
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ── Tab handle ───────────────────────────────────────────────────────
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
                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            else
                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        tokens.telegramBlue.copy(alpha = 0.55f),
                        tokens.neonViolet.copy(alpha = 0.25f),
                    ),
                ),
            ),
            shadowElevation = AppElevation.Level3,
            tonalElevation = AppElevation.Level2,
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
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // ── Expanded panel ───────────────────────────────────────────────────
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(tokens.cardBorderGradient),
                ),
                shadowElevation = AppElevation.Level3,
                tonalElevation = AppElevation.Level2,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp).width(panelWidthDp - 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Тема",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
                            color = if (supportsDynamicColor) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                            Text(
                                "Палитра",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                PaletteCircle("cyber", 0xFF2AABEE, currentPalette, onPaletteChange)
                                PaletteCircle("indigo", 0xFF5B588D, currentPalette, onPaletteChange)
                                PaletteCircle("forest", 0xFF5F5D68, currentPalette, onPaletteChange)
                                PaletteCircle("espresso", 0xFF6D4C41, currentPalette, onPaletteChange)
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
    val tokens = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "theme_opt_scale",
    )

    Surface(
        onClick = onClick,
        shape = AppShapes.Large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(tokens.telegramBlue.copy(alpha = 0.55f), tokens.neonViolet.copy(alpha = 0.25f)),
            ),
        ) else null,
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
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
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
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            AppTheme.colors.telegramBlue,
                            AppTheme.colors.neonCyan,
                            AppTheme.colors.neonViolet,
                            AppTheme.colors.telegramBlue,
                        ),
                    ),
                    shape = CircleShape,
                ) else Modifier,
            ),
    )
}
