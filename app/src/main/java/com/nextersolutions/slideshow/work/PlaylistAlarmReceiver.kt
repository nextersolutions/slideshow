package com.nextersolutions.slideshow.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives the periodic alarm, enqueues a [SyncPlaylistWorker], and re-arms
 * the alarm for the next tick. Together with [PlaylistScheduler] this gives
 * us roughly 1-minute period with WorkManager doing the actual network I/O.
 */
@AndroidEntryPoint
class PlaylistAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: PlaylistScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TICK) return
        scheduler.enqueueImmediateSync()
        scheduler.schedulePeriodicSync()
    }

    companion object {
        const val ACTION_TICK = "com.nextersolutions.slideshow.ACTION_TICK"
    }
}
