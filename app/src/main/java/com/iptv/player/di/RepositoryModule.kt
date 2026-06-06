package com.iptv.player.di

import com.iptv.player.data.repository.AccountRepositoryImpl
import com.iptv.player.data.repository.LiveRepositoryImpl
import com.iptv.player.domain.repository.AccountRepository
import com.iptv.player.domain.repository.LiveRepository
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
}
