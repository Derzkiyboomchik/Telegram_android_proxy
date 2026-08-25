package com.tgws.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.Socket

class ProxyService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var statsJob: Job? = null
    private var watchdogJob: Job? = null
    private var restartJob: Job? = null
    private var networkJob: Job? = null
    private var lastNotificationContent: String = ""
    private var lastNotificationAtMs: Long = 0L
    private var notificationStartedAtMs: Long = 0L
    @Volatile
    private var stopInProgress = false
    @Volatile
    private var startInProgress = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var networkMonitor: NetworkMonitor

    // Saved intent extras for restart on kill / onTaskRemoved
    private var lastPort: Int = 1443
    private var lastIps: String = ""
    private var lastPoolSize: Int = 4
    private var lastCfEnabled: Boolean = true
    private var lastCfPriority: Boolean = true
    private var lastCfDomain: String = ""
    private var lastSecretKey: String = ""
    private var lastPowerSaver: Boolean = true

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updatePowerProfile()
        }
    }

    companion object {
        const val ACTION_START = "com.tgws.proxy.START"
        const val ACTION_STOP = "com.tgws.proxy.STOP"
        const val ACTION_RESTART = "com.tgws.proxy.RESTART"
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_IPS = "EXTRA_IPS"
        const val EXTRA_POOL_SIZE = "EXTRA_POOL_SIZE"
        const val EXTRA_CFPROXY_ENABLED = "EXTRA_CFPROXY_ENABLED"
        const val EXTRA_CFPROXY_PRIORITY = "EXTRA_CFPROXY_PRIORITY"
        const val EXTRA_CFPROXY_DOMAIN = "EXTRA_CFPROXY_DOMAIN"
        const val EXTRA_SECRET_KEY = "EXTRA_SECRET_KEY"
        const val EXTRA_POWER_SAVER = "EXTRA_POWER_SAVER"

        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "TG_WS_Proxy_Service_v4"
        private const val TAG = "ProxyService"

        // Adaptive stats polling intervals
        private const val STATS_UPDATE_ACTIVE_MS = 3_000L
        private const val STATS_UPDATE_IDLE_MS = 30_000L
        private const val NOTIFICATION_MIN_UPDATE_MS = 3_000L
        private const val NATIVE_STOP_WAIT_MS = 3_000L

        // Startup verification timeout
        private const val STARTUP_CHECK_DELAY_MS = 3000L

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        networkMonitor = NetworkMonitor(this).apply { startMonitoring() }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
        }
        registerReceiver(powerReceiver, filter)

        // Observe network state changes
        networkJob = serviceScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                NativeProxy.setNetworkOnline(isOnline)
                if (_isRunning.value && !stopInProgress && !startInProgress) {
                    if (!isOnline) {
                        Log.i(TAG, "Network lost, entering offline standby")
                        updateNotification("Ожидание сети...", force = true)
                    } else {
                        Log.i(TAG, "Network restored, resuming proxy")
                        acquireTransientWakeLock(3000L)
                        updateNotification("Прокси работает", force = true)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                LogManager.clearLogs()
                val port = intent.getIntExtra(EXTRA_PORT, 1443)
                val ips = intent.getStringExtra(EXTRA_IPS) ?: ""
                val poolSize = intent.getIntExtra(EXTRA_POOL_SIZE, 4)
                val cfEnabled = intent.getBooleanExtra(EXTRA_CFPROXY_ENABLED, true)
                val cfPriority = intent.getBooleanExtra(EXTRA_CFPROXY_PRIORITY, true)
                val cfDomain = intent.getStringExtra(EXTRA_CFPROXY_DOMAIN) ?: ""
                val secretKey = intent.getStringExtra(EXTRA_SECRET_KEY) ?: ""
                val powerSaver = intent.getBooleanExtra(EXTRA_POWER_SAVER, true)
                startProxy(port, ips, poolSize, cfEnabled, cfPriority, cfDomain, secretKey, powerSaver)
            }
            ACTION_STOP -> {
                stopProxy()
            }
            ACTION_RESTART -> {
                restartProxy()
            }
            null -> {
                if (lastPort > 0 && lastSecretKey.isNotEmpty()) {
                    Log.w(TAG, "Service restarted by system, re-starting proxy")
                    startProxy(lastPort, lastIps, lastPoolSize, lastCfEnabled, lastCfPriority, lastCfDomain, lastSecretKey, lastPowerSaver)
                } else {
                    stopSelf()
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startProxy(port: Int, ips: String, poolSize: Int = 4,
                           cfEnabled: Boolean = true, cfPriority: Boolean = true,
                           cfDomain: String = "", secretKey: String = "", powerSaver: Boolean = true) {
        if (_isRunning.value || startInProgress) return
        startInProgress = true
        stopInProgress = false

        // Save params for restart
        lastPort = port
        lastIps = ips
        lastPoolSize = poolSize
        lastCfEnabled = cfEnabled
        lastCfPriority = cfPriority
        lastCfDomain = cfDomain
        lastSecretKey = secretKey
        lastPowerSaver = powerSaver
        notificationStartedAtMs = System.currentTimeMillis()
        lastNotificationContent = "Запуск прокси..."
        lastNotificationAtMs = notificationStartedAtMs

        val notification = createNotification(lastNotificationContent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Brief transient wakelock to ensure initial socket binding under CPU throttling
        acquireTransientWakeLock(5000L)

        // Start Go proxy in a separate daemon thread
        Thread({
            try {
                NativeProxy.setNetworkOnline(networkMonitor.isOnline.value)
                NativeProxy.setPowerSaveMode(powerSaver)
                NativeProxy.setPoolSize(poolSize)
                NativeProxy.setCfProxyCacheDir(cacheDir.absolutePath)
                NativeProxy.setCfProxyConfig(cfEnabled, cfPriority, cfDomain)
                val result = NativeProxy.startProxy("127.0.0.1", port, ips, secretKey, 1)
                if (result != 0) {
                    Log.e(TAG, "StartProxy returned error code: $result")
                    serviceScope.launch {
                        updateNotification("Ошибка запуска (код: $result)", force = true)
                        delay(3000)
                        stopProxy()
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start proxy via JNA", e)
                serviceScope.launch {
                    updateNotification("Ошибка: ${e.message}", force = true)
                    delay(3000)
                    stopProxy()
                }
            } finally {
                startInProgress = false
            }
        }, "ProxyStart").apply {
            isDaemon = true
            start()
        }

        updateRunningState(true)

        // Watchdog: verify port is listening
        watchdogJob = serviceScope.launch {
            delay(STARTUP_CHECK_DELAY_MS)
            if (_isRunning.value) {
                val isListening = withContext(Dispatchers.IO) {
                    isPortOpen("127.0.0.1", port, 2000)
                }
                if (isListening) {
                    val status = if (networkMonitor.isOnline.value) "Прокси работает" else "Ожидание сети..."
                    updateNotification(status, force = true)
                    Log.i(TAG, "Proxy verified: listening on port $port")
                } else {
                    Log.e(TAG, "Proxy NOT listening on port $port after ${STARTUP_CHECK_DELAY_MS}ms")
                    updateNotification("⚠ Прокси не отвечает", force = true)
                }
            }
        }

        // Adaptive Stats Updater: polls infrequently when idle / screen is off to save battery
        statsJob = serviceScope.launch {
            var activeSessionCount = 0
            while (isActive) {
                val isOnline = networkMonitor.isOnline.value
                val isInteractive = isScreenInteractive()
                
                val pollDelay = when {
                    !isOnline -> STATS_UPDATE_IDLE_MS
                    !isInteractive && activeSessionCount == 0 -> STATS_UPDATE_IDLE_MS
                    else -> STATS_UPDATE_ACTIVE_MS
                }
                delay(pollDelay)

                if (_isRunning.value && !stopInProgress && isOnline) {
                    try {
                        val rawStats = NativeProxy.getStats() ?: continue
                        val upRaw = extractStat(rawStats, "up=")
                        val downRaw = extractStat(rawStats, "down=")
                        val activeConns = extractStat(rawStats, "active=")

                        activeSessionCount = activeConns.toIntOrNull() ?: 0
                        val totalBytes = parseHumanBytes(upRaw) + parseHumanBytes(downRaw)
                        val text = "Трафик: ${formatBytes(totalBytes)} · $activeSessionCount сесс."
                        updateNotification(text)
                    } catch (e: Exception) {
                        Log.w(TAG, "Stats update failed", e)
                    }
                }
            }
        }
    }

    private fun isScreenInteractive(): Boolean {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isInteractive
        } catch (_: Exception) {
            true
        }
    }

    private fun updatePowerProfile() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isSystemPowerSave = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pm.isPowerSaveMode
        } else false
        val screenOff = !pm.isInteractive

        val enablePowerSave = lastPowerSaver || isSystemPowerSave || screenOff
        NativeProxy.setPowerSaveMode(enablePowerSave)
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun updateNotification(content: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force) {
            if (content == lastNotificationContent) return
            if (lastNotificationAtMs != 0L && now - lastNotificationAtMs < NOTIFICATION_MIN_UPDATE_MS) return
        }

        lastNotificationContent = content
        lastNotificationAtMs = now
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, createNotification(content))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    private fun restartProxy() {
        if (restartJob?.isActive == true) return
        if (lastPort <= 0 || lastSecretKey.isEmpty()) {
            Log.w(TAG, "Restart requested without saved proxy configuration")
            return
        }

        restartJob = serviceScope.launch {
            Log.i(TAG, "Restarting proxy from notification")
            updateNotification("Перезапуск прокси...", force = true)

            watchdogJob?.cancel()
            watchdogJob = null
            statsJob?.cancel()
            statsJob = null

            requestNativeStop("restart")
            releaseWakeLock()
            updateRunningState(false)
            startInProgress = false
            delay(350)

            startProxy(
                port = lastPort,
                ips = lastIps,
                poolSize = lastPoolSize,
                cfEnabled = lastCfEnabled,
                cfPriority = lastCfPriority,
                cfDomain = lastCfDomain,
                secretKey = lastSecretKey,
                powerSaver = lastPowerSaver
            )
        }
    }

    private fun extractStat(stats: String, key: String): String {
        val idx = stats.indexOf(key)
        if (idx == -1) return "0B"
        val start = idx + key.length
        val end = stats.indexOf(" ", start)
        return if (end == -1) stats.substring(start) else stats.substring(start, end)
    }

    private fun parseHumanBytes(s: String): Double {
        val num = s.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        return when {
            s.endsWith("TB") -> num * 1024.0 * 1024 * 1024 * 1024
            s.endsWith("GB") -> num * 1024.0 * 1024 * 1024
            s.endsWith("MB") -> num * 1024.0 * 1024
            s.endsWith("KB") -> num * 1024.0
            else -> num
        }
    }

    private fun formatBytes(bytes: Double): String {
        if (bytes < 1024) return "%.0fB".format(bytes)
        if (bytes < 1024 * 1024) return "%.1fKB".format(bytes / 1024)
        if (bytes < 1024 * 1024 * 1024) return "%.1fMB".format(bytes / (1024 * 1024))
        return "%.2fGB".format(bytes / (1024 * 1024 * 1024))
    }

    private fun stopProxy() {
        if (stopInProgress) return
        stopInProgress = true
        restartJob?.cancel()
        restartJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        statsJob?.cancel()
        statsJob = null
        serviceScope.launch {
            updateNotification("Остановка прокси...", force = true)
            requestNativeStop("stop")
            releaseWakeLock()
            updateRunningState(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    private suspend fun requestNativeStop(reason: String): Boolean {
        val completed = CompletableDeferred<Unit>()
        Thread({
            try {
                NativeProxy.stopProxy()
            } catch (e: Exception) {
                Log.w(TAG, "StopProxy failed during $reason", e)
            } finally {
                completed.complete(Unit)
            }
        }, "ProxyStop-$reason").apply {
            isDaemon = true
            start()
        }

        val finished = withTimeoutOrNull(NATIVE_STOP_WAIT_MS) {
            completed.await()
            true
        } ?: false

        if (!finished) {
            Log.w(TAG, "Native stop is still running after ${NATIVE_STOP_WAIT_MS}ms during $reason")
        }
        return finished
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (_isRunning.value) {
            Log.w(TAG, "onTaskRemoved: proxy is running, service stays alive")
        }
    }

    private fun acquireTransientWakeLock(durationMs: Long = 5000L) {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TgWsProxy::TransientWakeLock"
            ).apply {
                acquire(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire transient WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release WakeLock", e)
        }
        wakeLock = null
    }

    private fun updateRunningState(isRunning: Boolean) {
        _isRunning.value = isRunning
        ProxyTileService.requestSync(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Фоновый Прокси",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление о работе прокси-сервера"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ProxyService::class.java).apply {
            action = ACTION_STOP
        }
        val restartIntent = Intent(this, ProxyService::class.java).apply {
            action = ACTION_RESTART
        }
        val restartPendingIntent = PendingIntent.getService(
            this, 2, restartIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telegram WS Proxy")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Перезапуск", restartPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", stopPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(notificationStartedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis())
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(powerReceiver)
        } catch (_: Exception) {}
        networkJob?.cancel()
        networkJob = null
        networkMonitor.stopMonitoring()
        restartJob?.cancel()
        restartJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        statsJob?.cancel()
        statsJob = null
        releaseWakeLock()
        if (_isRunning.value) {
            updateRunningState(false)
        }
        startInProgress = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
