package com.nextersolutions.slideshow.network

import com.nextersolutions.slideshow.core.api.datasource.remote.RemoteDataSource
import com.nextersolutions.slideshow.core.common.ApiRoutes.GET_CREATIVE
import com.nextersolutions.slideshow.core.common.ApiRoutes.PLAYLISTS
import com.nextersolutions.slideshow.network.dto.PlaylistResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteDataSourceImpl @Inject constructor(
    private val httpClient: HttpClient
): RemoteDataSource {

    override suspend fun getPlaylist(screenKey: String): PlaylistResponseDto {
        return httpClient
            .get(PLAYLISTS + screenKey)
            .body()
    }

    override suspend fun downloadCreativeStream(
        creativeKey: String,
        onChannel: suspend (ByteReadChannel, Long?) -> Unit,
    ) {
        httpClient.prepareGet(GET_CREATIVE + creativeKey).execute { response ->
            val length = response.headers["Content-Length"]?.toLongOrNull()
            onChannel(response.bodyAsChannel(), length)
        }
    }
}
