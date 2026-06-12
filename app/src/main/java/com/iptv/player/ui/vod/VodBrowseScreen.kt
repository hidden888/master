package com.iptv.player.ui.vod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items as gridItems
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptv.player.core.ui.components.ErrorView
import com.iptv.player.core.ui.components.LoadingIndicator
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.CATEGORY_RECENT
import com.iptv.player.ui.live.SyncState

@Composable
fun VodBrowseScreen(
    onOpenMovie: (Int) -> Unit,
    viewModel: VodViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    when {
        syncState is SyncState.Loading && movies.isEmpty() -> LoadingIndicator()
        syncState is SyncState.Error && movies.isEmpty() ->
            ErrorView(message = (syncState as SyncState.Error).message, onRetry = viewModel::sync)
        else -> Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            CategoryRail(
                categories = categories,
                selectedCategoryId = selectedCategory,
                onSelect = viewModel::selectCategory,
                modifier = Modifier.width(260.dp).fillMaxHeight(),
            )
            PosterGrid(
                movies = movies,
                onOpen = onOpenMovie,
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
            CategoryRow("✨  Neu hinzugefügt", selectedCategoryId == CATEGORY_RECENT) { onSelect(CATEGORY_RECENT) }
        }
        item {
            CategoryRow("Alle Filme", selectedCategoryId == CATEGORY_ALL) { onSelect(CATEGORY_ALL) }
        }
        items(categories, key = { it.id }) { category ->
            CategoryRow(category.name, selectedCategoryId == category.id) { onSelect(category.id) }
        }
    }
}

@Composable
private fun CategoryRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = when {
        isFocused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PosterGrid(
    movies: List<Movie>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (movies.isEmpty()) {
        Box(modifier) {
            Text("Keine Filme gefunden.", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }
    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(5),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(movies, key = { it.streamId }) { movie ->
            PosterCard(movie = movie, onClick = { onOpen(movie.streamId) })
        }
    }
}

@Composable
private fun PosterCard(movie: Movie, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Column(
        modifier = Modifier
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
            if (!movie.cover.isNullOrBlank()) {
                AsyncImage(
                    model = movie.cover,
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = movie.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
