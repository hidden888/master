package com.iptv.player.ui.live

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.core.ui.components.TvTextField
import com.iptv.player.core.util.AppSettings
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.EpgProgram
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
    val currentPrograms by viewModel.currentPrograms.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val hiddenIds by viewModel.hiddenIds.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var contextChannel by remember { mutableStateOf<Channel?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            syncState is SyncState.Loading && channels.isEmpty() && query.isBlank() -> LoadingIndicator()
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp)) {
                    TvTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        label = "Sender suchen",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    ChannelList(
                        channels = channels,
                        currentByChannel = currentPrograms,
                        favoriteIds = favoriteIds,
                        hiddenIds = hiddenIds,
                        settings = settings,
                        onPlay = onPlayChannel,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onContext = { contextChannel = it },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }

        contextChannel?.let { channel ->
            ChannelContextMenu(
                channel = channel,
                isFavorite = channel.streamId in favoriteIds,
                isHidden = channel.streamId in hiddenIds,
                onPlay = { onPlayChannel(channel.streamId); contextChannel = null },
                onToggleFavorite = { viewModel.toggleFavorite(channel.streamId); contextChannel = null },
                onToggleHidden = {
                    viewModel.setChannelHidden(channel.streamId, channel.streamId !in hiddenIds)
                    contextChannel = null
                },
                onDismiss = { contextChannel = null },
            )
        }
    }
}

/** A row that reacts to touch taps AND D-pad ENTER, with a focus/selection highlight. */
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
            SelectableRow(selected = selectedCategoryId == CATEGORY_ALL, onClick = { onSelect(CATEGORY_ALL) }) {
                Text("Alle Sender", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        items(categories, key = { it.id }) { category ->
            SelectableRow(selected = selectedCategoryId == category.id, onClick = { onSelect(category.id) }) {
                Text(category.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelList(
    channels: List<Channel>,
    currentByChannel: Map<String, EpgProgram>,
    favoriteIds: Set<Int>,
    hiddenIds: Set<Int>,
    settings: AppSettings,
    onPlay: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onContext: (Channel) -> Unit,
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
            val current = channel.epgChannelId?.let { currentByChannel[it] }
            ChannelRow(
                channel = channel,
                current = current,
                isFavorite = channel.streamId in favoriteIds,
                isHidden = channel.streamId in hiddenIds,
                settings = settings,
                onClick = { onPlay(channel.streamId) },
                onLongClick = { onContext(channel) },
                onToggleFavorite = { onToggleFavorite(channel.streamId) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelRow(
    channel: Channel,
    current: EpgProgram?,
    isFavorite: Boolean,
    isHidden: Boolean,
    settings: AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (settings.showChannelNumbers) {
            Text(
                text = channel.num.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(48.dp),
            )
        }
        if (settings.showChannelLogos && !channel.icon.isNullOrBlank()) {
            AsyncImage(
                model = channel.icon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHidden) {
                    Text("🚫 ", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
                }
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (current != null) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB8C0CC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ProgramProgress(program = current, modifier = Modifier.padding(top = 4.dp).fillMaxWidth())
            }
        }
        FavoriteStar(isFavorite = isFavorite, onToggle = onToggleFavorite)
    }
}

/** A focusable star at the trailing edge of a channel row that toggles the favorite state. */
@Composable
private fun FavoriteStar(isFavorite: Boolean, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0x33FFFFFF) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFavorite) "★" else "☆",
            style = MaterialTheme.typography.titleLarge,
            color = if (isFavorite) MaterialTheme.colorScheme.primary else Color(0xFFB8C0CC),
        )
    }
}

@Composable
private fun ChannelContextMenu(
    channel: Channel,
    isFavorite: Boolean,
    isHidden: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(channel.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
            PrimaryButton(text = "Wiedergeben", onClick = onPlay)
            PrimaryButton(
                text = if (isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                onClick = onToggleFavorite,
            )
            PrimaryButton(
                text = if (isHidden) "Sender einblenden" else "Sender ausblenden",
                onClick = onToggleHidden,
            )
            PrimaryButton(text = "Abbrechen", onClick = onDismiss)
        }
    }
}

/** A thin bar showing how far the current program has progressed. */
@Composable
private fun ProgramProgress(program: EpgProgram, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val span = (program.endUtc - program.startUtc).coerceAtLeast(1)
    val fraction = ((now - program.startUtc).toFloat() / span).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x33FFFFFF)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
