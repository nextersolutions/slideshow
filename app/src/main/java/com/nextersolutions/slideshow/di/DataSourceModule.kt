package com.nextersolutions.slideshow.di

import com.nextersolutions.slideshow.core.api.datasource.local.PlayListLocalDataSource
import com.nextersolutions.slideshow.core.api.datasource.remote.RemoteDataSource
import com.nextersolutions.slideshow.database.PlayListLocalDataSourceImpl
import com.nextersolutions.slideshow.database.PlaylistDao
import com.nextersolutions.slideshow.network.RemoteDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun providesPlayListLocalDataSource(
        dao: PlaylistDao
    ): PlayListLocalDataSource = PlayListLocalDataSourceImpl(dao)

    @Provides
    @Singleton
    fun providesRemoteDataSource(
        client: HttpClient
    ): RemoteDataSource = RemoteDataSourceImpl(client)
}