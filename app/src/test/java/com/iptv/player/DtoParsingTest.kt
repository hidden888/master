package com.iptv.player

import com.iptv.player.data.remote.dto.LiveStreamDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DtoParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `live stream parses numeric fields whether they are numbers or strings`() {
        val payload = """
            [
              {"num": 1, "name": "Channel A", "stream_id": 100, "tv_archive": 1, "category_id": "5"},
              {"num": "2", "name": "Channel B", "stream_id": "200", "tv_archive": "0", "extra": "ignored"}
            ]
        """.trimIndent()

        val result = json.decodeFromString(ListSerializer(LiveStreamDto.serializer()), payload)

        assertEquals(2, result.size)
        assertEquals(100, result[0].streamId)
        assertEquals(1, result[0].tvArchive)
        assertEquals(2, result[1].num)
        assertEquals(200, result[1].streamId)
        assertEquals(0, result[1].tvArchive)
    }
}
