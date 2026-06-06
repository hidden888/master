package com.iptv.player.data.local

import androidx.room.TypeConverter
import com.iptv.player.core.util.StreamType

class Converters {
    @TypeConverter
    fun fromStreamType(type: StreamType): String = type.name

    @TypeConverter
    fun toStreamType(value: String): StreamType = StreamType.valueOf(value)
}
