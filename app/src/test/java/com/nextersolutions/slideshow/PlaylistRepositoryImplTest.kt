package com.nextersolutions.slideshow

import com.nextersolutions.slideshow.core.api.datasource.local.PlayListLocalDataSource
import com.nextersolutions.slideshow.core.api.datasource.remote.RemoteDataSource
import com.nextersolutions.slideshow.data.repository.PlaylistRepositoryImpl
import com.nextersolutions.slideshow.data.storage.FileManager
import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.network.dto.PlaylistDto
import com.nextersolutions.slideshow.network.dto.PlaylistItemDto
import com.nextersolutions.slideshow.network.dto.PlaylistResponseDto
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class PlaylistRepositoryImplTest {

    private lateinit var api: RemoteDataSource
    private lateinit var localDataSource: PlayListLocalDataSource
    private lateinit var fileManager: FileManager
    private lateinit var repository: PlaylistRepositoryImpl

    @Before
    fun setUp() {
        api = mock()
        localDataSource = mock()
        fileManager = mock()
        repository = PlaylistRepositoryImpl(api, localDataSource, fileManager)

        whenever(fileManager.isDownloaded(any())).thenReturn(false)
        whenever(fileManager.absolutePathFor(any())).thenAnswer { inv ->
            "/media/${inv.getArgument<String>(0)}"
        }
        whenever(fileManager.fileFor(any())).thenAnswer { inv ->
            File("/media/${inv.getArgument<String>(0)}")
        }
    }

    @Test
    fun `observeItems delegates to local data source`() = runTest {
        val entities = listOf(entity("a.jpg"))
        whenever(localDataSource.observeAll()).thenReturn(flowOf(entities))

        val result = repository.observeItems()

        verify(localDataSource).observeAll()
    }

    @Test
    fun `getItems delegates to local data source`() = runTest {
        val entities = listOf(entity("a.jpg"))
        whenever(localDataSource.getAll()).thenReturn(entities)

        val result = repository.getItems()

        assertEquals(entities, result)
    }

    @Test
    fun `findByCreativeKey returns entity from local source`() = runTest {
        val e = entity("img.jpg")
        whenever(localDataSource.findByCreativeKey("img.jpg")).thenReturn(e)
        assertEquals(e, repository.findByCreativeKey("img.jpg"))
    }

    @Test
    fun `findByCreativeKey returns null when not found`() = runTest {
        whenever(localDataSource.findByCreativeKey(any())).thenReturn(null)
        assertEquals(null, repository.findByCreativeKey("missing.jpg"))
    }

    @Test
    fun `refreshPlaylist returns creativeKeys for items with no local file`() = runTest {
        val dto = responseDto(item("a.jpg", 1), item("b.jpg", 2))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(emptyList(), emptyList())

        val missing = repository.refreshPlaylist("key")
        assertEquals(listOf("a.jpg", "b.jpg"), missing)
    }

    @Test
    fun `refreshPlaylist excludes already-downloaded creativeKeys`() = runTest {
        val dto = responseDto(item("a.jpg", 1), item("b.jpg", 2))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(emptyList(), emptyList())
        whenever(fileManager.isDownloaded("a.jpg")).thenReturn(true)

        val missing = repository.refreshPlaylist("key")

        assertEquals(listOf("b.jpg"), missing)
    }

    @Test
    fun `refreshPlaylist restores localPath from existing DB entry`() = runTest {
        val existing = entity("a.jpg", localPath = "/media/a.jpg")
        val dto = responseDto(item("a.jpg", 1))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(listOf(existing), listOf(existing))

        val missing = repository.refreshPlaylist("key")

        assertTrue(missing.isEmpty())
    }

    @Test
    fun `refreshPlaylist deduplicates missing creativeKeys`() = runTest {
        val dto = responseDto(item("a.jpg", 1), item("a.jpg", 2))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(emptyList(), emptyList())

        val missing = repository.refreshPlaylist("key")

        assertEquals(listOf("a.jpg"), missing)
    }

    @Test
    fun `refreshPlaylist calls deleteStaleItems and upsert`() = runTest {
        val dto = responseDto(item("a.jpg", 1))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(emptyList(), emptyList())

        repository.refreshPlaylist("key")

        verify(localDataSource).deleteStaleItems(listOf("a.jpg"))
        verify(localDataSource).upsert(any())
    }

    @Test
    fun `refreshPlaylist prunes unreferenced files`() = runTest {
        val dto = responseDto(item("a.jpg", 1))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(emptyList(), emptyList())

        repository.refreshPlaylist("key")

        verify(fileManager).pruneUnreferenced(setOf("a.jpg"))
    }

    @Test
    fun `refreshPlaylist does second-pass localPath restore for race-condition files`() = runTest {
        val dto = responseDto(item("a.jpg", 1))
        whenever(api.getPlaylist("key")).thenReturn(dto)
        whenever(localDataSource.getAll()).thenReturn(emptyList(), emptyList())
        whenever(fileManager.isDownloaded("a.jpg")).thenReturn(false).thenReturn(true)

        repository.refreshPlaylist("key")

        verify(localDataSource).setLocalPath(eq("a.jpg"), any())
    }

    @Test
    fun `downloadCreative skips network call when file already on disk`() = runTest {
        whenever(fileManager.isDownloaded("img.jpg")).thenReturn(true)
        whenever(fileManager.fileFor("img.jpg")).thenReturn(File("/media/img.jpg"))

        repository.downloadCreative("img.jpg")

        verify(api, never()).downloadCreativeStream(any(), any())
        verify(localDataSource).setLocalPath("img.jpg", "/media/img.jpg")
    }


    private fun item(creativeKey: String, orderKey: Int = 0, duration: Int = 10) =
        PlaylistItemDto(duration = duration, creativeKey = creativeKey, orderKey = orderKey)

    private fun responseDto(vararg items: PlaylistItemDto) =
        PlaylistResponseDto(playlists = listOf(PlaylistDto(items.toList())))

    private fun entity(
        creativeKey: String,
        orderKey: Int = 0,
        localPath: String? = null,
    ) = PlaylistItemEntity(
        orderKey = orderKey,
        screenKey = "key",
        creativeKey = creativeKey,
        durationSec = 10,
        localPath = localPath,
    )
}
