package com.nextersolutions.slideshow

import com.nextersolutions.slideshow.core.api.repository.PlaylistRepository
import com.nextersolutions.slideshow.data.storage.ScreenKeyPreferences
import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.domain.model.MediaType
import com.nextersolutions.slideshow.domain.usecase.ObservePlaylistUseCase
import com.nextersolutions.slideshow.domain.usecase.ObserveScreenKeyUseCase
import com.nextersolutions.slideshow.domain.usecase.SetScreenKeyUseCase
import com.nextersolutions.slideshow.domain.usecase.SyncPlaylistUseCase
import com.nextersolutions.slideshow.work.PlaylistScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UseCaseTest {

    @Test
    fun `ObservePlaylistUseCase maps entities with localPath to view data`() = runTest {
        val entity = PlaylistItemEntity(
            orderKey = 1, screenKey = "key", creativeKey = "img.jpg",
            durationSec = 5, localPath = "/img.jpg"
        )
        val repository: PlaylistRepository = mock()
        whenever(repository.observeItems()).thenReturn(flowOf(listOf(entity)))

        val result = ObservePlaylistUseCase(repository).invoke().first()

        assertEquals(1, result.size)
        assertEquals("img.jpg", result.first().creativeKey)
        assertEquals(MediaType.IMAGE, result.first().mediaType)
    }

    @Test
    fun `ObservePlaylistUseCase emits empty list when repository is empty`() = runTest {
        val repository: PlaylistRepository = mock()
        whenever(repository.observeItems()).thenReturn(flowOf(emptyList()))

        val result = ObservePlaylistUseCase(repository).invoke().first()

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `ObservePlaylistUseCase filters out entities without localPath`() = runTest {
        val withPath = PlaylistItemEntity(1, "key", "a.jpg", 5, "/a.jpg")
        val withoutPath = PlaylistItemEntity(2, "key", "b.jpg", 5, null)
        val repository: PlaylistRepository = mock()
        whenever(repository.observeItems()).thenReturn(flowOf(listOf(withPath, withoutPath)))

        val result = ObservePlaylistUseCase(repository).invoke().first()

        assertEquals(1, result.size)
        assertEquals("a.jpg", result.first().creativeKey)
    }

    @Test
    fun `ObserveScreenKeyUseCase delegates emission to prefs`() = runTest {
        val prefs: ScreenKeyPreferences = mock()
        whenever(prefs.observeScreenKey()).thenReturn(flowOf("screen-abc"))

        val result = ObserveScreenKeyUseCase(prefs).invoke().first()

        assertEquals("screen-abc", result)
    }

    @Test
    fun `SetScreenKeyUseCase saves trimmed key and triggers immediate and periodic sync`() = runTest {
        val prefs: ScreenKeyPreferences = mock()
        val scheduler: PlaylistScheduler = mock()

        SetScreenKeyUseCase(prefs, SyncPlaylistUseCase(scheduler)).invoke("  my-key  ")

        verify(prefs).setScreenKey("my-key")
        verify(scheduler).enqueueImmediateSync()
        verify(scheduler).schedulePeriodicSync()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SetScreenKeyUseCase throws for blank key`() = runTest {
        SetScreenKeyUseCase(mock(), SyncPlaylistUseCase(mock())).invoke("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SetScreenKeyUseCase throws for empty key`() = runTest {
        SetScreenKeyUseCase(mock(), SyncPlaylistUseCase(mock())).invoke("")
    }

    @Test
    fun `SetScreenKeyUseCase does not persist or sync when key is invalid`() = runTest {
        val prefs: ScreenKeyPreferences = mock()
        val scheduler: PlaylistScheduler = mock()

        runCatching {
            SetScreenKeyUseCase(prefs, SyncPlaylistUseCase(scheduler)).invoke("")
        }

        verify(prefs, never()).setScreenKey(any())
        verify(scheduler, never()).enqueueImmediateSync()
    }

    @Test
    fun `SyncPlaylistUseCase enqueues immediate and periodic sync by default`() {
        val scheduler: PlaylistScheduler = mock()

        SyncPlaylistUseCase(scheduler).invoke()

        verify(scheduler).enqueueImmediateSync()
        verify(scheduler).schedulePeriodicSync()
    }

    @Test
    fun `SyncPlaylistUseCase skips immediate sync when immediate=false`() {
        val scheduler: PlaylistScheduler = mock()

        SyncPlaylistUseCase(scheduler).invoke(immediate = false)

        verify(scheduler, never()).enqueueImmediateSync()
        verify(scheduler).schedulePeriodicSync()
    }
}
