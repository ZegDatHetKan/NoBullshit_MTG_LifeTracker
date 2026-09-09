package com.nobs.mtglifetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nobs.mtglifetracker.model.EventKind
import com.nobs.mtglifetracker.model.GameEvent
import com.nobs.mtglifetracker.model.GameState
import com.nobs.mtglifetracker.ui.theme.LocalIsDark
import com.nobs.mtglifetracker.ui.theme.paletteFor

@Composable
fun HistoryDialog(game: GameState, onDismiss: () -> Unit) {
    TrackerDialog(title = "Game history", onDismiss = onDismiss) {
        if (game.history.isEmpty()) {
            Text(
                text = "Nothing has happened yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            LazyColumn(
                Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                // Most recent first: the last thing that happened is the thing in dispute.
                reverseLayout = false,
            ) {
                items(game.history.reversed()) { event ->
                    HistoryRow(event = event, game = game)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(event: GameEvent, game: GameState) {
    val player = game.player(event.playerId)
    val palette = paletteFor(player.colorIndex)
    val dot = if (LocalIsDark.current) palette.dark else palette.light

    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            Text(
                text = when (event.kind) {
                    EventKind.LIFE -> "Life ${event.delta.signed()} → ${event.resulting}"
                    EventKind.COUNTER ->
                        "${event.counter?.label} ${event.delta.signed()} → ${event.resulting}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = event.delta.signed(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (event.delta < 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun Int.signed(): String = if (this > 0) "+$this" else toString()
