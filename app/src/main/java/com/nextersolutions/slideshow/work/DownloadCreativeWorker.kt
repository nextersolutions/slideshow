package com.nextersolutions.slideshow.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextersolutions.slideshow.core.api.repository.PlaylistRepository
import com.nextersolutions.slideshow.data.repository.PlaylistRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadCreativeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PlaylistRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val creativeKey = inputData.getString(KEY_CREATIVE_KEY) ?: return Result.failure()
        return try {
            val existing = repository.findByCreativeKey(creativeKey)
            if (existing?.localPath != null && java.io.File(existing.localPath).exists()) {
                return Result.success()
            }
            repository.downloadCreative(creativeKey)
            Result.success()
        } catch (ioe: java.io.IOException) {
            if (runAttemptCount < MAX_RETRY) Result.retry() else Result.failure()
        } catch (t: Throwable) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_CREATIVE_KEY = "creativeKey"
        const val MAX_RETRY = 5
        fun uniqueName(creativeKey: String) = "download-$creativeKey"
    }
}
