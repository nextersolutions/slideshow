package com.nextersolutions.slideshow.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all background scheduling for the app.
 *
 * We deliberately do not use [androidx.work.PeriodicWorkRequest] because its
 * minimum interval is 15 minutes; instead we chain a [OneTimeWorkRequest] that
 * re-schedules itself via [AlarmManager] every [PERIODIC_INTERVAL_MS].
 */
@Singleton
class PlaylistScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager get() = WorkManager.getInstance(context)
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    /** Runs a playlist sync immediately (subject to WorkManager's scheduling). */
    fun enqueueImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncPlaylistWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            SyncPlaylistWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Schedules the next periodic sync via AlarmManager. The broadcast receiver
     * re-enqueues a SyncPlaylistWorker and then calls back into this method,
     * creating a self-sustaining 1-minute loop.
     */
    fun schedulePeriodicSync() {
        val pendingIntent = periodicPendingIntent()
        val triggerAt = SystemClock.elapsedRealtime() + PERIODIC_INTERVAL_MS

        // On Android 12+, setExactAndAllowWhileIdle requires SCHEDULE_EXACT_ALARM
        // or USE_EXACT_ALARM. We declared USE_EXACT_ALARM in the manifest for
        // the common case; fall back to inexact on devices where exact alarms
        // aren't available to avoid crashing.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        } catch (se: SecurityException) {
            // Fallback if a rare OEM revokes our permission at runtime.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
    }

    fun cancelPeriodicSync() {
        alarmManager.cancel(periodicPendingIntent())
    }

    fun enqueueDownload(creativeKey: String) {
        val request = OneTimeWorkRequestBuilder<DownloadCreativeWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(DownloadCreativeWorker.KEY_CREATIVE_KEY to creativeKey)
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            DownloadCreativeWorker.uniqueName(creativeKey),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun periodicPendingIntent(): PendingIntent {
        val intent = Intent(context, PlaylistAlarmReceiver::class.java).apply {
            action = PlaylistAlarmReceiver.ACTION_TICK
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    companion object {
        /** One minute by default, as requested by the spec. */
        const val PERIODIC_INTERVAL_MS = 60_000L
        private const val REQUEST_CODE = 0x5117
    }
}
