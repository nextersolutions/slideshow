package com.nextersolutions.slideshow.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
