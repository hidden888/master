package com.iptv.player.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Main : Screen("main")

    data object Player : Screen("player/{streamId}") {
        fun create(streamId: Int) = "player/$streamId"
        const val ARG_STREAM_ID = "streamId"
    }
}
