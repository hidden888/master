package com.iptv.player.ui.vod

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptv.player.core.ui.components.ErrorView
import com.iptv.player.core.ui.components.LoadingIndicator
import com.iptv.player.core.ui.components.PrimaryButton

@Composable
fun VodDetailScreen(
    onPlay: (Int, String) -> Unit,
    onBack: () -> Unit,
    viewModel: VodDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    when {
        uiState.loading -> LoadingIndicator()
        uiState.error != null && uiState.detail == null ->
            ErrorView(message = uiState.error!!, onRetry = null)
        else -> {
            LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(390.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (!uiState.cover.isNullOrBlank()) {
                        AsyncImage(
                            model = uiState.cover,
                            contentDescription = uiState.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    MetaLine(uiState.detail?.genre, uiState.detail?.duration, uiState.detail?.rating)

                    PrimaryButton(
                        text = "▶  Abspielen",
                        onClick = { onPlay(viewModel.movieId, viewModel.playbackExtension) },
                        modifier = Modifier.focusRequester(playFocus),
                    )

                    uiState.detail?.plot?.takeIf { it.isNotBlank() }?.let { plot ->
                        Text(
                            text = plot,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1D5DB),
                        )
                    }
                    uiState.detail?.cast?.takeIf { it.isNotBlank() }?.let { cast ->
                        Text(
                            text = "Besetzung: $cast",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C0CC),
                        )
                    }
                    uiState.detail?.director?.takeIf { it.isNotBlank() }?.let { director ->
                        Text(
                            text = "Regie: $director",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C0CC),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaLine(genre: String?, duration: String?, rating: String?) {
    val parts = listOfNotNull(
        genre?.takeIf { it.isNotBlank() },
        duration?.takeIf { it.isNotBlank() },
        rating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
    )
    if (parts.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = parts.joinToString("   •   "),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB8C0CC),
        )
        Spacer(Modifier.width(4.dp))
    }
}
