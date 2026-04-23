package com.nextersolutions.slideshow.core.api.datasource.remote

import com.nextersolutions.slideshow.network.dto.PlaylistResponseDto
import io.ktor.utils.io.ByteReadChannel

interface RemoteDataSource {
    suspend fun getPlaylist(screenKey: String): PlaylistResponseDto
    suspend fun downloadCreativeStream(
        creativeKey: String,
        onChannel: suspend (ByteReadChannel, Long?) -> Unit,
    )
}