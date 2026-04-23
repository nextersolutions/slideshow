package com.nextersolutions.slideshow

import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.domain.mapper.PlaylistMappers.toEntities
import com.nextersolutions.slideshow.domain.mapper.PlaylistMappers.toViewData
import com.nextersolutions.slideshow.domain.model.MediaType
import com.nextersolutions.slideshow.network.dto.PlaylistDto
import com.nextersolutions.slideshow.network.dto.PlaylistItemDto
import com.nextersolutions.slideshow.network.dto.PlaylistResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistMappersTest {
    @Test
    fun `toEntities maps fields correctly`() {
        val dto = responseDto(item(orderKey = 1, creativeKey = "img.jpg", duration = 5))
        val entities = dto.toEntities("key123") { null }

        assertEquals(1, entities.size)
        with(entities.first()) {
            assertEquals(1, orderKey)
            assertEquals("key123", screenKey)
            assertEquals("img.jpg", creativeKey)
            assertEquals(5, durationSec)
            assertNull(localPath)
        }
    }

    @Test
    fun `toEntities calls localPathLookup and stores returned path`() {
        val dto = responseDto(item(creativeKey = "vid.mp4"))
        val entities = dto.toEntities("key") { "/storage/vid.mp4" }

        assertEquals("/storage/vid.mp4", entities.first().localPath)
    }

    @Test
    fun `toEntities filters out items with blank creativeKey`() {
        val dto = responseDto(
            item(orderKey = 1, creativeKey = "valid.jpg"),
            item(orderKey = 2, creativeKey = ""),
            item(orderKey = 3, creativeKey = "   "),
        )
        val entities = dto.toEntities("key") { null }

        assertEquals(1, entities.size)
        assertEquals("valid.jpg", entities.first().creativeKey)
    }

    @Test
    fun `toEntities sorts result by orderKey ascending`() {
        val dto = responseDto(
            item(orderKey = 3, creativeKey = "c.jpg"),
            item(orderKey = 1, creativeKey = "a.jpg"),
            item(orderKey = 2, creativeKey = "b.jpg"),
        )
        val entities = dto.toEntities("key") { null }

        assertEquals(listOf(1, 2, 3), entities.map { it.orderKey })
    }

    @Test
    fun `toEntities flattens items from multiple playlists`() {
        val dto = PlaylistResponseDto(
            playlists = listOf(
                PlaylistDto(listOf(item(orderKey = 1, creativeKey = "a.jpg"))),
                PlaylistDto(listOf(item(orderKey = 2, creativeKey = "b.jpg"))),
            )
        )
        val entities = dto.toEntities("key") { null }

        assertEquals(2, entities.size)
    }

    @Test
    fun `toEntities returns empty list when dto has no playlists`() {
        val dto = PlaylistResponseDto(playlists = emptyList())
        assertTrue(dto.toEntities("key") { null }.isEmpty())
    }

    @Test
    fun `toEntities returns empty list when all playlist items are blank`() {
        val dto = responseDto(item(creativeKey = ""), item(creativeKey = "  "))
        assertTrue(dto.toEntities("key") { null }.isEmpty())
    }

    @Test
    fun `toViewData maps entity with localPath to view data`() {
        val entities = listOf(entity(orderKey = 1, creativeKey = "photo.jpg", localPath = "/a/photo.jpg"))
        val viewData = entities.toViewData()

        assertEquals(1, viewData.size)
        with(viewData.first()) {
            assertEquals("photo.jpg", creativeKey)
            assertEquals(1, orderKey)
            assertEquals("/a/photo.jpg", localPath)
            assertEquals(MediaType.IMAGE, mediaType)
        }
    }

    @Test
    fun `toViewData filters out entities without localPath`() {
        val entities = listOf(
            entity(orderKey = 1, creativeKey = "a.jpg", localPath = "/a.jpg"),
            entity(orderKey = 2, creativeKey = "b.jpg", localPath = null),
        )
        val viewData = entities.toViewData()

        assertEquals(1, viewData.size)
        assertEquals("a.jpg", viewData.first().creativeKey)
    }

    @Test
    fun `toViewData detects VIDEO type for mp4`() {
        val entities = listOf(entity(creativeKey = "clip.mp4", localPath = "/clip.mp4"))
        assertEquals(MediaType.VIDEO, entities.toViewData().first().mediaType)
    }

    @Test
    fun `toViewData detects VIDEO type for mov`() {
        val entities = listOf(entity(creativeKey = "clip.mov", localPath = "/clip.mov"))
        assertEquals(MediaType.VIDEO, entities.toViewData().first().mediaType)
    }

    @Test
    fun `toViewData detects VIDEO type for webm`() {
        val entities = listOf(entity(creativeKey = "clip.webm", localPath = "/clip.webm"))
        assertEquals(MediaType.VIDEO, entities.toViewData().first().mediaType)
    }

    @Test
    fun `toViewData detects IMAGE type for jpg`() {
        val entities = listOf(entity(creativeKey = "img.jpg", localPath = "/img.jpg"))
        assertEquals(MediaType.IMAGE, entities.toViewData().first().mediaType)
    }

    @Test
    fun `toViewData detects IMAGE type for png`() {
        val entities = listOf(entity(creativeKey = "img.png", localPath = "/img.png"))
        assertEquals(MediaType.IMAGE, entities.toViewData().first().mediaType)
    }

    @Test
    fun `toViewData is case-insensitive for extension detection`() {
        val entities = listOf(entity(creativeKey = "CLIP.MP4", localPath = "/CLIP.MP4"))
        assertEquals(MediaType.VIDEO, entities.toViewData().first().mediaType)
    }

    @Test
    fun `toViewData id is composed of orderKey and creativeKey`() {
        val entities = listOf(entity(orderKey = 7, creativeKey = "x.jpg", localPath = "/x.jpg"))
        assertEquals("7:x.jpg", entities.toViewData().first().id)
    }

    @Test
    fun `toViewData returns empty list for empty input`() {
        assertTrue(emptyList<PlaylistItemEntity>().toViewData().isEmpty())
    }

    private fun item(
        orderKey: Int = 0,
        creativeKey: String = "file.jpg",
        duration: Int = 10,
    ) = PlaylistItemDto(duration = duration, creativeKey = creativeKey, orderKey = orderKey)

    private fun responseDto(vararg items: PlaylistItemDto) =
        PlaylistResponseDto(playlists = listOf(PlaylistDto(items.toList())))

    private fun entity(
        orderKey: Int = 0,
        creativeKey: String = "file.jpg",
        durationSec: Int = 10,
        localPath: String? = null,
    ) = PlaylistItemEntity(
        orderKey = orderKey,
        screenKey = "key",
        creativeKey = creativeKey,
        durationSec = durationSec,
        localPath = localPath,
    )
}
