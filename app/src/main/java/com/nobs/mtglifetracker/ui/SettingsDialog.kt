package com.nobs.mtglifetracker.ui

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

@Composable
fun SettingsDialog(
    settings: Settings,
    onStartingLife: (Int) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    TrackerDialog(title = "Settings", onDismiss = onDismiss, dismissLabel = "Done") {
        SectionLabel("Starting life")
        Text(
            text = "Changing this starts a new game.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun ConfirmResetDialog(startingLife: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    TrackerDialog(
        title = "New game",
        onDismiss = onDismiss,
        dismissLabel = "Cancel",
        confirm = { TextButton(onClick = onConfirm) { Text("Start over") } },
    ) {
        Text(
            text = "Reset both players to $startingLife life and clear all counters and " +
                "history? Names and colours stay as they are.",
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
    onDismiss: () -> Unit,
) {
    var name by remember(player.id) { mutableStateOf(player.name) }

    TrackerDialog(
        title = "Player",
        onDismiss = onDismiss,
        dismissLabel = "Cancel",
        confirm = {
            TextButton(onClick = { onRename(name); onDismiss() }) { Text("Save") }
        },
    ) {
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
                val selected = player.colorIndex == index
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
                        .clickable(onClickLabel = palette.name) { onColor(index) },
                )
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
