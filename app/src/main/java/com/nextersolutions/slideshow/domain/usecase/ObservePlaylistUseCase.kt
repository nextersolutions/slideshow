package com.nextersolutions.slideshow.domain.usecase

import com.nextersolutions.slideshow.core.api.repository.PlaylistRepository
import com.nextersolutions.slideshow.data.repository.PlaylistRepositoryImpl
import com.nextersolutions.slideshow.domain.mapper.PlaylistMappers.toViewData
import com.nextersolutions.slideshow.domain.model.PlaylistItemViewData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObservePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository,
) {
    operator fun invoke(): Flow<List<PlaylistItemViewData>> =
        repository.observeItems().map { it.toViewData() }
}
