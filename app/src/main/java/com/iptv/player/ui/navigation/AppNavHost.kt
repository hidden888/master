package com.iptv.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iptv.player.core.ui.components.LoadingIndicator
import com.iptv.player.core.util.StreamType
import com.iptv.player.ui.main.MainScreen
import com.iptv.player.ui.onboarding.LoginScreen
import com.iptv.player.ui.player.PlayerScreen
import com.iptv.player.ui.profile.ProfileSelectScreen
import com.iptv.player.ui.series.SeriesDetailScreen
import com.iptv.player.ui.vod.VodDetailScreen

@Composable
fun AppNavHost(startupViewModel: StartupViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val startRoute by startupViewModel.startRoute.collectAsStateWithLifecycle()

    val resolved = startRoute ?: run {
        LoadingIndicator()
        return
    }

    NavHost(navController = navController, startDestination = resolved) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.ProfileSelect.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ProfileSelect.route) {
            ProfileSelectScreen(
                onProfileActive = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.ProfileSelect.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onPlayChannel = { streamId ->
                    navController.navigate(Screen.Player.create(StreamType.LIVE, streamId))
                },
                onOpenMovie = { movieId ->
                    navController.navigate(Screen.VodDetail.create(movieId))
                },
                onOpenSeries = { seriesId ->
                    navController.navigate(Screen.SeriesDetail.create(seriesId))
                },
                onSwitchProfile = {
                    navController.navigate(Screen.ProfileSelect.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onAddAccount = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument(Screen.Player.ARG_TYPE) { type = NavType.StringType },
                navArgument(Screen.Player.ARG_ID) { type = NavType.IntType },
                navArgument(Screen.Player.ARG_EXT) { type = NavType.StringType },
            ),
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.VodDetail.route,
            arguments = listOf(navArgument(Screen.VodDetail.ARG_ID) { type = NavType.IntType }),
        ) {
            VodDetailScreen(
                onPlay = { id, ext ->
                    navController.navigate(Screen.Player.create(StreamType.VOD, id, ext))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(navArgument(Screen.SeriesDetail.ARG_ID) { type = NavType.IntType }),
        ) {
            SeriesDetailScreen(
                onPlayEpisode = { episodeId, ext ->
                    navController.navigate(Screen.Player.create(StreamType.SERIES, episodeId, ext))
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
