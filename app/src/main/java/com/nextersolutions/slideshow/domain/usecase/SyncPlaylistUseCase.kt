package com.nextersolutions.slideshow.domain.usecase

import com.nextersolutions.slideshow.work.PlaylistScheduler
import javax.inject.Inject

/**
 * Kicks off an immediate one-shot playlist sync, plus (re-)schedules the
 * periodic alarm so the app keeps itself up to date in the background.
 */
class SyncPlaylistUseCase @Inject constructor(
    private val scheduler: PlaylistScheduler,
) {
    operator fun invoke(immediate: Boolean = true) {
        if (immediate) scheduler.enqueueImmediateSync()
        scheduler.schedulePeriodicSync()
    }
}
