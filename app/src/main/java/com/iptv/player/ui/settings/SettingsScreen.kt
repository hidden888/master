package com.iptv.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.core.ui.components.TvTextField
import com.iptv.player.core.util.AspectMode
import com.iptv.player.core.util.BufferProfile
import com.iptv.player.core.util.ChannelSort
import com.iptv.player.core.util.DecoderMode
import com.iptv.player.core.util.StreamFormat
import com.iptv.player.domain.model.Account

@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit,
    onAddAccount: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAccountId by viewModel.activeAccountId.collectAsStateWithLifecycle()
    val accountSwitched by viewModel.accountSwitched.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var epgUrl by remember(settings.epgUrl) { mutableStateOf(settings.epgUrl) }

    LaunchedEffect(accountSwitched) {
        if (accountSwitched) onSwitchProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        // ---- Profil ----
        SectionTitle("Profil")
        PrimaryButton(text = "Profil wechseln", onClick = onSwitchProfile)

        // ---- Player ----
        SectionTitle("Player")
        ChoiceRow(
            label = "Stream-Format (Live)",
            options = StreamFormat.entries,
            selected = settings.streamFormat,
            labelOf = { it.label },
            onSelect = viewModel::setStreamFormat,
        )
        ChoiceRow(
            label = "Seitenverhältnis",
            options = AspectMode.entries,
            selected = settings.aspectMode,
            labelOf = { it.label },
            onSelect = viewModel::setAspectMode,
        )
        ChoiceRow(
            label = "Puffer",
            options = BufferProfile.entries,
            selected = settings.bufferProfile,
            labelOf = { it.label },
            onSelect = viewModel::setBufferProfile,
        )
        ChoiceRow(
            label = "Decoder",
            options = DecoderMode.entries,
            selected = settings.decoderMode,
            labelOf = { it.label },
            onSelect = viewModel::setDecoderMode,
        )
        Text(
            text = "Format/Puffer/Decoder greifen ab der nächsten Wiedergabe. " +
                "Im Player öffnet die MENU-Taste Ton-/Untertitel-/Qualitätsauswahl, Seitenverhältnis und Tempo.",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB8C0CC),
        )

        // ---- Sender ----
        SectionTitle("Sender")
        ChoiceRow(
            label = "Sortierung",
            options = ChannelSort.entries,
            selected = settings.channelSort,
            labelOf = { it.label },
            onSelect = viewModel::setChannelSort,
        )
        ToggleRow("Sendernummern anzeigen", settings.showChannelNumbers, viewModel::setShowChannelNumbers)
        ToggleRow("Senderlogos anzeigen", settings.showChannelLogos, viewModel::setShowChannelLogos)
        ToggleRow("Ausgeblendete Sender anzeigen", settings.showHiddenChannels, viewModel::setShowHiddenChannels)
        Text(
            text = "Sender ausblenden/einblenden: im Live-Tab lange auf einen Sender drücken (Kontextmenü).",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB8C0CC),
        )

        // ---- EPG ----
        SectionTitle("Programmführer (EPG)")
        Text(
            text = "Eigene XMLTV-URL (optional). Leer = Standard des Anbieters. " +
                "Schlägt das XMLTV fehl, holt die App Now/Next je Sender automatisch.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB8C0CC),
        )
        TvTextField(
            value = epgUrl,
            onValueChange = { epgUrl = it },
            label = "EPG-URL (z. B. http://server/xmltv.php?username=…)",
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.setEpgUrl(epgUrl) },
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "EPG-URL speichern & laden", onClick = { viewModel.setEpgUrl(epgUrl) }, enabled = !busy)
            PrimaryButton(text = "Programmführer aktualisieren", onClick = viewModel::refreshEpg, enabled = !busy)
            PrimaryButton(text = "Sender/Filme/Serien neu laden", onClick = viewModel::reloadCatalogs, enabled = !busy)
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }

        // ---- Konten ----
        SectionTitle("Konten")
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    active = account.id == activeAccountId,
                    onClick = { viewModel.switchAccount(account.id) },
                )
            }
            PrimaryButton(
                text = "+  Konto hinzufügen",
                onClick = onAddAccount,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionTitle("Über")
        Text(
            text = "StreamDeck TV · Version 0.1.0",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB8C0CC),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                Chip(
                    text = labelOf(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = { onToggle(!checked) })
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp, 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg, RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AccountRow(account: Account, active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bg = when {
        isFocused -> MaterialTheme.colorScheme.primary
        active -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color(0x14FFFFFF)
    }
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = account.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8C0CC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (active) {
            Text("● Aktiv", style = MaterialTheme.typography.labelLarge, color = Color(0xFF22C55E))
        }
    }
}
