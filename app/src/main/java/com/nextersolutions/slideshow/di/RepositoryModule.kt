package com.nextersolutions.slideshow.di

import com.nextersolutions.slideshow.core.api.datasource.local.PlayListLocalDataSource
import com.nextersolutions.slideshow.core.api.datasource.remote.RemoteDataSource
import com.nextersolutions.slideshow.core.api.repository.PlaylistRepository
import com.nextersolutions.slideshow.data.repository.PlaylistRepositoryImpl
import com.nextersolutions.slideshow.data.storage.FileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePlaylistRepository(
        api: RemoteDataSource,
        playListLocalDataSource: PlayListLocalDataSource,
        fileManager: FileManager
    ): PlaylistRepository {
        return PlaylistRepositoryImpl(api, playListLocalDataSource, fileManager)
    }
}