package com.iptv.player

import com.iptv.player.core.util.UrlBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlBuilderTest {

    @Test
    fun `normalizeBaseUrl strips trailing slash and adds scheme`() {
        assertEquals("http://server.tld:8080", UrlBuilder.normalizeBaseUrl("server.tld:8080/"))
        assertEquals("http://server.tld:8080", UrlBuilder.normalizeBaseUrl("  http://server.tld:8080// "))
        assertEquals("https://x.tld", UrlBuilder.normalizeBaseUrl("https://x.tld"))
    }

    @Test
    fun `playerApi appends endpoint`() {
        assertEquals(
            "http://server.tld:8080/player_api.php",
            UrlBuilder.playerApi("http://server.tld:8080/"),
        )
    }

    @Test
    fun `live url is built with default m3u8 extension`() {
        assertEquals(
            "http://s.tld:80/live/user/pass/1234.m3u8",
            UrlBuilder.liveUrl("http://s.tld:80", "user", "pass", 1234),
        )
    }

    @Test
    fun `vod and series urls use container extension`() {
        assertEquals(
            "http://s.tld/movie/u/p/77.mkv",
            UrlBuilder.vodUrl("http://s.tld", "u", "p", 77, "mkv"),
        )
        assertEquals(
            "http://s.tld/series/u/p/88.mp4",
            UrlBuilder.seriesUrl("http://s.tld", "u", "p", 88, ""),
        )
    }
}
