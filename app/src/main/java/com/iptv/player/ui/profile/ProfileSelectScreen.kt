package com.iptv.player.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptv.player.core.ui.components.PrimaryButton
import com.iptv.player.core.ui.components.TvTextField
import com.iptv.player.domain.model.Profile

@Composable
fun ProfileSelectScreen(
    onProfileActive: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val activated by viewModel.activated.collectAsStateWithLifecycle()

    LaunchedEffect(activated) {
        if (activated) onProfileActive()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (state.managing) "Profile verwalten" else "Wer schaut?",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.size(28.dp))
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileTile(
                        profile = profile,
                        managing = state.managing,
                        onClick = { viewModel.onProfileClicked(profile) },
                    )
                }
                item {
                    AddTile(onClick = viewModel::startCreate)
                }
            }
            Spacer(Modifier.size(28.dp))
            PrimaryButton(
                text = if (state.managing) "Fertig" else "Profile verwalten",
                onClick = viewModel::toggleManaging,
            )
        }

        state.pinPromptFor?.let { profile ->
            PinDialog(
                profileName = profile.name,
                error = state.pinError,
                onSubmit = viewModel::submitPin,
                onDismiss = viewModel::dismissPin,
            )
        }

        state.editing?.let { edit ->
            ProfileEditDialog(
                edit = edit,
                onSave = viewModel::saveEdit,
                onDelete = if (edit.id != null) viewModel::deleteEditing else null,
                onDismiss = viewModel::dismissEdit,
            )
        }
    }
}

@Composable
private fun ProfileTile(profile: Profile, managing: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(profile.avatarColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            if (managing) {
                Text(
                    text = "✎",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            if (profile.hasPin) {
                Text("🔒 ", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB8C0CC))
            }
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (profile.isKids) {
            Text("Kinder", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E))
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color(0x33FFFFFF)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(3.dp, borderColor, CircleShape)
                .background(Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = MaterialTheme.typography.displayMedium, color = Color.White)
        }
        Text(
            text = "Neues Profil",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFB8C0CC),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PinDialog(
    profileName: String,
    error: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    DialogScaffold {
        Text("PIN für $profileName", style = MaterialTheme.typography.titleLarge, color = Color.White)
        TvTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
            label = "PIN eingeben",
            isPassword = true,
            keyboardType = KeyboardType.NumberPassword,
            onImeAction = { onSubmit(pin) },
        )
        if (error) {
            Text("Falsche PIN.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "Entsperren", onClick = { onSubmit(pin) })
            PrimaryButton(text = "Abbrechen", onClick = onDismiss)
        }
    }
}

@Composable
private fun ProfileEditDialog(
    edit: ProfileEdit,
    onSave: (String, Long, Boolean, String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(edit.name) }
    var color by remember { mutableStateOf(edit.avatarColor) }
    var isKids by remember { mutableStateOf(edit.isKids) }
    var pin by remember { mutableStateOf("") }

    DialogScaffold {
        Text(
            text = if (edit.id == null) "Neues Profil" else "Profil bearbeiten",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        TvTextField(value = name, onValueChange = { name = it }, label = "Name")

        Text("Farbe", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB8C0CC))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PROFILE_COLORS.forEach { swatch ->
                ColorSwatch(color = swatch, selected = swatch == color, onClick = { color = swatch })
            }
        }

        ToggleRow(label = "Kinderprofil", checked = isKids, onToggle = { isKids = !isKids })

        TvTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
            label = "PIN (Ziffern, leer = keine Sperre)",
            isPassword = true,
            keyboardType = KeyboardType.NumberPassword,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "Speichern", onClick = { onSave(name, color, isKids, pin) })
            if (onDelete != null) {
                PrimaryButton(text = "Löschen", onClick = onDelete)
            }
            PrimaryButton(text = "Abbrechen", onClick = onDismiss)
        }
    }
}

@Composable
private fun ColorSwatch(color: Long, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring = if (isFocused || selected) Color.White else Color.Transparent
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(3.dp, ring, CircleShape)
            .background(Color(color), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color.White else Color.Transparent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DialogScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}
