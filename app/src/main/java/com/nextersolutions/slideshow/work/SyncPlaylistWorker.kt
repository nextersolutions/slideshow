package com.nextersolutions.slideshow.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextersolutions.slideshow.core.api.repository.PlaylistRepository
import com.nextersolutions.slideshow.data.repository.PlaylistRepositoryImpl
import com.nextersolutions.slideshow.data.storage.ScreenKeyPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        const val UNIQUE_NAME = "sync-playlist"
    }
}
