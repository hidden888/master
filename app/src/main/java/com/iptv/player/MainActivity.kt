package com.iptv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.player.core.ui.theme.IptvTheme
import com.iptv.player.ui.AppThemeViewModel
import com.iptv.player.ui.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val accent by themeViewModel.accentArgb.collectAsStateWithLifecycle()
            IptvTheme(accent = Color(accent)) {
                AppNavHost()
            }
        }
    }
}
