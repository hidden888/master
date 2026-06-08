package com.iptv.player.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.model.Series

@Composable
fun FavoritesScreen(
    onPlayChannel: (Int) -> Unit,
    onOpenMovie: (Int) -> Unit,
    onOpenSeries: (Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val series by viewModel.series.collectAsStateWithLifecycle()

    val empty = channels.isEmpty() && movies.isEmpty() && series.isEmpty()
    if (empty) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Noch keine Favoriten.\nMarkiere Sender, Filme oder Serien mit ★.",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFB8C0CC),
            )
        }
        return
    }

    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (channels.isNotEmpty()) {
            item {
                Section(title = "Sender") {
                    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(channels, key = { it.streamId }) { channel ->
                            ChannelTile(channel = channel, onClick = { onPlayChannel(channel.streamId) })
                        }
                    }
                }
            }
        }
        if (movies.isNotEmpty()) {
            item {
                Section(title = "Filme") {
                    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(movies, key = { it.streamId }) { movie ->
                            PosterTile(
                                title = movie.name,
                                cover = movie.cover,
                                onClick = { onOpenMovie(movie.streamId) },
                            )
                        }
                    }
                }
            }
        }
        if (series.isNotEmpty()) {
            item {
                Section(title = "Serien") {
                    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(series, key = { it.seriesId }) { show ->
                            PosterTile(
                                title = show.name,
                                cover = show.cover,
                                onClick = { onOpenSeries(show.seriesId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        content()
    }
}

@Composable
private fun ChannelTile(channel: Channel, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(101.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!channel.icon.isNullOrBlank()) {
                AsyncImage(
                    model = channel.icon,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
            } else {
                Text("${channel.num}", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PosterTile(title: String, cover: String?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
