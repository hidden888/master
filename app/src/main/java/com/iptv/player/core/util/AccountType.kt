package com.iptv.player.core.util

/** How an account's channels are sourced. */
enum class AccountType {
    /** Xtream Codes API (player_api.php). */
    XTREAM,

    /** A plain M3U/M3U8 playlist URL with direct stream links. */
    M3U,
}
