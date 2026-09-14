package com.tgws.proxy

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * UpdateChecker — checks GitHub Releases for a newer APK and downloads it.
 */
object UpdateChecker {

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val apkUrl: String,
        val apkSize: Long,
        val releaseNotes: String,
        val htmlUrl: String,
        val publishedAt: String = "",
    )

    /**
     * Returns the latest release info, or null if no update is available.
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
                parseRelease(JSONObject(raw))
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches all published releases from GitHub repository.
     */
    suspend fun fetchAllReleases(@Suppress("UNUSED_PARAMETER") context: Context): List<ReleaseInfo> = withContext(Dispatchers.IO) {
        val owner = BuildConfig.GITHUB_OWNER
        val repo = BuildConfig.GITHUB_REPO
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases"
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
                val array = JSONArray(raw)
                val list = mutableListOf<ReleaseInfo>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    parseRelease(item)?.let { list.add(it) }
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
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

    /**
     * True if the remote version is strictly older than current (downgrade).
     */
    fun isOlder(remote: String, current: String): Boolean {
        val r = remote.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
        val c = current.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv < cv
        }
        return false
    }

    private fun parseRelease(json: JSONObject): ReleaseInfo? {
        val tagName = json.optString("tag_name", "")
        if (tagName.isEmpty()) return null
        val name = json.optString("name", tagName)
        val body = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "")
        val publishedAt = json.optString("published_at", json.optString("created_at", ""))
        val assets = json.optJSONArray("assets") ?: return null

        // Pick the best .apk asset (prefer release / universal / matching ABI)
        val currentAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        var apkAsset: JSONObject? = null
        var bestScore = -1
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val assetName = asset.optString("name", "")
            if (!assetName.endsWith(".apk", ignoreCase = true)) continue
            val score = when {
                assetName.contains(currentAbi, ignoreCase = true) -> 4
                assetName.contains("release", ignoreCase = true) -> 3
                assetName.contains("universal", ignoreCase = true) -> 2
                else -> 1
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
            publishedAt = publishedAt.take(10), // YYYY-MM-DD
        )
    }

    /**
     * Downloads the APK via the system DownloadManager and returns the
     * download id so the caller can track completion.
     */
    fun downloadApk(context: Context, release: ReleaseInfo): Long {
        val updatesDir = File(context.externalCacheDir, "updates").apply { mkdirs() }
        // Clean older APKs first
        updatesDir.listFiles()?.forEach { it.delete() }
        val outFile = File(updatesDir, "tgwsproxy-${release.versionName}.apk")

        val request = DownloadManager.Request(Uri.parse(release.apkUrl)).apply {
            setTitle("TG WS Proxy ${release.versionName}")
            setDescription("Загрузка версии ${release.versionName}")
            setDestinationUri(Uri.fromFile(outFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    /**
     * Triggers the system package installer for the given APK file.
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
