package com.iptv.player.ui.live

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
import androidx.tv.foundation.lazy.list.items
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

/** A row that reliably reacts to touch taps AND D-pad ENTER, with a focus/selection highlight. */
@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bg = when {
        isFocused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
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
            SelectableRow(
                selected = selectedCategoryId == CATEGORY_ALL,
                onClick = { onSelect(CATEGORY_ALL) },
            ) {
                Text(
                    "Alle Sender",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        items(categories, key = { it.id }) { category ->
            SelectableRow(
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
            ) {
                Text(
                    category.name,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            SelectableRow(
                selected = false,
                onClick = { onPlay(channel.streamId) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.num.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(48.dp),
                    )
                    if (!channel.icon.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
