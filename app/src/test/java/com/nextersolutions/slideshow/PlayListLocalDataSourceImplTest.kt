package com.nextersolutions.slideshow

import com.nextersolutions.slideshow.database.PlayListLocalDataSourceImpl
import com.nextersolutions.slideshow.database.PlaylistDao
import com.nextersolutions.slideshow.database.PlaylistItemEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayListLocalDataSourceImplTest {

    private lateinit var dao: PlaylistDao
    private lateinit var dataSource: PlayListLocalDataSourceImpl

    @Before
    fun setUp() {
        dao = mock()
        dataSource = PlayListLocalDataSourceImpl(dao)
    }

    @Test
    fun `observeAll delegates to dao`() {
        val flow = flowOf(listOf(entity(1, "a.jpg")))
        whenever(dao.observeAll()).thenReturn(flow)

        val result = dataSource.observeAll()

        assertEquals(flow, result)
        verify(dao).observeAll()
    }

    @Test
    fun `getAll delegates to dao`() = runTest {
        val entities = listOf(entity(1, "a.jpg"))
        whenever(dao.getAll()).thenReturn(entities)

        assertEquals(entities, dataSource.getAll())
        verify(dao).getAll()
    }

    @Test
    fun `findByCreativeKey delegates to dao`() = runTest {
        val e = entity(1, "img.jpg")
        whenever(dao.findByCreativeKey("img.jpg")).thenReturn(e)

        assertEquals(e, dataSource.findByCreativeKey("img.jpg"))
        verify(dao).findByCreativeKey("img.jpg")
    }

    @Test
    fun `upsert delegates to dao`() = runTest {
        val items = listOf(entity(1, "a.jpg"))

        dataSource.upsert(items)

        verify(dao).upsert(items)
    }

    @Test
    fun `setLocalPath delegates to dao`() = runTest {
        dataSource.setLocalPath("img.jpg", "/storage/img.jpg")

        verify(dao).setLocalPath("img.jpg", "/storage/img.jpg")
    }

    @Test
    fun `deleteAll delegates to dao`() = runTest {
        dataSource.deleteAll()

        verify(dao).deleteAll()
    }

    @Test
    fun `deleteStaleItems delegates to dao`() = runTest {
        val keys = listOf("a.jpg", "b.jpg")

        dataSource.deleteStaleItems(keys)

        verify(dao).deleteStaleItems(keys)
    }

    @Test
    fun `getDownloadedCreativeKeys delegates to dao`() = runTest {
        whenever(dao.getDownloadedCreativeKeys()).thenReturn(listOf("a.jpg"))

        val result = dataSource.getDownloadedCreativeKeys()

        assertEquals(listOf("a.jpg"), result)
        verify(dao).getDownloadedCreativeKeys()
    }

    private fun entity(orderKey: Int, creativeKey: String) =
        PlaylistItemEntity(orderKey, "screen", creativeKey, 10, null)
}
