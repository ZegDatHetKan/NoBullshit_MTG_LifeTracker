package com.nobs.mtglifetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape

/** Which of the shared, table-level dialogs is open. */
private sealed interface Dialog {
    data object History : Dialog
    data object Dice : Dialog
    data object Settings : Dialog
    data object ConfirmReset : Dialog
    data class PlayerMenu(val playerId: Int) : Dialog
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val ui by viewModel.state.collectAsState()
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    val fourPlayers = ui.game.players.size == 4

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        PlayerRow(
            ids = if (fourPlayers) listOf(2, 3) else listOf(1),
            ui = ui,
            viewModel = viewModel,
            rotated = true,
            compact = fourPlayers,
            onPlayerMenu = { dialog = Dialog.PlayerMenu(it) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        CenterBar(
            canUndo = ui.canUndo,
            onUndo = viewModel::undo,
            onHistory = { dialog = Dialog.History },
            onDice = { dialog = Dialog.Dice },
            onReset = { dialog = Dialog.ConfirmReset },
            onSettings = { dialog = Dialog.Settings },
        )

        PlayerRow(
            ids = if (fourPlayers) listOf(0, 1) else listOf(0),
            ui = ui,
            viewModel = viewModel,
            rotated = false,
            compact = fourPlayers,
            onPlayerMenu = { dialog = Dialog.PlayerMenu(it) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }

    when (val open = dialog) {
        null -> Unit
        Dialog.History -> HistoryDialog(
            game = ui.game,
            onDismiss = { dialog = null },
        )
        Dialog.Dice -> DiceDialog(
            players = ui.game.players,
            onDismiss = { dialog = null },
        )
        Dialog.Settings -> SettingsDialog(
            settings = ui.settings,
            onStartingLife = viewModel::setStartingLife,
            onGameMode = viewModel::setGameMode,
            onTheme = viewModel::setTheme,
            onKeepScreenOn = { on -> viewModel.updateSettings { it.copy(keepScreenOn = on) } },
            onHaptics = { on -> viewModel.updateSettings { it.copy(haptics = on) } },
            onDismiss = { dialog = null },
        )
        Dialog.ConfirmReset -> ConfirmResetDialog(
            startingLife = ui.settings.startingLife,
            playerCount = ui.settings.gameMode.playerCount,
            onConfirm = {
                viewModel.resetGame()
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is Dialog.PlayerMenu -> PlayerMenuDialog(
            player = ui.game.player(open.playerId),
            onRename = { viewModel.rename(open.playerId, it) },
            onColor = { viewModel.setColor(open.playerId, it) },
            allowArtwork = !fourPlayers,
            onArtwork = { artwork, opacity -> viewModel.setArtwork(open.playerId, artwork, opacity) },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun PlayerRow(
    ids: List<Int>,
    ui: UiState,
    viewModel: GameViewModel,
    rotated: Boolean,
    compact: Boolean,
    onPlayerMenu: (Int) -> Unit,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        ids.forEach { id ->
            key(id, compact) {
                PlayerPanel(
                    player = ui.game.player(id),
                    pendingDelta = ui.pendingDeltas[id],
                    rotated = rotated,
                    compact = compact,
                    haptics = ui.settings.haptics,
                    onLifeChange = { viewModel.changeLife(id, it) },
                    onCounterChange = { type, delta -> viewModel.changeCounter(id, type, delta) },
                    onOpenPlayerMenu = { onPlayerMenu(id) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

/**
 * Sits between the two halves so either player can reach it, and stays thin so the life
 * totals keep the screen.
 */
@Composable
private fun CenterBar(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onHistory: () -> Unit,
    onDice: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Icons.AutoMirrored.Filled.Undo, "Undo last change", enabled = canUndo, onClick = onUndo)
            BarButton(Icons.Filled.History, "Game history", onClick = onHistory)
            BarButton(Icons.Filled.Casino, "Roll dice or flip a coin", onClick = onDice)
            BarButton(Icons.Filled.RestartAlt, "New game", onClick = onReset)
            BarButton(Icons.Filled.Settings, "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun BarButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClickLabel = description) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(23.dp),
        )
    }
}
