package com.nextersolutions.slideshow

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextersolutions.slideshow.database.PlaylistDao
import com.nextersolutions.slideshow.database.PlaylistItemEntity
import com.nextersolutions.slideshow.database.SlideshowDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: SlideshowDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SlideshowDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_insertsItems_getAll_returnsThem() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg"), entity(2, "b.jpg")))

        val all = dao.getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun upsert_replacesExistingRowByOrderKey() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg")))
        dao.upsert(listOf(entity(1, "replaced.jpg")))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("replaced.jpg", all.first().creativeKey)
    }

    @Test
    fun getAll_returnsSortedByOrderKeyAscending() = runTest {
        dao.upsert(listOf(entity(3, "c.jpg"), entity(1, "a.jpg"), entity(2, "b.jpg")))

        val keys = dao.getAll().map { it.orderKey }
        assertEquals(listOf(1, 2, 3), keys)
    }

    @Test
    fun observeAll_emitsCurrentItems() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg")))

        val items = dao.observeAll().first()
        assertEquals(1, items.size)
    }

    @Test
    fun observeAll_emitsUpdatedItemsAfterUpsert() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg")))
        dao.upsert(listOf(entity(2, "b.jpg")))

        val items = dao.observeAll().first()
        assertEquals(2, items.size)
    }

    @Test
    fun findByCreativeKey_returnsMatchingEntity() = runTest {
        dao.upsert(listOf(entity(1, "img.jpg")))

        val found = dao.findByCreativeKey("img.jpg")
        assertNotNull(found)
        assertEquals("img.jpg", found!!.creativeKey)
    }

    @Test
    fun findByCreativeKey_returnsNullWhenNotFound() = runTest {
        val found = dao.findByCreativeKey("nope.jpg")
        assertNull(found)
    }

    @Test
    fun findByCreativeKey_returnsFirstMatchWhenDuplicateCreativeKeys() = runTest {
        dao.upsert(listOf(entity(1, "dup.jpg"), entity(2, "dup.jpg")))

        val found = dao.findByCreativeKey("dup.jpg")
        assertNotNull(found)
    }

    @Test
    fun setLocalPath_updatesPathForMatchingCreativeKey() = runTest {
        dao.upsert(listOf(entity(1, "img.jpg")))

        dao.setLocalPath("img.jpg", "/storage/img.jpg")

        assertEquals("/storage/img.jpg", dao.findByCreativeKey("img.jpg")!!.localPath)
    }

    @Test
    fun setLocalPath_updatesAllRowsWithSameCreativeKey() = runTest {
        dao.upsert(listOf(entity(1, "dup.jpg"), entity(2, "dup.jpg")))

        dao.setLocalPath("dup.jpg", "/storage/dup.jpg")

        val all = dao.getAll().filter { it.creativeKey == "dup.jpg" }
        assertTrue(all.all { it.localPath == "/storage/dup.jpg" })
    }

    @Test
    fun setLocalPath_doesNothingWhenCreativeKeyNotFound() = runTest {
        dao.upsert(listOf(entity(1, "other.jpg")))

        dao.setLocalPath("missing.jpg", "/x")

        assertNull(dao.findByCreativeKey("other.jpg")!!.localPath)
    }

    @Test
    fun deleteAll_removesAllItems() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg"), entity(2, "b.jpg")))

        dao.deleteAll()

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteAll_onEmptyTable_doesNotThrow() = runTest {
        dao.deleteAll() // should not throw
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteStaleItems_removesOnlyItemsNotInActiveSet() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg"), entity(2, "b.jpg"), entity(3, "c.jpg")))

        dao.deleteStaleItems(listOf("a.jpg", "c.jpg"))

        val remaining = dao.getAll().map { it.creativeKey }
        assertEquals(listOf("a.jpg", "c.jpg"), remaining)
    }

    @Test
    fun deleteStaleItems_deletesEverythingWhenActiveSetIsEmpty() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg")))

        dao.deleteStaleItems(emptyList())

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun deleteStaleItems_keepsEverythingWhenAllAreActive() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg"), entity(2, "b.jpg")))

        dao.deleteStaleItems(listOf("a.jpg", "b.jpg"))

        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun getDownloadedCreativeKeys_returnsOnlyKeysWithLocalPath() = runTest {
        dao.upsert(listOf(
            entity(1, "a.jpg", localPath = "/a.jpg"),
            entity(2, "b.jpg", localPath = null),
        ))

        val keys = dao.getDownloadedCreativeKeys()

        assertEquals(listOf("a.jpg"), keys)
    }

    @Test
    fun getDownloadedCreativeKeys_deduplicatesSameCreativeKey() = runTest {
        dao.upsert(listOf(
            entity(1, "dup.jpg", localPath = "/dup.jpg"),
            entity(2, "dup.jpg", localPath = "/dup.jpg"),
        ))

        val keys = dao.getDownloadedCreativeKeys()

        assertEquals(1, keys.size)
    }

    @Test
    fun getDownloadedCreativeKeys_returnsEmptyWhenNoneDownloaded() = runTest {
        dao.upsert(listOf(entity(1, "a.jpg", localPath = null)))

        assertTrue(dao.getDownloadedCreativeKeys().isEmpty())
    }

    private fun entity(
        orderKey: Int,
        creativeKey: String,
        localPath: String? = null,
    ) = PlaylistItemEntity(
        orderKey = orderKey,
        screenKey = "screen",
        creativeKey = creativeKey,
        durationSec = 10,
        localPath = localPath,
    )
}
