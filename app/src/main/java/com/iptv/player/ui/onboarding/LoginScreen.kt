package com.iptv.player.ui.onboarding

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.core.ui.components.TvTextField
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var m3uMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var epgUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Konto hinzufügen",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (m3uMode) "Lade eine M3U-Playlist (URL)" else "Verbinde dich mit deinem Xtream-Codes-Anbieter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeChip("Xtream Codes", selected = !m3uMode) { m3uMode = false; viewModel.resetError() }
                ModeChip("M3U-Playlist", selected = m3uMode) { m3uMode = true; viewModel.resetError() }
            }

            TvTextField(value = name, onValueChange = { name = it }, label = "Anzeigename (optional)")

            if (m3uMode) {
                TvTextField(
                    value = url,
                    onValueChange = { url = it; viewModel.resetError() },
                    label = "Playlist-URL (z. B. http://server/get.php?... oder .m3u8)",
                    keyboardType = KeyboardType.Uri,
                )
                TvTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it; viewModel.resetError() },
                    label = "EPG-URL (optional, XMLTV)",
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                    onImeAction = { viewModel.loginM3u(name, url, epgUrl) },
                )
            } else {
                TvTextField(
                    value = url,
                    onValueChange = { url = it; viewModel.resetError() },
                    label = "Server-URL (z. B. http://server.tld:8080)",
                    keyboardType = KeyboardType.Uri,
                )
                TvTextField(
                    value = username,
                    onValueChange = { username = it; viewModel.resetError() },
                    label = "Benutzername",
                )
                TvTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.resetError() },
                    label = "Passwort",
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = { viewModel.login(name, url, username, password) },
                )
            }

            Text(
                text = "Tipp: Auf dem Handy mit der ✓/Fertig-Taste der Tastatur einloggen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (uiState == LoginUiState.Loading) {
                Text(
                    text = "Verbinde…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            val errorMessage = (uiState as? LoginUiState.Error)?.message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            PrimaryButton(
                text = if (uiState == LoginUiState.Loading) "Verbinde…" else "Verbinden",
                onClick = {
                    if (m3uMode) viewModel.loginM3u(name, url, epgUrl)
                    else viewModel.login(name, url, username, password)
                },
                enabled = uiState != LoginUiState.Loading,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = when {
        isFocused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color(0x14FFFFFF)
    }
    val borderColor = when {
        isFocused -> Color.White
        selected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg, RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
