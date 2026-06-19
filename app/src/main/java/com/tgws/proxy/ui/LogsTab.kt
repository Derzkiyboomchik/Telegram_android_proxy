package com.tgws.proxy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tgws.proxy.LogEntry
import com.tgws.proxy.LogManager
import com.tgws.proxy.SettingsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTab(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLogs by LogManager.logs.collectAsStateWithLifecycle()

    val savedInfo by settingsStore.logShowInfo.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_LOG_SHOW_INFO)
    val savedError by settingsStore.logShowError.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_LOG_SHOW_ERROR)
    val savedNull by settingsStore.logShowNull.collectAsStateWithLifecycle(initialValue = false)

    val filteredLogs = remember(currentLogs, savedInfo, savedError, savedNull) {
        if (savedNull) {
            listOf(
                LogEntry(
                    key = "null_msg",
                    message = "NULL — логи отключены",
                    count = 1,
                    isError = false,
                    priority = 4,
                    isEssential = true,
                ),
            )
        } else {
            currentLogs.filter { entry ->
                entry.isEssential ||
                    (savedInfo && entry.priority == 4) ||
                    (savedError && entry.priority >= 5)
            }
        }
    }

    val listState = rememberLazyListState()
    var hasInitialScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            if (!hasInitialScrolled) {
                listState.scrollToItem(filteredLogs.size - 1)
                hasInitialScrolled = true
            } else {
                listState.animateScrollToItem(filteredLogs.size - 1)
            }
        }
    }

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.22f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 16.dp),
    ) {
        // Sticky header — glass panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.Large,
            color = scheme.surface.copy(alpha = if (isDark) 0.45f else 0.65f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = if (isDark) 0.20f else 0.40f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Лог событий",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scheme.primary.copy(alpha = 0.18f),
                    ) {
                        Text(
                            "${filteredLogs.size}",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Row {
                    IconButton(onClick = { LogManager.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить", tint = scheme.primary)
                    }
                    IconButton(onClick = {
                        val text = filteredLogs.joinToString("\n") { "${it.message} (×${it.count})" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TgWsProxy Logs", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = scheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogFilterChip(
                label = "INFO",
                selected = savedInfo && !savedNull,
                enabled = !savedNull,
                modifier = Modifier.weight(1f),
            ) {
                scope.launch { settingsStore.saveLogFilters(false, !savedInfo, savedError, false) }
            }
            LogFilterChip(
                label = "ERROR",
                selected = savedError && !savedNull,
                enabled = !savedNull,
                modifier = Modifier.weight(1f),
            ) {
                scope.launch { settingsStore.saveLogFilters(false, savedInfo, !savedError, false) }
            }
            LogFilterChip(
                label = "NULL",
                selected = savedNull,
                enabled = true,
                modifier = Modifier.weight(1f),
            ) {
                scope.launch { settingsStore.saveLogFilters(false, false, false, !savedNull) }
            }
        }

        // Logs container — translucent glass
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = AppShapes.XLarge,
            color = scheme.surface.copy(alpha = if (isDark) 0.40f else 0.60f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = if (isDark) 0.18f else 0.35f)),
        ) {
            if (filteredLogs.isEmpty()) {
                EmptyLogState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(
                        items = filteredLogs,
                        key = { it.key },
                    ) { entry ->
                        LogLine(entry)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogFilterChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        },
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.25f),
            selectedBorderColor = Color.White.copy(alpha = 0.45f),
            borderWidth = 0.5.dp,
            selectedBorderWidth = 0.5.dp,
        ),
    )
}

@Composable
private fun LogLine(entry: LogEntry) {
    val tokens = AppTheme.glass
    val scheme = MaterialTheme.colorScheme

    val color = when (entry.priority) {
        6 -> tokens.logError
        5 -> tokens.logWarn
        4 -> tokens.logInfo
        3 -> tokens.logDebug
        else -> scheme.onSurface
    }
    val icon = when (entry.priority) {
        6 -> Icons.Default.Error
        5 -> Icons.Default.Warning
        4 -> Icons.Default.Info
        3 -> Icons.Default.BugReport
        else -> Icons.Default.Info
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = tokens.logCounter.copy(alpha = 0.18f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${entry.count}",
                    color = tokens.logCounter,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.85f),
            modifier = Modifier.size(14.dp),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = entry.message,
            color = color,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (entry.isError) FontWeight.Bold else FontWeight.Normal,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
        )
    }
}

@Composable
private fun EmptyLogState() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.40f),
                modifier = Modifier.size(40.dp),
            )
            Text(
                "Логи отсутствуют",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Запустите прокси — события появятся здесь",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
