package com.nextersolutions.slideshow.di

import android.content.Context
import androidx.room.Room
import com.nextersolutions.slideshow.database.PlaylistDao
import com.nextersolutions.slideshow.database.SlideshowDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SlideshowDatabase =
        Room.databaseBuilder(context, SlideshowDatabase::class.java, SlideshowDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePlaylistDao(db: SlideshowDatabase): PlaylistDao = db.playlistDao()
}
