package com.tgws.proxy.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgws.proxy.ProxyService
import com.tgws.proxy.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun generateRandomSecret(): String {
    val bytes = ByteArray(16)
    java.security.SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

// ════════════════════════════════════════════════════════════════════════════
// Section header — Telegram settings style: solid accent bubble (rounded
// square, white icon) + title/subtitle inside the card
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: Color = Color(0xFF2AABEE),
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Accent bubble — solid color rounded square with a white icon
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent,
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Switch row
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    accent: Color? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val iconTint = accent ?: scheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) iconTint else scheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.45f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SoftDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

// Telegram-style section accents
private val AccentBlue = Color(0xFF2AABEE)
private val AccentOrange = Color(0xFFFF9500)
private val AccentGreen = Color(0xFF34C759)
private val AccentPurple = Color(0xFFAF52DE)
private val AccentGray = Color(0xFF8E8E93)

// ════════════════════════════════════════════════════════════════════════════
// SettingsTab — Telegram settings style. NO donate row.
// ════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRunning by ProxyService.isRunning.collectAsStateWithLifecycle()

    val isReady by settingsStore.isReady.collectAsStateWithLifecycle(initialValue = false)
    val isExperimental by settingsStore.isExperimentalMode.collectAsStateWithLifecycle(initialValue = false)

    val savedIsDcAuto by settingsStore.isDcAuto.collectAsStateWithLifecycle(initialValue = true)
    val savedDc1 by settingsStore.dc1.collectAsStateWithLifecycle(initialValue = "")
    val savedDc2 by settingsStore.dc2.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_DIRECT_DC2_IP)
    val savedDc3 by settingsStore.dc3.collectAsStateWithLifecycle(initialValue = "")
    val savedDc4 by settingsStore.dc4.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_DIRECT_DC4_IP)
    val savedDc5 by settingsStore.dc5.collectAsStateWithLifecycle(initialValue = "")
    val savedDc203 by settingsStore.dc203.collectAsStateWithLifecycle(initialValue = "")
    val savedDc1m by settingsStore.dc1m.collectAsStateWithLifecycle(initialValue = "")
    val savedDc2m by settingsStore.dc2m.collectAsStateWithLifecycle(initialValue = "")
    val savedDc3m by settingsStore.dc3m.collectAsStateWithLifecycle(initialValue = "")
    val savedDc4m by settingsStore.dc4m.collectAsStateWithLifecycle(initialValue = "")
    val savedDc5m by settingsStore.dc5m.collectAsStateWithLifecycle(initialValue = "")
    val savedDc203m by settingsStore.dc203m.collectAsStateWithLifecycle(initialValue = "")
    val savedPort by settingsStore.port.collectAsStateWithLifecycle(initialValue = "1443")
    val savedPoolSize by settingsStore.poolSize.collectAsStateWithLifecycle(initialValue = 4)
    val savedCfEnabled by settingsStore.cfproxyEnabled.collectAsStateWithLifecycle(initialValue = true)
    val savedCustomDomainEnabled by settingsStore.customCfDomainEnabled.collectAsStateWithLifecycle(initialValue = false)
    val savedCustomDomain by settingsStore.customCfDomain.collectAsStateWithLifecycle(initialValue = "")
    val autoStartOnBoot by settingsStore.autoStartOnBoot.collectAsStateWithLifecycle(initialValue = false)
    val savedSecretKey by settingsStore.secretKey.collectAsStateWithLifecycle(initialValue = "LOADING")

    // Appearance (migrated from the old floating theme toolbar)
    val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = "system")
    val isDynamicColor by settingsStore.isDynamicColor.collectAsStateWithLifecycle(initialValue = true)
    val themePalette by settingsStore.themePalette.collectAsStateWithLifecycle(initialValue = "aurora")

    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
        }
        return
    }

    var isDcAuto by rememberSaveable(savedIsDcAuto) { mutableStateOf(savedIsDcAuto) }
    var experimentalMode by rememberSaveable(isExperimental) { mutableStateOf(isExperimental) }
    var dc1Text by rememberSaveable(savedDc1) { mutableStateOf(savedDc1) }
    var dc2Text by rememberSaveable(savedDc2) { mutableStateOf(savedDc2) }
    var dc3Text by rememberSaveable(savedDc3) { mutableStateOf(savedDc3) }
    var dc4Text by rememberSaveable(savedDc4) { mutableStateOf(savedDc4) }
    var dc5Text by rememberSaveable(savedDc5) { mutableStateOf(savedDc5) }
    var dc203Text by rememberSaveable(savedDc203) { mutableStateOf(savedDc203) }
    var dc1mText by rememberSaveable(savedDc1m) { mutableStateOf(savedDc1m) }
    var dc2mText by rememberSaveable(savedDc2m) { mutableStateOf(savedDc2m) }
    var dc3mText by rememberSaveable(savedDc3m) { mutableStateOf(savedDc3m) }
    var dc4mText by rememberSaveable(savedDc4m) { mutableStateOf(savedDc4m) }
    var dc5mText by rememberSaveable(savedDc5m) { mutableStateOf(savedDc5m) }
    var dc203mText by rememberSaveable(savedDc203m) { mutableStateOf(savedDc203m) }
    var portText by rememberSaveable(savedPort) { mutableStateOf(savedPort) }
    var selectedPoolSize by rememberSaveable(savedPoolSize) { mutableIntStateOf(savedPoolSize) }
    var cfEnabled by rememberSaveable(savedCfEnabled) { mutableStateOf(savedCfEnabled) }
    var customCfDomainEnabled by rememberSaveable(savedCustomDomainEnabled) { mutableStateOf(savedCustomDomainEnabled) }
    var customCfDomain by rememberSaveable(savedCustomDomain) { mutableStateOf(savedCustomDomain) }
    var secretKeyText by remember(savedSecretKey) { mutableStateOf(if (savedSecretKey == "LOADING") "" else savedSecretKey) }

    LaunchedEffect(savedSecretKey) {
        if (savedSecretKey == "") {
            val generated = generateRandomSecret()
            secretKeyText = generated
            settingsStore.saveSecretKey(generated)
        } else if (savedSecretKey != "LOADING") {
            secretKeyText = savedSecretKey
        }
    }

    var saveJob by remember { mutableStateOf<Job?>(null) }
    fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            settingsStore.saveAll(
                isDcAuto, dc1Text, dc2Text, dc3Text, dc4Text, dc5Text, dc203Text,
                dc1mText, dc2mText, dc3mText, dc4mText, dc5mText, dc203mText,
                experimentalMode, portText, selectedPoolSize,
                cfEnabled, customCfDomainEnabled, customCfDomain, secretKeyText
            )
        }
    }

    var showIpSetupDialog by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showIpSetupDialog) {
        IpSetupDialog(
            isExperimental = experimentalMode,
            onExperimentalChange = { experimentalMode = it; scheduleSave() },
            dc1Text = dc1Text, onDc1Change = { dc1Text = it; scheduleSave() },
            dc2Text = dc2Text, onDc2Change = { dc2Text = it; scheduleSave() },
            dc3Text = dc3Text, onDc3Change = { dc3Text = it; scheduleSave() },
            dc4Text = dc4Text, onDc4Change = { dc4Text = it; scheduleSave() },
            dc5Text = dc5Text, onDc5Change = { dc5Text = it; scheduleSave() },
            dc203Text = dc203Text, onDc203Change = { dc203Text = it; scheduleSave() },
            dc1mText = dc1mText, onDc1mChange = { dc1mText = it; scheduleSave() },
            dc2mText = dc2mText, onDc2mChange = { dc2mText = it; scheduleSave() },
            dc3mText = dc3mText, onDc3mChange = { dc3mText = it; scheduleSave() },
            dc4mText = dc4mText, onDc4mChange = { dc4mText = it; scheduleSave() },
            dc5mText = dc5mText, onDc5mChange = { dc5mText = it; scheduleSave() },
            dc203mText = dc203mText, onDc203mChange = { dc203mText = it; scheduleSave() },
            onDismiss = { showIpSetupDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Group: Подключение ──
        AppSectionCard {
            SectionHeader(
                icon = Icons.Default.Public,
                title = "Подключение",
                subtitle = "Порт, DC-адреса, автозапуск",
                accent = AccentBlue,
            )

            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it; scheduleSave() },
                label = { Text("Порт локального прокси") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = AppShapes.Large,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
            )

            OutlinedButton(
                onClick = { showIpSetupDialog = true },
                enabled = !cfEnabled && !isRunning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = AppShapes.Large,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                ),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = if (cfEnabled || isRunning) 0.2f else 0.5f),
                ),
            ) {
                Icon(Icons.Default.Lan, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (cfEnabled) "Адреса DC — авто (CF включён)" else "Настроить адреса DC",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            SoftDivider()

            SwitchSettingRow(
                icon = Icons.Default.PowerSettingsNew,
                title = "Автозапуск при включении",
                subtitle = "Запускать прокси после загрузки устройства",
                checked = autoStartOnBoot,
                accent = AccentBlue,
                onCheckedChange = { enabled ->
                    scope.launch { settingsStore.saveAutoStartOnBoot(enabled) }
                },
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Group: Обход блокировок ──
        AppSectionCard {
            SectionHeader(
                icon = Icons.Default.Shield,
                title = "Обход блокировок",
                subtitle = "CloudFlare CDN, WebSocket туннелирование",
                accent = AccentOrange,
            )

            SwitchSettingRow(
                icon = Icons.Default.Cloud,
                title = "CloudFlare CDN",
                subtitle = "Проксировать через домены CF",
                checked = cfEnabled,
                enabled = !isRunning,
                accent = AccentOrange,
                onCheckedChange = {
                    cfEnabled = it
                    isDcAuto = it
                    scheduleSave()
                },
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Group: Производительность ──
        AppSectionCard {
            SectionHeader(
                icon = Icons.Default.Workspaces,
                title = "Производительность",
                subtitle = "Размер пула WebSocket-соединений",
                accent = AccentGreen,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val poolOptions = listOf(2, 4, 6)
                poolOptions.forEach { size ->
                    PoolChip(
                        label = "$size",
                        selected = selectedPoolSize == size,
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        selectedPoolSize = size
                        scheduleSave()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Group: Секретный ключ ──
        AppSectionCard {
            SectionHeader(
                icon = Icons.Default.VpnKey,
                title = "Секретный ключ",
                subtitle = "Уникальный идентификатор прокси",
                accent = AccentPurple,
            )
            OutlinedTextField(
                value = secretKeyText,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = AppShapes.Large,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val newKey = generateRandomSecret()
                            secretKeyText = newKey
                            scope.launch { settingsStore.saveSecretKey(newKey) }
                            scheduleSave()
                        },
                        enabled = !isRunning,
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Group: Оформление ── (migrated from the old floating toolbar)
        AppSectionCard {
            SectionHeader(
                icon = Icons.Default.Palette,
                title = "Оформление",
                subtitle = "Тема и цветовая палитра",
                accent = AccentBlue,
            )

            ThemeModeRow(
                icon = Icons.Default.BrightnessAuto,
                title = "Системная",
                selected = themeMode == "system",
                onClick = { scope.launch { settingsStore.saveThemeMode("system") } },
            )
            ThemeModeRow(
                icon = Icons.Default.Brightness5,
                title = "Светлая",
                selected = themeMode == "light",
                onClick = { scope.launch { settingsStore.saveThemeMode("light") } },
            )
            ThemeModeRow(
                icon = Icons.Default.Brightness2,
                title = "Тёмная",
                selected = themeMode == "dark",
                onClick = { scope.launch { settingsStore.saveThemeMode("dark") } },
            )

            SoftDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Палитра",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                PaletteDot("aurora", Color(0xFF0EA5B7), themePalette) {
                    scope.launch { settingsStore.saveThemePalette("aurora") }
                }
                PaletteDot("sunset", Color(0xFFB5413B), themePalette) {
                    scope.launch { settingsStore.saveThemePalette("sunset") }
                }
                PaletteDot("graphite", Color(0xFF5C5F62), themePalette) {
                    scope.launch { settingsStore.saveThemePalette("graphite") }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Group: Обновления ── (in-app GitHub release updater)
        AppSectionCard {
            UpdateSection()
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Group: О приложении ── (only GitHub, no donate)
        AppSectionCard {
            SectionHeader(
                icon = Icons.Default.Code,
                title = "О приложении",
                subtitle = "Исходный код и версия",
                accent = AccentGray,
            )
            AboutRow(
                icon = Icons.Default.Code,
                title = "GitHub",
                subtitle = "Исходный код проекта",
                accent = MaterialTheme.colorScheme.primary,
            ) {
                openUrlInBrowser(context, "https://github.com/Derzkiyboomchik/Telegram_android_proxy")
            }
            SoftDivider()
            AboutRow(
                icon = Icons.Default.Workspaces,
                title = "Версия",
                subtitle = "${com.tgws.proxy.BuildConfig.VERSION_NAME} · MTProto over WebSocket",
                accent = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ThemeModeRow — Telegram-style radio row (title + trailing check)
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun ThemeModeRow(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) AccentBlue else scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PaletteDot — small palette color circle for the "Оформление" section
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun PaletteDot(
    paletteId: String,
    color: Color,
    selectedId: String,
    onClick: () -> Unit,
) {
    val isSelected = paletteId == selectedId ||
        // legacy ids collapse onto aurora (see getAppColorScheme remapping)
        (paletteId == "aurora" && selectedId in listOf("cyber", "indigo", "forest", "espresso"))
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.dp, AccentBlue, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick),
    )
}

// ════════════════════════════════════════════════════════════════════════════
// AboutRow — used for the "О приложении" section
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isClickable = onClick != null
    val bg by animateColorAsState(
        targetValue = if (isPressed && isClickable) accent.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(150),
        label = "about_bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(
                if (isClickable) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick!!,
                ) else Modifier,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = AppShapes.Small,
            color = accent.copy(alpha = 0.14f),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PoolChip
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun PoolChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.22f
    val container by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.surfaceVariant.copy(alpha = 0.3f)
            selected -> scheme.primary
            else -> if (isDark) scheme.surfaceContainerHigh else Color.White
        },
        animationSpec = tween(200),
        label = "pool_container",
    )
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.onSurface.copy(alpha = 0.35f)
            selected -> scheme.onPrimary
            else -> scheme.onSurface
        },
        animationSpec = tween(200),
        label = "pool_content",
    )
    val border by animateColorAsState(
        targetValue = if (selected) Color.Transparent
                      else scheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "pool_border",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.Large,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        border = BorderStroke(0.5.dp, border),
    ) {
        Text(
            label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// IpSetupDialog — restyled
// ════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IpSetupDialog(
    isExperimental: Boolean,
    onExperimentalChange: (Boolean) -> Unit,
    dc1Text: String, onDc1Change: (String) -> Unit,
    dc2Text: String, onDc2Change: (String) -> Unit,
    dc3Text: String, onDc3Change: (String) -> Unit,
    dc4Text: String, onDc4Change: (String) -> Unit,
    dc5Text: String, onDc5Change: (String) -> Unit,
    dc203Text: String, onDc203Change: (String) -> Unit,
    dc1mText: String, onDc1mChange: (String) -> Unit,
    dc2mText: String, onDc2mChange: (String) -> Unit,
    dc3mText: String, onDc3mChange: (String) -> Unit,
    dc4mText: String, onDc4mChange: (String) -> Unit,
    dc5mText: String, onDc5mChange: (String) -> Unit,
    dc203mText: String, onDc203mChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val onIpChange = { newValue: String, update: (String) -> Unit ->
        if (newValue.all { it.isDigit() || it == '.' }) {
            update(newValue)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = AppShapes.XLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AppElevation.Level3,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .heightIn(max = 560.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Адреса датацентров",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                @Composable
                fun dcInput(label: String, value: String, update: (String) -> Unit) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { onIpChange(it, update) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = AppShapes.Large,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            ),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isExperimental) {
                        dcInput("DC1", dc1Text, onDc1Change)
                        dcInput("DC2", dc2Text, onDc2Change)
                        dcInput("DC3", dc3Text, onDc3Change)
                        dcInput("DC4", dc4Text, onDc4Change)
                        dcInput("DC5", dc5Text, onDc5Change)
                        dcInput("DC203", dc203Text, onDc203Change)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Медиа датацентры", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        dcInput("DC1m", dc1mText, onDc1mChange)
                        dcInput("DC2m", dc2mText, onDc2mChange)
                        dcInput("DC3m", dc3mText, onDc3mChange)
                        dcInput("DC4m", dc4mText, onDc4mChange)
                        dcInput("DC5m", dc5mText, onDc5mChange)
                        dcInput("DC203m", dc203mText, onDc203mChange)
                    } else {
                        dcInput("DC2", dc2Text, onDc2Change)
                        dcInput("DC4", dc4Text, onDc4Change)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Экспериментальный режим",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(checked = isExperimental, onCheckedChange = onExperimentalChange)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = AppShapes.Large,
                ) {
                    Text("Готово", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
