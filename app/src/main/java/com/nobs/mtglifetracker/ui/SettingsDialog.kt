package com.nobs.mtglifetracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import com.nobs.mtglifetracker.model.GameMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nobs.mtglifetracker.model.PlayerState
import com.nobs.mtglifetracker.model.Settings
import com.nobs.mtglifetracker.model.ThemeMode
import com.nobs.mtglifetracker.ui.theme.LocalIsDark
import com.nobs.mtglifetracker.ui.theme.PlayerPalettes

/** The three starting totals that cover almost every game, plus room for anything else. */
private val LIFE_PRESETS = listOf(20, 40, 25)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: Settings,
    onGameMode: (GameMode) -> Unit,
    onStartingLife: (Int) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackerDialog(title = "Settings", onDismiss = onDismiss, dismissLabel = "Done") {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            SectionLabel("Game mode")
            Text("Changing mode starts a new game: 20 life in 1v1, 40 in Commander.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameMode.entries.forEach { mode ->
                    ChoiceChip(mode.label, settings.gameMode == mode) { onGameMode(mode) }
                }
            }
            SectionLabel("Starting life", top = 20.dp)
            Text(
                text = "Changing this starts a new game.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LIFE_PRESETS.forEach { life ->
                    ChoiceChip(
                        label = "$life",
                        selected = settings.startingLife == life,
                        onClick = { onStartingLife(life) },
                    )
                }
                CustomLifeChip(
                    current = settings.startingLife,
                    isCustom = settings.startingLife !in LIFE_PRESETS,
                    onSet = onStartingLife,
                )
            }

            SectionLabel("Theme", top = 20.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceChip(
                        label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = settings.theme == mode,
                        onClick = { onTheme(mode) },
                    )
                }
            }

            SectionLabel("Table", top = 20.dp)
            ToggleRow(
                title = "Keep the screen on",
                subtitle = "The display never sleeps mid-game.",
                checked = settings.keepScreenOn,
                onChange = onKeepScreenOn,
            )
            ToggleRow(
                title = "Haptic feedback",
                subtitle = "A short buzz confirms each tap.",
                checked = settings.haptics,
                onChange = onHaptics,
            )

            SectionLabel("About", top = 20.dp)
            Text(
                text = "No ads, no accounts, no analytics. This app requests no Android " +
                    "permissions at all — including internet — so nothing it records can " +
                    "leave your phone.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun ConfirmResetDialog(startingLife: Int, playerCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    TrackerDialog(
        title = "New game",
        onDismiss = onDismiss,
        dismissLabel = "Cancel",
        confirm = { TextButton(onClick = onConfirm) { Text("Start over") } },
    ) {
        Text(
            text = "Reset all $playerCount players to $startingLife life and clear all counters and " +
                "history? Names, colours and backgrounds stay as they are.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun PlayerMenuDialog(
    player: PlayerState,
    onRename: (String) -> Unit,
    onColor: (Int) -> Unit,
    allowArtwork: Boolean,
    onArtwork: (String?, Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(player.id) { mutableStateOf(player.name) }
    var colorIndex by remember(player.id) { mutableIntStateOf(player.colorIndex) }
    var artworkId by remember(player.id) { mutableStateOf(player.artworkId) }
    var opacity by remember(player.id) { mutableFloatStateOf(player.artworkOpacity) }

    TrackerDialog(
        title = "Player",
        onDismiss = onDismiss,
        dismissLabel = "Cancel",
        confirm = {
            TextButton(onClick = {
                onRename(name)
                onColor(colorIndex)
                onArtwork(artworkId, opacity)
                onDismiss()
            }) { Text("Save") }
        },
    ) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(18) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Colour", top = 18.dp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlayerPalettes.forEachIndexed { index, palette ->
                    val color = if (LocalIsDark.current) palette.dark else palette.light
                    val selected = colorIndex == index
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else Color.Transparent,
                                shape = CircleShape,
                            )
                            .semantics { this.selected = selected; contentDescription = palette.name }
                            .clickable(onClickLabel = palette.name) { colorIndex = index },
                    )
                }
            }
            SectionLabel("Background", top = 18.dp)
            if (allowArtwork) {
                ChoiceChip("Solid colour", artworkId == null) { artworkId = null }
                val selectedArt = CardArtworks.find(artworkId)
                if (selectedArt != null) {
                    val palette = PlayerPalettes[colorIndex]
                    Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (LocalIsDark.current) palette.dark else palette.light),
                        contentAlignment = Alignment.Center) {
                        Image(painterResource(selectedArt.resource), null,
                            Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = opacity)
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.22f)))
                        Text(player.life.toString(), style = MaterialTheme.typography.displayLarge,
                            color = Color.White)
                    }
                    Text("Artwork intensity · ${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp))
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.2f..0.8f,
                        modifier = Modifier.semantics { contentDescription = "Artwork intensity" })
                }
                CardArtworks.all.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { art ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .border(if (artworkId == art.id) 2.dp else 0.dp,
                                    if (artworkId == art.id) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(10.dp))
                                .semantics { selected = artworkId == art.id }
                                .clickable(onClickLabel = "Use ${art.name} as background") { artworkId = art.id }
                                .padding(4.dp)) {
                                Image(painterResource(art.resource), art.name,
                                    Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop)
                                Text(art.name, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 4.dp))
                                Text(art.artist, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                Text("Art © Wizards of the Coast · Images via Scryfall",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp))
            } else {
                Text("Card art is available in 1v1. Your choices are kept for when you return.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

    }
}

@Composable
private fun SectionLabel(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(top = top, bottom = 8.dp),
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            )
            .semantics { this.selected = selected }
            .clickable(onClickLabel = label) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CustomLifeChip(current: Int, isCustom: Boolean, onSet: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    ChoiceChip(
        label = if (isCustom) "$current" else "Other",
        selected = isCustom,
        onClick = { editing = true },
    )
    if (editing) {
        var text by remember { mutableStateOf(current.toString()) }
        TrackerDialog(
            title = "Starting life",
            onDismiss = { editing = false },
            dismissLabel = "Cancel",
            confirm = {
                TextButton(
                    onClick = {
                        text.toIntOrNull()?.coerceIn(1, 999)?.let(onSet)
                        editing = false
                    },
                ) { Text("Set") }
            },
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
