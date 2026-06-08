package com.iptv.player.ui.navigation

import com.iptv.player.core.util.StreamType

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object ProfileSelect : Screen("profiles")
    data object Main : Screen("main")

    data object Player : Screen("player/{type}/{id}/{ext}") {
        const val ARG_TYPE = "type"
        const val ARG_ID = "id"
        const val ARG_EXT = "ext"

        fun create(type: StreamType, id: Int, ext: String = "live") =
            "player/${type.name}/$id/${ext.ifBlank { "live" }}"
    }

    data object VodDetail : Screen("vod/{id}") {
        const val ARG_ID = "id"
        fun create(id: Int) = "vod/$id"
    }

    data object SeriesDetail : Screen("series/{id}") {
        const val ARG_ID = "id"
        fun create(id: Int) = "series/$id"
    }
}
