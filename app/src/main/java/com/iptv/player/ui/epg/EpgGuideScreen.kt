package com.iptv.player.ui.epg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.EpgProgram
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MINUTE_WIDTH = 4.dp
private val CHANNEL_COL_WIDTH = 200.dp
private val ROW_HEIGHT = 64.dp
private const val SLOT_MINUTES = 30

@Composable
fun EpgGuideScreen(
    onPlayChannel: (Int) -> Unit,
    viewModel: EpgViewModel = hiltViewModel(),
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    when {
        state is EpgState.Loading && programs.isEmpty() ->
            CenteredMessage("Programmführer wird geladen…")

        state is EpgState.Error && programs.isEmpty() ->
            CenteredMessage(
                message = "EPG konnte nicht geladen werden.\n${(state as EpgState.Error).message}",
                onRetry = viewModel::refresh,
            )

        state is EpgState.Empty ->
            CenteredMessage(
                message = "Dein Anbieter liefert keinen Programmführer (XMLTV).\n" +
                    "Now/Next-Infos in der Senderliste funktionieren trotzdem.",
                onRetry = viewModel::refresh,
            )

        channels.isEmpty() ->
            CenteredMessage("Keine Sender vorhanden.")

        else -> Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            TimelineHeader(
                windowStart = viewModel.windowStart,
                windowEnd = viewModel.windowEnd,
                scroll = scroll,
                timeFormat = timeFormat,
            )
            TvLazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(channels, key = { it.streamId }) { channel ->
                    ChannelRow(
                        channel = channel,
                        programs = channel.epgChannelId?.let { programs[it] }.orEmpty(),
                        windowStart = viewModel.windowStart,
                        windowEnd = viewModel.windowEnd,
                        scroll = scroll,
                        timeFormat = timeFormat,
                        onPlay = { onPlayChannel(channel.streamId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Aktualisieren", onClick = onRetry)
            }
        }
    }
}

@Composable
private fun TimelineHeader(
    windowStart: Long,
    windowEnd: Long,
    scroll: ScrollState,
    timeFormat: SimpleDateFormat,
) {
    val slots = ((windowEnd - windowStart) / (SLOT_MINUTES * 60_000L)).toInt()
    Row(modifier = Modifier.height(28.dp)) {
        Spacer(Modifier.width(CHANNEL_COL_WIDTH))
        Row(modifier = Modifier.horizontalScroll(scroll)) {
            for (i in 0 until slots) {
                val slotStart = windowStart + i * SLOT_MINUTES * 60_000L
                Text(
                    text = timeFormat.format(Date(slotStart)),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB8C0CC),
                    modifier = Modifier.width(MINUTE_WIDTH * SLOT_MINUTES).padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    programs: List<EpgProgram>,
    windowStart: Long,
    windowEnd: Long,
    scroll: ScrollState,
    timeFormat: SimpleDateFormat,
    onPlay: () -> Unit,
) {
    Row(modifier = Modifier.height(ROW_HEIGHT)) {
        Box(
            modifier = Modifier
                .width(CHANNEL_COL_WIDTH)
                .height(ROW_HEIGHT)
                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "${channel.num}  ${channel.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(modifier = Modifier.horizontalScroll(scroll)) {
            var cursor = windowStart
            programs.forEach { program ->
                val s = program.startUtc.coerceIn(windowStart, windowEnd)
                val e = program.endUtc.coerceIn(windowStart, windowEnd)
                if (e <= s) return@forEach
                if (s > cursor) {
                    Spacer(Modifier.width(widthFor(s - cursor)))
                }
                ProgramCell(
                    title = program.title,
                    time = timeFormat.format(Date(program.startUtc)),
                    width = widthFor(e - s),
                    onClick = onPlay,
                )
                cursor = e
            }
        }
    }
}

@Composable
private fun ProgramCell(
    title: String,
    time: String,
    width: Dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isFocused) Color.White else Color(0x22FFFFFF)

    Box(
        modifier = Modifier
            .width(width)
            .height(ROW_HEIGHT)
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB8C0CC),
                maxLines = 1,
            )
        }
    }
}

private fun widthFor(millis: Long): Dp = MINUTE_WIDTH * (millis / 60_000f)
