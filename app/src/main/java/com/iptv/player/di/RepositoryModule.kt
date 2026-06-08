package com.iptv.player.di

import com.iptv.player.data.repository.AccountRepositoryImpl
import com.iptv.player.data.repository.EpgRepositoryImpl
import com.iptv.player.data.repository.FavoriteRepositoryImpl
import com.iptv.player.data.repository.LiveRepositoryImpl
import com.iptv.player.data.repository.SeriesRepositoryImpl
import com.iptv.player.data.repository.VodRepositoryImpl
import com.iptv.player.domain.repository.AccountRepository
import com.iptv.player.domain.repository.EpgRepository
import com.iptv.player.domain.repository.FavoriteRepository
import com.iptv.player.domain.repository.LiveRepository
import com.iptv.player.domain.repository.SeriesRepository
import com.iptv.player.domain.repository.VodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindLiveRepository(impl: LiveRepositoryImpl): LiveRepository

    @Binds
    @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds
    @Singleton
    abstract fun bindVodRepository(impl: VodRepositoryImpl): VodRepository

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository
}
