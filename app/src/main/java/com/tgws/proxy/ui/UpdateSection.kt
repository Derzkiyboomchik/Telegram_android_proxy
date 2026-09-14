package com.tgws.proxy.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgws.proxy.BuildConfig
import com.tgws.proxy.UpdateChecker
import kotlinx.coroutines.launch
import java.io.File

/**
 * UpdateSection — Compose UI block for the in-app updater and version switcher.
 */
@Composable
fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
    var noUpdate by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var downloading by remember { mutableStateOf(false) }
    var downloadingVersionName by remember { mutableStateOf("") }

    // Version switcher state
    var loadingAllReleases by remember { mutableStateOf(false) }
    var allReleases by remember { mutableStateOf<List<UpdateChecker.ReleaseInfo>>(emptyList()) }
    var showAllReleases by remember { mutableStateOf(false) }

    // Listen for ACTION_DOWNLOAD_COMPLETE — when APK finishes downloading, trigger installer.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != downloadId) return
                downloading = false
                val ctx2 = ctx ?: return
                val dm = ctx2.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor: Cursor = dm.query(DownloadManager.Query().setFilterById(id))
                cursor.use {
                    if (it.moveToFirst()) {
                        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriString: String? = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            val file = uriString?.let { s -> File(android.net.Uri.parse(s).path ?: "") }
                            if (file?.exists() == true) {
                                UpdateChecker.installApk(ctx2, file)
                            } else {
                                Toast.makeText(ctx2, "Не удалось найти загруженный файл", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(ctx2, "Загрузка не удалась", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── 1. Проверка обновлений ──
        SectionHeader(
            icon = Icons.Default.SystemUpdate,
            title = "Обновления",
            subtitle = "Текущая версия ${BuildConfig.VERSION_NAME}",
        )

        if (checking) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Проверка…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (downloading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Загрузка версии $downloadingVersionName…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (noUpdate && release == null) {
            Text(
                "У вас установлена последняя версия",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }

        release?.let { r ->
            UpdateAvailableCard(
                release = r,
                downloading = downloading,
                onDownload = {
                    downloadingVersionName = r.versionName
                    downloadId = UpdateChecker.downloadApk(context, r)
                    downloading = true
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    checking = true
                    noUpdate = false
                    release = null
                    scope.launch {
                        val r = UpdateChecker.fetchLatestRelease(context)
                        checking = false
                        if (r == null) {
                            Toast.makeText(context, "Не удалось проверить обновления", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (UpdateChecker.isNewer(r.versionName, BuildConfig.VERSION_NAME)) {
                            release = r
                        } else {
                            noUpdate = true
                        }
                    }
                },
                enabled = !checking && !downloading,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = AppShapes.Large,
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Проверить", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(4.dp))

        // ── 2. Виджет: Смена версии (Архив релизов) ──
        SectionHeader(
            icon = Icons.Default.History,
            title = "Смена версии",
            subtitle = "Установка других версий и откат",
        )

        OutlinedButton(
            onClick = {
                if (!showAllReleases && allReleases.isEmpty()) {
                    loadingAllReleases = true
                    scope.launch {
                        allReleases = UpdateChecker.fetchAllReleases(context)
                        loadingAllReleases = false
                        showAllReleases = true
                        if (allReleases.isEmpty()) {
                            Toast.makeText(context, "Не удалось загрузить список версий", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    showAllReleases = !showAllReleases
                }
            },
            enabled = !loadingAllReleases && !downloading,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = AppShapes.Large,
        ) {
            if (loadingAllReleases) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Загрузка списка версий…", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.History, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (showAllReleases) "Скрыть список версий" else "Показать все доступные версии",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        AnimatedVisibility(visible = showAllReleases && allReleases.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Info notice about Android OS downgrade protection
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "Откат на старую версию",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Android блокирует установку старой версии поверх новой (ошибка «Пакет недействителен»). Для установки более старой версии сначала удалите текущее приложение TG WS Proxy с телефона, а затем установите скачанный APK.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                allReleases.forEach { rel ->
                    ReleaseItemCard(
                        release = rel,
                        currentVersion = BuildConfig.VERSION_NAME,
                        isDownloading = downloading && downloadingVersionName == rel.versionName,
                        onDownload = {
                            downloadingVersionName = rel.versionName
                            downloadId = UpdateChecker.downloadApk(context, rel)
                            downloading = true
                            Toast.makeText(context, "Начата загрузка версии ${rel.versionName}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Text(
            "Репозиторий: github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ReleaseItemCard(
    release: UpdateChecker.ReleaseInfo,
    currentVersion: String,
    isDownloading: Boolean,
    onDownload: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isCurrent = release.versionName == currentVersion
    val isOlder = UpdateChecker.isOlder(release.versionName, currentVersion)
    val isNewer = UpdateChecker.isNewer(release.versionName, currentVersion)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Medium,
        color = when {
            isCurrent -> scheme.primaryContainer.copy(alpha = 0.35f)
            else -> scheme.surfaceContainerHigh
        },
        border = BorderStroke(
            0.5.dp,
            when {
                isCurrent -> scheme.primary.copy(alpha = 0.5f)
                else -> scheme.outline.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = release.tagName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )

                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.18f),
                            border = BorderStroke(0.5.dp, Color(0xFF2E7D32))
                        ) {
                            Text(
                                text = "Текущая",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isNewer) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0288D1).copy(alpha = 0.18f),
                            border = BorderStroke(0.5.dp, Color(0xFF0288D1))
                        ) {
                            Text(
                                text = "Новее",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0288D1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isOlder) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE65100).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFFE65100))
                        ) {
                            Text(
                                text = "Откат",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (release.publishedAt.isNotBlank()) {
                    Text(
                        text = release.publishedAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            if (release.apkSize > 0) {
                val sizeMb = "%.1f MB".format(release.apkSize / (1024.0 * 1024.0))
                Text(
                    text = "Размер APK: $sizeMb",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            if (release.releaseNotes.isNotBlank()) {
                Text(
                    text = release.releaseNotes.take(200),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 3,
                )
            }

            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = AppShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrent) scheme.surfaceVariant else scheme.primary,
                    contentColor = if (isCurrent) scheme.onSurfaceVariant else scheme.onPrimary
                )
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = scheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузка…", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCurrent) "Переустановить ${release.versionName}" else "Скачать ${release.versionName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(
    release: UpdateChecker.ReleaseInfo,
    downloading: Boolean,
    onDownload: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.22f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Medium,
        color = scheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f),
        border = BorderStroke(
            0.5.dp,
            scheme.primary.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Доступно обновление ${release.versionName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (release.releaseNotes.isNotBlank()) {
                Text(
                    release.releaseNotes.take(400),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 6,
                )
            }
            Button(
                onClick = onDownload,
                enabled = !downloading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = AppShapes.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Скачать и установить", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF2AABEE),
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

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
