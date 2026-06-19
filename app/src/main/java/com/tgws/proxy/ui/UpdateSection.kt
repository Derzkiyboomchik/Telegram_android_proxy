package com.tgws.proxy.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.tgws.proxy.BuildConfig
import com.tgws.proxy.R
import com.tgws.proxy.UpdateChecker
import kotlinx.coroutines.launch
import java.io.File

/**
 * UpdateSection — Compose UI block for the in-app updater.
 *
 * Renders a glass card with:
 *   - current version + GitHub repo info
 *   - "Проверить обновления" button (manual check)
 *   - if an update is available — release notes + "Скачать и установить" button
 *   - download progress bar while downloading
 *   - auto-installs once download completes (broadcast receiver)
 *
 * The section is dropped into SettingsTab inside its own GlassCard.
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
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    // Listen for ACTION_DOWNLOAD_COMPLETE — when our APK finishes downloading,
    // trigger the system installer.
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
                                downloadedFile = file
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
        // On Android 13+ (API 33+) we MUST specify RECEIVER_EXPORTED or
        // RECEIVER_NOT_EXPORTED when registering a runtime receiver —
        // otherwise registerReceiver() throws SecurityException and crashes
        // the app. ACTION_DOWNLOAD_COMPLETE is a system broadcast, so we
        // use RECEIVER_NOT_EXPORTED (we don't need other apps to talk to us).
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
        SectionHeader(
            icon = Icons.Default.SystemUpdate,
            title = "Обновления",
            subtitle = "Текущая версия ${BuildConfig.VERSION_NAME}",
        )

        if (checking) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Загрузка обновления…",
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
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        release?.let { r ->
            UpdateAvailableCard(
                release = r,
                downloading = downloading,
                onDownload = {
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
                modifier = Modifier.weight(1f).height(48.dp),
                shape = AppShapes.Large,
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Проверить", fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            "Источник: github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
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
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = if (isDark) 0.30f else 0.50f),
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

// Local helper used by UpdateSection — kept private to avoid collisions.
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
            shape = AppShapes.Small,
            color = scheme.primary.copy(alpha = 0.14f),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                Color.White.copy(alpha = 0.20f),
            ),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp),
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
