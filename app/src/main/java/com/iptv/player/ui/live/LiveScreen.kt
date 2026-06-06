package com.iptv.player.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptv.player.core.ui.components.ErrorView
import com.iptv.player.core.ui.components.LoadingIndicator
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.repository.CATEGORY_ALL

@Composable
fun LiveScreen(
    onPlayChannel: (Int) -> Unit,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    when {
        syncState is SyncState.Loading && channels.isEmpty() -> LoadingIndicator()
        syncState is SyncState.Error && channels.isEmpty() ->
            ErrorView(
                message = (syncState as SyncState.Error).message,
                onRetry = viewModel::sync,
            )
        else -> Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CategoryRail(
                categories = categories,
                selectedCategoryId = selectedCategory,
                onSelect = viewModel::selectCategory,
                modifier = Modifier.width(280.dp).fillMaxHeight(),
            )
            ChannelList(
                channels = channels,
                onPlay = onPlayChannel,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<Category>,
    selectedCategoryId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TvLazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            ListItem(
                selected = selectedCategoryId == CATEGORY_ALL,
                onClick = { onSelect(CATEGORY_ALL) },
                headlineContent = { Text("Alle Sender") },
            )
        }
        items(categories, key = { it.id }) { category ->
            ListItem(
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
                headlineContent = {
                    Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<Channel>,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (channels.isEmpty()) {
        Box(modifier) {
            Text(
                text = "Keine Sender gefunden.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }
    TvLazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(channels, key = { it.streamId }) { channel ->
            ListItem(
                selected = false,
                onClick = { onPlay(channel.streamId) },
                leadingContent = {
                    if (!channel.icon.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                },
                overlineContent = { Text(channel.num.toString()) },
                headlineContent = {
                    Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}
