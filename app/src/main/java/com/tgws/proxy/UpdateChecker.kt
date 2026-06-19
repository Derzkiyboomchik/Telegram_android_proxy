package com.tgws.proxy

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * UpdateChecker — checks GitHub Releases for a newer APK and downloads it.
 *
 * The release JSON is fetched from:
 *   https://api.github.com/repos/<owner>/<repo>/releases/latest
 *
 * We pick the first .apk asset in the release. The release tag must follow
 * the convention `v<versionName>` (e.g. `v1.0.2`) OR carry the version code
 * in the body — but the simplest reliable signal is the asset's own
 * `name`/`browser_download_url`, so we compare `tag_name` numerically
 * against the running build's versionName.
 *
 * Installation flow:
 *   1. User taps "Update" in the dialog.
 *   2. APK is downloaded via DownloadManager into app's external cache.
 *   3. We fire an ACTION_VIEW intent with the FileProvider URI and MIME
 *      type "application/vnd.android.package-archive" — the system package
 *      installer takes over.
 *
 * Requirements:
 *   - android.permission.REQUEST_INSTALL_PACKAGES (for the install intent
 *     to be honoured on Android 8+).
 *   - android.permission.INTERNET (already present).
 *   - FileProvider configured at "<package>.fileprovider" with paths for
 *     "updates/" under external-cache.
 *   - The APK on GitHub must be signed with the SAME key as the one
 *     currently installed, or the system installer will refuse it.
 */
object UpdateChecker {

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val apkUrl: String,
        val apkSize: Long,
        val releaseNotes: String,
        val htmlUrl: String,
    )

    /**
     * Returns the latest release info, or null if no update is available
     * (or if the network call fails — we never throw).
     */
    suspend fun fetchLatestRelease(@Suppress("UNUSED_PARAMETER") context: Context): ReleaseInfo? = withContext(Dispatchers.IO) {
        val owner = BuildConfig.GITHUB_OWNER
        val repo = BuildConfig.GITHUB_REPO
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val currentVersionName = BuildConfig.VERSION_NAME

        try {
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "TgWsProxy/$currentVersionName (Android)")
                connectTimeout = 12_000
                readTimeout = 12_000
            }
            conn.inputStream.use { stream ->
                val raw = stream.bufferedReader().use { it.readText() }
                parseRelease(raw)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True if the remote version is strictly newer than the running build.
     * Compares dotted numeric version strings (1.0.1 > 1.0.0; 1.0.10 > 1.0.2).
     */
    fun isNewer(remote: String, current: String): Boolean {
        val r = remote.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
        val c = current.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    private fun parseRelease(raw: String): ReleaseInfo? {
        val json = JSONObject(raw)
        val tagName = json.optString("tag_name", "")
        if (tagName.isEmpty()) return null
        val name = json.optString("name", tagName)
        val body = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "")
        val assets = json.optJSONArray("assets") ?: return null

        // Pick the first .apk asset. If multiple APKs (per-ABI), prefer one
        // whose name contains the current ABI; otherwise take the universal
        // one or the first .apk overall.
        val currentAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        var apkAsset: JSONObject? = null
        var bestScore = -1
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val assetName = asset.optString("name", "")
            if (!assetName.endsWith(".apk", ignoreCase = true)) continue
            val score = when {
                assetName.contains(currentAbi, ignoreCase = true) -> 3
                assetName.contains("universal", ignoreCase = true) -> 2
                assetName.contains("release", ignoreCase = true) -> 1
                else -> 0
            }
            if (score > bestScore) {
                bestScore = score
                apkAsset = asset
            }
        }
        if (apkAsset == null) return null
        return ReleaseInfo(
            tagName = tagName,
            versionName = tagName.removePrefix("v"),
            apkUrl = apkAsset.optString("browser_download_url", ""),
            apkSize = apkAsset.optLong("size", 0L),
            releaseNotes = body.ifEmpty { name },
            htmlUrl = htmlUrl,
        )
    }

    /**
     * Downloads the APK via the system DownloadManager and returns the
     * download id so the caller can track completion. The downloaded file
     * lands in app's external cache dir under "updates/".
     */
    fun downloadApk(context: Context, release: ReleaseInfo): Long {
        val updatesDir = File(context.externalCacheDir, "updates").apply { mkdirs() }
        // Clean older APKs first
        updatesDir.listFiles()?.forEach { it.delete() }
        val outFile = File(updatesDir, "tgwsproxy-${release.versionName}.apk")

        val request = DownloadManager.Request(Uri.parse(release.apkUrl)).apply {
            setTitle("TG WS Proxy ${release.versionName}")
            setDescription("Загрузка обновления")
            setDestinationUri(Uri.fromFile(outFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    /**
     * Triggers the system package installer for the given APK file. The
     * APK must already be downloaded.
     */
    fun installApk(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
