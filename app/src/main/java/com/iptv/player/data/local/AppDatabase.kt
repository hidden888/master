package com.iptv.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.ChannelDao
import com.iptv.player.data.local.dao.EpgDao
import com.iptv.player.data.local.dao.FavoriteDao
import com.iptv.player.data.local.dao.ProfileDao
import com.iptv.player.data.local.dao.ResumeDao
import com.iptv.player.data.local.dao.SeriesDao
import com.iptv.player.data.local.dao.VodDao
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity
import com.iptv.player.data.local.entity.EpgProgramEntity
import com.iptv.player.data.local.entity.FavoriteEntity
import com.iptv.player.data.local.entity.ProfileEntity
import com.iptv.player.data.local.entity.ResumeEntity
import com.iptv.player.data.local.entity.SeriesEntity
import com.iptv.player.data.local.entity.VodEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class,
        VodEntity::class,
        SeriesEntity::class,
        FavoriteEntity::class,
        ProfileEntity::class,
        ResumeEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
    abstract fun vodDao(): VodDao
    abstract fun seriesDao(): SeriesDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun profileDao(): ProfileDao
    abstract fun resumeDao(): ResumeDao
}
