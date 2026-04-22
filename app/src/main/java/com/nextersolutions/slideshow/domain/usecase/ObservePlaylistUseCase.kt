package com.nextersolutions.slideshow.domain.usecase

import com.nextersolutions.slideshow.data.repository.PlaylistRepository
import com.nextersolutions.slideshow.domain.mapper.PlaylistMappers.toViewData
import com.nextersolutions.slideshow.domain.model.PlaylistItemViewData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observes the current playlist as ViewData. Items without a downloaded file
 * are filtered out so the player never tries to play something that isn't on
 * disk.
 */
class ObservePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<PlaylistItemViewData>> =
        repository.observeItems().map { it.toViewData() }
}
