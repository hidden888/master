package com.iptv.player.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LiveTv
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.iptv.player.ui.live.LiveScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    LIVE("Live-TV", Icons.AutoMirrored.Filled.LiveTv),
    MOVIES("Filme", Icons.Default.Movie),
    SERIES("Serien", Icons.Default.Tv),
    FAVORITES("Favoriten", Icons.Default.Favorite),
    GUIDE("Programm", Icons.Default.GridView),
    SETTINGS("Einstellungen", Icons.Default.Settings),
}

@Composable
fun MainScreen(onPlayChannel: (Int) -> Unit) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.LIVE) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationDrawerItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        leadingContent = {
                            Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(24.dp))
                        },
                    ) {
                        Text(tab.label)
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainTab.LIVE -> LiveScreen(onPlayChannel = onPlayChannel)
                else -> Placeholder(selectedTab.label)
            }
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "$title – kommt bald",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
