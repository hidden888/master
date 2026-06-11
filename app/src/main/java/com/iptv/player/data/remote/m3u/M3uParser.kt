package com.iptv.player.data.remote.m3u

import javax.inject.Inject

data class M3uEntry(
    val name: String,
    val logo: String?,
    val tvgId: String?,
    val group: String?,
    val url: String,
)

data class M3uPlaylist(
    /** EPG URL declared in the playlist header (url-tvg / x-tvg-url), if any. */
    val epgUrl: String?,
    val entries: List<M3uEntry>,
)

/** Minimal M3U/M3U8 playlist parser for IPTV playlists (#EXTM3U / #EXTINF + URL lines). */
class M3uParser @Inject constructor() {

    fun parse(text: String): M3uPlaylist {
        var epgUrl: String? = null
        val entries = mutableListOf<M3uEntry>()
        var pending: M3uEntry? = null

        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) ->
                    epgUrl = attr(line, "url-tvg") ?: attr(line, "x-tvg-url")

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val title = line.substringAfter(',', "").trim()
                    pending = M3uEntry(
                        name = title.ifBlank { attr(line, "tvg-name") ?: "Sender" },
                        logo = attr(line, "tvg-logo"),
                        tvgId = attr(line, "tvg-id"),
                        group = attr(line, "group-title"),
                        url = "",
                    )
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) ->
                    pending = pending?.copy(group = line.substringAfter(':').trim())

                line.startsWith("#") -> Unit // ignore other directives

                else -> {
                    val current = pending
                    if (current != null) {
                        entries.add(current.copy(url = line))
                        pending = null
                    } else {
                        entries.add(
                            M3uEntry(name = "Sender ${entries.size + 1}", logo = null, tvgId = null, group = null, url = line),
                        )
                    }
                }
            }
        }
        return M3uPlaylist(epgUrl = epgUrl?.takeIf { it.isNotBlank() }, entries = entries)
    }

    private fun attr(line: String, key: String): String? =
        Regex("$key=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
}
