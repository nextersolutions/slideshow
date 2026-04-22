package com.nextersolutions.slideshow.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextersolutions.slideshow.data.repository.PlaylistRepository
import com.nextersolutions.slideshow.data.storage.ScreenKeyPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Fetches the latest playlist metadata for the current screen key and queues
 * a [DownloadCreativeWorker] for every creative whose file is not yet on disk.
 *
 * The worker is idempotent — if nothing has changed, no downloads are queued
 * and the player keeps playing the existing items.
 */
@HiltWorker
class SyncPlaylistWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PlaylistRepository,
    private val screenKeyPreferences: ScreenKeyPreferences,
    private val scheduler: PlaylistScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val screenKey = screenKeyPreferences.getScreenKey()
            val missing = repository.refreshPlaylist(screenKey)
            missing.forEach { creativeKey ->
                scheduler.enqueueDownload(creativeKey)
            }
            Result.success()
        } catch (t: Throwable) {
            // If we're offline, falling back to the cached playlist is fine —
            // the next alarm tick will retry. Signal "retry" a few times first
            // to pick up transient failures.
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        const val UNIQUE_NAME = "sync-playlist"
    }
}
