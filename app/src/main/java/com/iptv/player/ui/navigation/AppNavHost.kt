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
import com.iptv.player.ui.main.MainScreen
import com.iptv.player.ui.onboarding.LoginScreen
import com.iptv.player.ui.player.PlayerScreen

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
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onPlayChannel = { streamId ->
                    navController.navigate(Screen.Player.create(streamId))
                },
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument(Screen.Player.ARG_STREAM_ID) { type = NavType.IntType }),
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
