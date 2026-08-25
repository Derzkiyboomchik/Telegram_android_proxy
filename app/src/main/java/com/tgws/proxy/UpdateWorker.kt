package com.tgws.proxy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * UpdateWorker — periodic and on-demand background check for a new GitHub release.
 *
 * Runs roughly every 12 hours or on demand. When a newer release is found, posts a
 * high-priority push notification prompting the user to update.
 */
class UpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val release = UpdateChecker.fetchLatestRelease(context) ?: return Result.success()
        if (!UpdateChecker.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
            return Result.success()
        }
        postUpdateNotification(context, release)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "tgwsproxy_update_check"
        const val CHANNEL_ID = "tgwsproxy_updates_v2"
        const val NOTIFICATION_ID = 2001

        fun postUpdateNotification(context: Context, release: UpdateChecker.ReleaseInfo) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Обновления приложения",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Уведомления о новых релизах Telegram WS Proxy на GitHub"
                    setShowBadge(true)
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "settings")
                putExtra("check_update", true)
            }
            val pi = PendingIntent.getActivity(
                context,
                1001,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Доступна новая версия ${release.versionName}!")
                .setContentText("Нажмите для загрузки и установки обновления")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle("Вышло обновление ${release.versionName}")
                        .bigText(release.releaseNotes.take(300).ifBlank { "Доступна новая версия на GitHub. Нажмите для установки." })
                )
                .setContentIntent(pi)
                .addAction(
                    android.R.drawable.stat_sys_download,
                    "Обновить",
                    pi
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            nm.notify(NOTIFICATION_ID, notif)
        }

        /**
         * Schedules the periodic update check.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(
                12, TimeUnit.HOURS,
                15, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Runs a one-time immediate background update check.
         */
        fun checkNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
