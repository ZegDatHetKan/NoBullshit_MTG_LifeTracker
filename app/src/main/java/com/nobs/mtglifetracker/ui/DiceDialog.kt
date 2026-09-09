package com.nobs.mtglifetracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nobs.mtglifetracker.model.PlayerState
import kotlin.random.Random

/**
 * Deciding who goes first is a shared moment, so the result is drawn twice — once
 * flipped for the player across the table — instead of making one of them lean over.
 */
@Composable
fun DiceDialog(players: List<PlayerState>, onDismiss: () -> Unit) {
    var result by remember { mutableStateOf<String?>(null) }
    var rolls by remember { mutableIntStateOf(0) }
    // Re-animating on every roll is what makes a repeat of the same number feel like a new roll.
    val spin by animateFloatAsState(targetValue = rolls * 360f, label = "spin")

    TrackerDialog(title = "Dice & coin", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ResultFace(text = result, rotated = true, spin = spin)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            )
            ResultFace(text = result, rotated = false, spin = spin)

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { result = "${Random.nextInt(1, 21)}"; rolls++ },
                    modifier = Modifier.weight(1f),
                ) { Text("D20") }
                OutlinedButton(
                    onClick = {
                        result = if (Random.nextBoolean()) "HEADS" else "TAILS"
                        rolls++
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Coin") }
            }
            Button(
                onClick = {
                    result = players.random().name.uppercase()
                    rolls++
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Who plays first") }
        }
    }
}

@Composable
private fun ResultFace(text: String?, rotated: Boolean, spin: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .then(if (rotated) Modifier.rotate(180f) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text ?: "—",
            fontSize = if ((text?.length ?: 1) > 5) 26.sp else 44.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.rotate(spin),
        )
    }
}
