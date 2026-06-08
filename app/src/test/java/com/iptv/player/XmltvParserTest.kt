package com.iptv.player

import com.iptv.player.data.remote.xmltv.XmltvParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class XmltvParserTest {

    private val parser = XmltvParser()

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC)
        .toInstant().toEpochMilli()

    @Test
    fun `parses UTC time`() {
        assertEquals(utcMillis(2024, 1, 15, 14, 0), parser.parseXmltvTime("20240115140000 +0000"))
    }

    @Test
    fun `parses time without offset as UTC`() {
        assertEquals(utcMillis(2024, 1, 15, 14, 0), parser.parseXmltvTime("20240115140000"))
    }

    @Test
    fun `applies positive offset to reach UTC`() {
        // 16:00 at +0200 is 14:00 UTC
        assertEquals(utcMillis(2024, 1, 15, 14, 0), parser.parseXmltvTime("20240115160000 +0200"))
    }

    @Test
    fun `applies negative offset to reach UTC`() {
        // 09:00 at -0500 is 14:00 UTC
        assertEquals(utcMillis(2024, 1, 15, 14, 0), parser.parseXmltvTime("20240115090000 -0500"))
    }

    @Test
    fun `returns zero for blank or malformed input`() {
        assertEquals(0L, parser.parseXmltvTime(null))
        assertEquals(0L, parser.parseXmltvTime(""))
        assertEquals(0L, parser.parseXmltvTime("2024"))
    }
}
