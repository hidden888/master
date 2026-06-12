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
import androidx.compose.foundation.layout.height
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.ErrorView
import com.iptv.player.core.util.AspectMode
import com.iptv.player.domain.model.Channel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    BackHandler {
        when {
            uiState.showOptions -> viewModel.hideOptions()
            uiState.showChannelList -> viewModel.hideChannelList()
            else -> onBack()
        }
    }

    // Keep focus on the surface for D-pad zapping when no overlay is open.
    LaunchedEffect(uiState.showChannelList, uiState.showOptions) {
        if (!uiState.showChannelList && !uiState.showOptions) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val baseModifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (uiState.showOptions || uiState.showChannelList) return@onPreviewKeyEvent false
            val digit = keyToDigit(event.key)
            when {
                event.key == Key.Menu -> { viewModel.toggleOptions(); true }
                uiState.isLive && digit != null -> { viewModel.onDigit(digit); true }
                uiState.isLive &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter) -> {
                    viewModel.onCenter(); true
                }
                uiState.isLive && (event.key == Key.DirectionUp || event.key == Key.ChannelUp) -> {
                    viewModel.channelUp(); true
                }
                uiState.isLive && (event.key == Key.DirectionDown || event.key == Key.ChannelDown) -> {
                    viewModel.channelDown(); true
                }
                else -> false
            }
        }
    // Live: tap toggles the channel list. VOD/series: rely on the player's own seek controls.
    val rootModifier = if (uiState.isLive) {
        baseModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { viewModel.onCenter() },
        )
    } else {
        baseModifier
    }

    Box(modifier = rootModifier) {
        val resizeMode = when (uiState.aspectMode) {
            AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    useController = !uiState.isLive
                    keepScreenOn = true
                    setResizeMode(resizeMode)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    player = viewModel.player
                }
            },
            update = {
                it.player = viewModel.player
                it.setResizeMode(resizeMode)
            },
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

        if (uiState.error == null && uiState.isLive && uiState.infoVisible &&
            !uiState.showChannelList && uiState.numberInput.isEmpty()
        ) {
            NowNextBar(
                channelNumber = uiState.channelNumber,
                channelName = uiState.title,
                nowTitle = uiState.nowTitle,
                nextTitle = uiState.nextTitle,
                nowStartUtc = uiState.nowStartUtc,
                nowEndUtc = uiState.nowEndUtc,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
            )
        }

        if (uiState.numberInput.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 28.dp, vertical = 16.dp),
            ) {
                Text(
                    text = uiState.numberInput,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
            }
        }

        if (uiState.showChannelList) {
            ChannelSurfOverlay(
                channels = channels,
                onSelect = viewModel::tuneTo,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        if (uiState.error == null && !uiState.showChannelList && !uiState.showOptions) {
            Text(
                text = "MENU = Optionen",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0x99FFFFFF),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }

        if (uiState.showOptions) {
            PlayerOptionsOverlay(
                tracks = tracks,
                isLive = uiState.isLive,
                aspectLabel = uiState.aspectMode.label,
                playbackSpeed = uiState.playbackSpeed,
                onCycleAspect = viewModel::cycleAspect,
                onSetSpeed = viewModel::setSpeed,
                onSelectAudio = viewModel::selectAudio,
                onSelectText = viewModel::selectText,
                onSelectVideo = viewModel::selectVideo,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

@Composable
private fun PlayerOptionsOverlay(
    tracks: PlayerTracks,
    isLive: Boolean,
    aspectLabel: String,
    playbackSpeed: Float,
    onCycleAspect: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSelectAudio: (Int) -> Unit,
    onSelectText: (Int) -> Unit,
    onSelectVideo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(420.dp)
            .background(Color(0xF2111418))
            .padding(16.dp),
    ) {
        TvLazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                OptionsHeader("Bild")
            }
            item {
                OptionRow(label = "Seitenverhältnis: $aspectLabel", selected = false, onClick = onCycleAspect)
            }
            if (!isLive) {
                item { OptionsHeader("Geschwindigkeit") }
                items(SPEED_OPTIONS) { speed ->
                    OptionRow(
                        label = if (speed == 1.0f) "Normal (1.0×)" else "${speed}×",
                        selected = speed == playbackSpeed,
                        onClick = { onSetSpeed(speed) },
                    )
                }
            }
            if (tracks.audio.isNotEmpty()) {
                item { OptionsHeader("Tonspur") }
                items(tracks.audio) { option ->
                    OptionRow(option.label, option.selected) { onSelectAudio(option.key) }
                }
            }
            if (tracks.text.isNotEmpty()) {
                item { OptionsHeader("Untertitel") }
                items(tracks.text) { option ->
                    OptionRow(option.label, option.selected) { onSelectText(option.key) }
                }
            }
            if (tracks.video.isNotEmpty()) {
                item { OptionsHeader("Qualität") }
                items(tracks.video) { option ->
                    OptionRow(option.label, option.selected) { onSelectVideo(option.key) }
                }
            }
        }
    }
}

@Composable
private fun OptionsHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = Color(0xFFB8C0CC),
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp, start = 4.dp),
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) "●" else "○",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF22C55E) else Color(0x66FFFFFF),
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
    nowTitle: String,
    nextTitle: String,
    nowStartUtc: Long,
    nowEndUtc: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xCC000000))
            .padding(horizontal = 32.dp, vertical = 20.dp),
    ) {
        Text(
            text = if (channelNumber > 0) "$channelNumber · $channelName" else channelName,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        if (nowTitle.isNotBlank()) {
            Text(
                text = "Jetzt: $nowTitle",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (nowEndUtc > nowStartUtc) {
                val now = System.currentTimeMillis()
                val fraction = ((now - nowStartUtc).toFloat() / (nowEndUtc - nowStartUtc)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(0.5f)
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
        }
        if (nextTitle.isNotBlank()) {
            Text(
                text = "Danach: $nextTitle",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8C0CC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun keyToDigit(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}
