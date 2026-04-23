package com.nextersolutions.slideshow

import com.nextersolutions.slideshow.core.common.ApiRoutes
import com.nextersolutions.slideshow.network.RemoteDataSourceImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RemoteDataSourceImplTest {

    @Test
    fun `getPlaylist deserializes valid response`() = runTest {
        val json = """
            {
              "playlists": [
                {
                  "playlistItems": [
                    { "orderKey": 1, "creativeKey": "img.jpg", "duration": 10 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val sut = buildSut { respond(json, HttpStatusCode.OK, jsonHeaders()) }

        val dto = sut.getPlaylist("screen-key")

        assertEquals(1, dto.playlists.size)
        assertEquals(1, dto.playlists.first().playlistItems.size)
        with(dto.playlists.first().playlistItems.first()) {
            assertEquals(1, orderKey)
            assertEquals("img.jpg", creativeKey)
            assertEquals(10, duration)
        }
    }

    @Test
    fun `getPlaylist requests correct URL`() = runTest {
        var capturedUrl = ""
        val json = """{"playlists":[]}"""

        val sut = buildSut { request ->
            capturedUrl = request.url.toString()
            respond(json, HttpStatusCode.OK, jsonHeaders())
        }

        sut.getPlaylist("my-screen")

        assertEquals(ApiRoutes.PLAYLISTS + "my-screen", capturedUrl)
    }

    @Test
    fun `getPlaylist handles empty playlists array`() = runTest {
        val json = """{"playlists":[]}"""
        val sut = buildSut { respond(json, HttpStatusCode.OK, jsonHeaders()) }

        val dto = sut.getPlaylist("key")

        assertEquals(0, dto.playlists.size)
    }

    @Test
    fun `getPlaylist uses default values for missing fields`() = runTest {
        val json = """{"playlists":[{"playlistItems":[{}]}]}"""
        val sut = buildSut { respond(json, HttpStatusCode.OK, jsonHeaders()) }

        val dto = sut.getPlaylist("key")
        val item = dto.playlists.first().playlistItems.first()

        assertEquals(0, item.orderKey)
        assertEquals("", item.creativeKey)
        assertEquals(0, item.duration)
    }

    @Test(expected = Exception::class)
    fun `getPlaylist throws on server error`() = runTest {
        val sut = buildSut { respondError(HttpStatusCode.InternalServerError) }

        sut.getPlaylist("key")
    }

    @Test
    fun `downloadCreativeStream requests correct URL`() = runTest {
        var capturedUrl = ""
        val sut = buildSut { request ->
            capturedUrl = request.url.toString()
            respond(ByteArray(0), HttpStatusCode.OK)
        }

        sut.downloadCreativeStream("vid.mp4") { _, _ -> }

        assertEquals(ApiRoutes.GET_CREATIVE + "vid.mp4", capturedUrl)
    }

    @Test
    fun `downloadCreativeStream passes content-length to callback`() = runTest {
        val bytes = "hello".toByteArray()
        val sut = buildSut {
            respond(
                bytes, HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentLength, bytes.size.toString())
            )
        }

        var receivedLength: Long? = null
        sut.downloadCreativeStream("file.jpg") { _, length ->
            receivedLength = length
        }

        assertEquals(bytes.size.toLong(), receivedLength)
    }

    @Test
    fun `downloadCreativeStream passes null content-length when header absent`() = runTest {
        val sut = buildSut { respond(ByteArray(0), HttpStatusCode.OK) }

        var receivedLength: Long? = -1L
        sut.downloadCreativeStream("file.jpg") { _, length ->
            receivedLength = length
        }

        assertEquals(null, receivedLength)
    }

    @Test
    fun `downloadCreativeStream passes readable channel to callback`() = runTest {
        val payload = "binary-content".toByteArray()
        val sut = buildSut { respond(payload, HttpStatusCode.OK) }

        var channelReceived = false
        sut.downloadCreativeStream("file.bin") { channel, _ ->
            assertNotNull(channel)
            channelReceived = true
        }

        assert(channelReceived)
    }

    private fun buildSut(
        handler: MockRequestHandler
    ): RemoteDataSourceImpl {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return RemoteDataSourceImpl(client)
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
}
