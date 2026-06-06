package com.iptv.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.ChannelDao
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
}
