package com.iptv.player.ui.series

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.ErrorView
import com.iptv.player.core.ui.components.LoadingIndicator
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.domain.model.Episode

@Composable
fun SeriesDetailScreen(
    onPlayEpisode: (Int, String) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    when {
        uiState.loading -> LoadingIndicator()
        uiState.error != null && uiState.detail == null ->
            ErrorView(message = uiState.error!!, onRetry = null)
        else -> {
            val detail = uiState.detail
            val season = uiState.selectedSeason
            val episodes = season?.let { detail?.episodesBySeason?.get(it) }.orEmpty()
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = uiState.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                detail?.genre?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C0CC))
                }
                detail?.plot?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD1D5DB),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                PrimaryButton(
                    text = if (uiState.isFavorite) "★  Favorit" else "☆  Favorit",
                    onClick = viewModel::toggleFavorite,
                    modifier = Modifier.padding(top = 12.dp),
                )

                if ((detail?.seasons?.size ?: 0) > 1) {
                    TvLazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        items(detail!!.seasons) { season ->
                            SeasonChip(
                                label = "Staffel $season",
                                selected = season == uiState.selectedSeason,
                                onClick = { viewModel.selectSeason(season) },
                            )
                        }
                    }
                } else {
                    Box(Modifier.padding(top = 8.dp))
                }

                TvLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    items(episodes, key = { it.id }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            onClick = { onPlayEpisode(episode.id, episode.containerExtension ?: "mp4") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = when {
        isFocused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color(0x22FFFFFF)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = "${episode.episodeNum}.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8C0CC),
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = episode.title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
