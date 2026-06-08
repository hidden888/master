package com.iptv.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.domain.model.Account

@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit,
    onAddAccount: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAccountId by viewModel.activeAccountId.collectAsStateWithLifecycle()
    val accountSwitched by viewModel.accountSwitched.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    LaunchedEffect(accountSwitched) {
        if (accountSwitched) onSwitchProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        SectionTitle("Profil")
        PrimaryButton(text = "Profil wechseln", onClick = onSwitchProfile)

        SectionTitle("Inhalte")
        Text(
            text = "Lädt den Programmführer (EPG) neu. Falls dein Anbieter kein XMLTV hat, " +
                "wird automatisch Now/Next pro Sender geholt.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB8C0CC),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "Programmführer aktualisieren", onClick = viewModel::refreshEpg, enabled = !busy)
            PrimaryButton(text = "Sender/Filme/Serien neu laden", onClick = viewModel::reloadCatalogs, enabled = !busy)
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }

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
        modifier = Modifier.padding(top = 8.dp),
    )
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
