package com.nextersolutions.slideshow.domain.usecase

import com.nextersolutions.slideshow.work.PlaylistScheduler
import javax.inject.Inject

class SyncPlaylistUseCase @Inject constructor(
    private val scheduler: PlaylistScheduler,
) {
    operator fun invoke(immediate: Boolean = true) {
        if (immediate) scheduler.enqueueImmediateSync()
        scheduler.schedulePeriodicSync()
    }
}
