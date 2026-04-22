package com.nextersolutions.slideshow.network

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
class SlideshowApi @Inject constructor(
    private val httpClient: HttpClient,
) {

    suspend fun getPlaylist(screenKey: String): PlaylistResponseDto {
        return httpClient
            .get("${BASE_URL}screen/playlistItems/$screenKey")
            .body()
    }

    /**
     * Streams a creative file. The caller is responsible for consuming the channel
     * and closing it (writing to disk from a WorkManager worker in our case).
     */
    suspend fun downloadCreativeStream(
        creativeKey: String,
        onChannel: suspend (ByteReadChannel, Long?) -> Unit,
    ) {
        httpClient.prepareGet("${BASE_URL}creative/get/$creativeKey").execute { response ->
            val length = response.headers["Content-Length"]?.toLongOrNull()
            onChannel(response.bodyAsChannel(), length)
        }
    }

    companion object {
        const val BASE_URL = "https://test.onsignage.com/PlayerBackend/"
    }
}
