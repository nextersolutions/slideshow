package com.nextersolutions.slideshow.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mediaDir: File by lazy {
        File(context.filesDir, MEDIA_DIR).apply { if (!exists()) mkdirs() }
    }

    fun fileFor(creativeKey: String): File = File(mediaDir, creativeKey)

    fun isDownloaded(creativeKey: String): Boolean {
        val f = fileFor(creativeKey)
        return f.exists() && f.length() > 0
    }

    fun absolutePathFor(creativeKey: String): String = fileFor(creativeKey).absolutePath

    fun pruneUnreferenced(referencedCreativeKeys: Set<String>) {
        mediaDir.listFiles()?.forEach { f ->
            if (f.name !in referencedCreativeKeys) {
                f.delete()
            }
        }
    }

    private companion object {
        const val MEDIA_DIR = "slideshow_media"
    }
}
