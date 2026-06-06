package com.iptv.player.core.util

/**
 * Builds Xtream-Codes stream URLs and normalizes the user-entered base URL.
 *
 *  - Live:   {base}/live/{user}/{pass}/{streamId}.{ext}
 *  - VOD:    {base}/movie/{user}/{pass}/{streamId}.{ext}
 *  - Series: {base}/series/{user}/{pass}/{episodeId}.{ext}
 */
object UrlBuilder {

    /** Strips trailing slashes and whitespace; prepends http:// if no scheme is present. */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim().trimEnd('/')
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            url = "http://$url"
        }
        return url
    }

    fun playerApi(base: String): String = "${normalizeBaseUrl(base)}/player_api.php"

    fun liveUrl(base: String, user: String, pass: String, streamId: Int, ext: String = "m3u8"): String =
        "${normalizeBaseUrl(base)}/live/$user/$pass/$streamId.$ext"

    fun vodUrl(base: String, user: String, pass: String, streamId: Int, ext: String): String =
        "${normalizeBaseUrl(base)}/movie/$user/$pass/$streamId.${ext.ifBlank { "mp4" }}"

    fun seriesUrl(base: String, user: String, pass: String, episodeId: Int, ext: String): String =
        "${normalizeBaseUrl(base)}/series/$user/$pass/$episodeId.${ext.ifBlank { "mp4" }}"
}
