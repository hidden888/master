package com.iptv.player.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.ErrorView
import com.iptv.player.domain.model.Channel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    BackHandler {
        if (uiState.showChannelList) viewModel.hideChannelList() else onBack()
    }

    // Keep focus on the surface for D-pad zapping when the overlay is closed.
    LaunchedEffect(uiState.showChannelList) {
        if (!uiState.showChannelList) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || uiState.showChannelList) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.ChannelUp -> { viewModel.channelUp(); true }
                    Key.DirectionDown, Key.ChannelDown -> { viewModel.channelDown(); true }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { viewModel.toggleChannelList() },
            ),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    player = viewModel.player
                }
            },
            update = { it.player = viewModel.player },
        )

        when {
            uiState.error != null -> ErrorView(
                message = uiState.error!!,
                onRetry = viewModel::retry,
            )
            uiState.isBuffering -> Text(
                text = "Lädt…",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (uiState.error == null && !uiState.showChannelList) {
            NowNextBar(
                channelNumber = uiState.channelNumber,
                channelName = uiState.channelName,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            )
        }

        if (uiState.showChannelList) {
            ChannelSurfOverlay(
                channels = channels,
                onSelect = viewModel::tuneTo,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun ChannelSurfOverlay(
    channels: List<Channel>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstItemFocus = remember { FocusRequester() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(380.dp)
            .background(Color(0xE6111418))
            .padding(12.dp),
    ) {
        TvLazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(channels) { index, channel ->
                ChannelOverlayRow(
                    channel = channel,
                    onClick = { onSelect(channel.streamId) },
                    modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
                )
            }
        }
    }
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) runCatching { firstItemFocus.requestFocus() }
    }
}

@Composable
private fun ChannelOverlayRow(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = channel.num.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFB8C0CC),
            modifier = Modifier.width(44.dp),
        )
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NowNextBar(
    channelNumber: Int,
    channelName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color(0xCC000000))
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = if (channelNumber > 0) "$channelNumber · $channelName" else channelName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
    }
}
