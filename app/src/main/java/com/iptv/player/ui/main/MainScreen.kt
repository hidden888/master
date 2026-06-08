package com.iptv.player.ui.main

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.ui.epg.EpgGuideScreen
import com.iptv.player.ui.live.LiveScreen
import com.iptv.player.ui.series.SeriesBrowseScreen
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
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.LIVE) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "StreamDeck TV",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 16.dp),
            )
            MainTab.entries.forEach { tab ->
                SidebarItem(
                    label = tab.label,
                    icon = tab.icon,
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (selectedTab) {
                MainTab.LIVE -> LiveScreen(onPlayChannel = onPlayChannel)
                MainTab.MOVIES -> VodBrowseScreen(onOpenMovie = onOpenMovie)
                MainTab.SERIES -> SeriesBrowseScreen(onOpenSeries = onOpenSeries)
                MainTab.GUIDE -> EpgGuideScreen(onPlayChannel = onPlayChannel)
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
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
        )
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
