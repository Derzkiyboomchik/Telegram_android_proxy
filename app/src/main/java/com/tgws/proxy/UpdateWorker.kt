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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * UpdateWorker — periodic background check for a new GitHub release.
 *
 * Runs roughly every 12 hours. When a newer release is found, posts a
 * notification that opens MainActivity → Settings → "Проверить обновления".
 *
 * The user must tap the notification and confirm the install dialog —
 * silent APK installation requires system-app privileges which we don't
 * have.
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

    private fun postUpdateNotification(context: Context, release: UpdateChecker.ReleaseInfo) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tgwsproxy_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Обновления",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Уведомления о новых версиях приложения"
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

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Доступно обновление ${release.versionName}")
            .setContentText("Нажмите, чтобы установить")
            .setStyle(NotificationCompat.BigTextStyle().bigText(release.releaseNotes.take(200)))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        nm.notify(2001, notif)
    }

    companion object {
        private const val WORK_NAME = "tgwsproxy_update_check"

        /**
         * Schedules the periodic update check. Idempotent — calling again
         * with ExistingPeriodicWorkPolicy.KEEP preserves the existing
         * schedule.
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
    }
}
