package com.iptv.player.di

import android.content.Context
import androidx.room.Room
import com.iptv.player.data.local.AppDatabase
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.ChannelDao
import com.iptv.player.data.local.dao.EpgDao
import com.iptv.player.data.local.dao.FavoriteDao
import com.iptv.player.data.local.dao.ProfileDao
import com.iptv.player.data.local.dao.ResumeDao
import com.iptv.player.data.local.dao.SeriesDao
import com.iptv.player.data.local.dao.VodDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "iptv.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideChannelDao(db: AppDatabase): ChannelDao = db.channelDao()

    @Provides
    fun provideEpgDao(db: AppDatabase): EpgDao = db.epgDao()

    @Provides
    fun provideVodDao(db: AppDatabase): VodDao = db.vodDao()

    @Provides
    fun provideSeriesDao(db: AppDatabase): SeriesDao = db.seriesDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideResumeDao(db: AppDatabase): ResumeDao = db.resumeDao()
}
