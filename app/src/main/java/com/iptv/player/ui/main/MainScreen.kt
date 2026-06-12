package com.iptv.player.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.ui.epg.EpgGuideScreen
import com.iptv.player.ui.favorites.FavoritesScreen
import com.iptv.player.ui.live.LiveScreen
import com.iptv.player.ui.series.SeriesBrowseScreen
import com.iptv.player.ui.settings.SettingsScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.iptv.player.ui.vod.VodBrowseScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    LIVE("Live-TV", Icons.Default.LiveTv),
    MOVIES("Filme", Icons.Default.Movie),
    SERIES("Serien", Icons.Default.Tv),
    FAVORITES("Favoriten", Icons.Default.Favorite),
    GUIDE("Programm", Icons.Default.GridView),
    SETTINGS("Einstellungen", Icons.Default.Settings),
}

@Composable
fun MainScreen(
    onPlayChannel: (Int) -> Unit,
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    onSwitchProfile: () -> Unit,
    onAddAccount: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.LIVE) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val startupChannel by viewModel.startupChannel.collectAsStateWithLifecycle()

    LaunchedEffect(startupChannel) {
        startupChannel?.let {
            viewModel.consumeStartupChannel()
            onPlayChannel(it)
        }
    }

    var clockTick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.showClock) {
        while (settings.showClock) {
            clockTick = System.currentTimeMillis()
            delay(30_000)
        }
    }

    var menuExpanded by remember { mutableStateOf(true) }
    val sidebarWidth by animateDpAsState(if (menuExpanded) 240.dp else 84.dp, label = "sidebar")

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .onFocusChanged { menuExpanded = it.hasFocus }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (menuExpanded) "StreamDeck TV" else "▶",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
            )
            if (settings.showClock && menuExpanded) {
                val timeText = remember(clockTick) {
                    SimpleDateFormat("EEE  HH:mm", Locale.getDefault()).format(Date(clockTick))
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8C0CC),
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                )
            } else {
                Spacer(Modifier.padding(bottom = 8.dp))
            }
            MainTab.entries.forEach { tab ->
                SidebarItem(
                    label = tab.label,
                    icon = tab.icon,
                    selected = selectedTab == tab,
                    expanded = menuExpanded,
                    onClick = { selectedTab = tab },
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (selectedTab) {
                MainTab.LIVE -> LiveScreen(onPlayChannel = onPlayChannel)
                MainTab.MOVIES -> VodBrowseScreen(onOpenMovie = onOpenMovie)
                MainTab.SERIES -> SeriesBrowseScreen(onOpenSeries = onOpenSeries)
                MainTab.FAVORITES -> FavoritesScreen(
                    onPlayChannel = onPlayChannel,
                    onOpenMovie = onOpenMovie,
                    onOpenSeries = onOpenSeries,
                )
                MainTab.GUIDE -> EpgGuideScreen(onPlayChannel = onPlayChannel)
                MainTab.SETTINGS -> SettingsScreen(
                    onSwitchProfile = onSwitchProfile,
                    onAddAccount = onAddAccount,
                )
                else -> Placeholder(selectedTab.label)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val contentColor = when {
        isFocused -> Color.White
        selected -> Color.White
        else -> Color(0xFFB8C0CC)
    }
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        if (expanded) {
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
            )
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
