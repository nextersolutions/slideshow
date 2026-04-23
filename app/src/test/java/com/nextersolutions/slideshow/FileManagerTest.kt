package com.nextersolutions.slideshow

import android.content.Context
import com.nextersolutions.slideshow.data.storage.FileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class FileManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var fileManager: FileManager

    @Before
    fun setUp() {
        val context: Context = mock()
        whenever(context.filesDir).thenReturn(tmpFolder.root)
        fileManager = FileManager(context)
    }

    @Test
    fun `isDownloaded returns false when file does not exist`() {
        assertFalse(fileManager.isDownloaded("missing.jpg"))
    }

    @Test
    fun `isDownloaded returns false for empty file`() {
        tmpFolder.newFolder("slideshow_media")
        tmpFolder.newFile("slideshow_media/empty.jpg") // 0 bytes
        assertFalse(fileManager.isDownloaded("empty.jpg"))
    }

    @Test
    fun `isDownloaded returns true for existing non-empty file`() {
        tmpFolder.newFolder("slideshow_media")
        tmpFolder.newFile("slideshow_media/real.jpg").writeText("data")
        assertTrue(fileManager.isDownloaded("real.jpg"))
    }

    @Test
    fun `absolutePathFor returns path inside slideshow_media dir`() {
        val path = fileManager.absolutePathFor("img.jpg")
        assertTrue(path.endsWith("slideshow_media/img.jpg"))
    }

    @Test
    fun `fileFor returns File inside slideshow_media dir`() {
        val file = fileManager.fileFor("video.mp4")
        assertEquals("video.mp4", file.name)
        assertTrue(file.parent!!.endsWith("slideshow_media"))
    }

    @Test
    fun `pruneUnreferenced deletes files not in active set`() {
        val mediaDir = tmpFolder.newFolder("slideshow_media")
        val stale = File(mediaDir, "stale.jpg").also { it.writeText("x") }
        val active = File(mediaDir, "keep.jpg").also { it.writeText("x") }

        fileManager.pruneUnreferenced(setOf("keep.jpg"))

        assertFalse(stale.exists())
        assertTrue(active.exists())
    }

    @Test
    fun `pruneUnreferenced deletes all files when active set is empty`() {
        val mediaDir = tmpFolder.newFolder("slideshow_media")
        File(mediaDir, "a.jpg").writeText("x")
        File(mediaDir, "b.jpg").writeText("x")

        fileManager.pruneUnreferenced(emptySet())

        assertEquals(0, mediaDir.listFiles()?.size ?: 0)
    }

    @Test
    fun `pruneUnreferenced does nothing when all files are active`() {
        val mediaDir = tmpFolder.newFolder("slideshow_media")
        File(mediaDir, "a.jpg").writeText("x")

        fileManager.pruneUnreferenced(setOf("a.jpg"))

        assertEquals(1, mediaDir.listFiles()?.size ?: 0)
    }

    @Test
    fun `pruneUnreferenced does nothing when media dir is empty`() {
        tmpFolder.newFolder("slideshow_media")
        // Should not throw
        fileManager.pruneUnreferenced(setOf("a.jpg"))
    }
}
