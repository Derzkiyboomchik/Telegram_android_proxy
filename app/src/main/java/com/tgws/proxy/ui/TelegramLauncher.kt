package com.tgws.proxy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tgws.proxy.R

data class TelegramClientInfo(
    val name: String,
    val packageName: String,
    val isInstalled: Boolean,
    val icon: Drawable? = null,
    val appLabel: String? = null
)

object TelegramLauncher {

    val KNOWN_CLIENTS = listOf(
        "Telegram" to "org.telegram.messenger",
        "Telegram Beta" to "org.telegram.messenger.beta",
        "Telegram Direct (Web)" to "org.telegram.messenger.web",
        "Telegram X" to "org.thunderdog.challegram",
        "Plus Messenger" to "org.telegram.plus",
        "AyuGram" to "org.telegram.ayu",
        "NekoX" to "nekox.messenger",
        "ForkClient" to "org.forkclient.messenger",
        "Forkgram" to "org.forkgram.messenger",
        "iMe Messenger" to "com.iMe.android",
        "Kotatogram" to "org.telegram.kotatogram",
        "BGram" to "org.telegram.bgram",
        "Cherrygram" to "org.telegram.cherrygram",
        "MDGram" to "com.mdgram.messenger",
        "Turrit" to "com.turrit.telegram",
        "Teleplus" to "ir.ilm.teleplus"
    )

    fun getInstalledClients(context: Context): List<TelegramClientInfo> {
        val pm = context.packageManager
        return KNOWN_CLIENTS.mapNotNull { (defaultName, pkg) ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                TelegramClientInfo(
                    name = defaultName,
                    packageName = pkg,
                    isInstalled = true,
                    icon = icon,
                    appLabel = label
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun launch(context: Context, proxyUrl: String, packageName: String? = null) {
        val tgUri = when {
            proxyUrl.startsWith("https://t.me/proxy", ignoreCase = true) ->
                proxyUrl.replaceFirst("https://t.me/proxy", "tg://proxy")
            proxyUrl.startsWith("http://t.me/proxy", ignoreCase = true) ->
                proxyUrl.replaceFirst("http://t.me/proxy", "tg://proxy")
            else -> proxyUrl
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tgUri)).apply {
                if (!packageName.isNullOrBlank()) {
                    setPackage(packageName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try standard https:// intent
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(proxyUrl)).apply {
                    if (!packageName.isNullOrBlank()) {
                        setPackage(packageName)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                copyToClipboard(context, proxyUrl)
                Toast.makeText(context, "Ссылка скопирована в буфер обмена", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Telegram Proxy", text))
    }

    fun shareLink(context: Context, proxyUrl: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, proxyUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться прокси").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramPickerBottomSheet(
    proxyUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val installed = remember { TelegramLauncher.getInstalledClients(context) }
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(scheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Подключение к Telegram",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = if (installed.isNotEmpty()) "Выберите клиент для активации прокси:" else "Установленные клиенты не найдены",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (installed.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installed) { client ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    TelegramLauncher.launch(context, proxyUrl, client.packageName)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (client.icon != null) {
                                    Image(
                                        bitmap = client.icon.toBitmap(96, 96).asImageBitmap(),
                                        contentDescription = client.name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_telegram_logo),
                                        contentDescription = null,
                                        tint = Color(0xFF2AABEE),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = client.appLabel ?: client.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = client.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Secondary Quick Actions (Copy / Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            TelegramLauncher.copyToClipboard(context, proxyUrl)
                            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Копировать",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = scheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            TelegramLauncher.shareLink(context, proxyUrl)
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Поделиться",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = scheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
