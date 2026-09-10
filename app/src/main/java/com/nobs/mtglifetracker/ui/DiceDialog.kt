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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nobs.mtglifetracker.model.PlayerState
import kotlin.random.Random

/**
 * Deciding who goes first is a shared moment, so the result gets the whole dialog at a size
 * that carries across the table rather than a small copy each. A roll that would read as a
 * different, equally legal roll from the far side is marked the way dice are: an underline
 * pinning down which edge is the bottom.
 */
@Composable
fun DiceDialog(players: List<PlayerState>, onDismiss: () -> Unit) {
    var result by remember { mutableStateOf<String?>(null) }
    var rolls by remember { mutableIntStateOf(0) }
    // Re-animating on every roll is what makes a repeat of the same number feel like a new roll.
    val spin by animateFloatAsState(targetValue = rolls * 360f, label = "spin")

    TrackerDialog(title = "Dice & coin", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ResultFace(text = result, spin = spin)

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
private fun ResultFace(text: String?, spin: Float) {
    val shown = text ?: "—"
    // Shrink to fit instead of picking a size from the character count: "TAILS" and a
    // renamed player are arbitrary widths, and any length threshold clips the first
    // result that lands on the wrong side of it.
    var sizeSp by remember(shown) { mutableFloatStateOf(RESULT_MAX_SP) }
    var settled by remember(shown) { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = shown,
            fontSize = sizeSp.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (text != null && readsAsAnotherRoll(text)) {
                TextDecoration.Underline
            } else {
                null
            },
            onTextLayout = { layout ->
                if (!settled) {
                    if (layout.didOverflowWidth && sizeSp > RESULT_MIN_SP) {
                        sizeSp = (sizeSp - RESULT_SHRINK_STEP_SP).coerceAtLeast(RESULT_MIN_SP)
                    } else {
                        settled = true
                    }
                }
            },
            // Held invisible for the frame or two the measuring takes, so a long result
            // never flashes at full size before snapping down.
            modifier = Modifier.alpha(if (settled) 1f else 0f).rotate(spin),
        )
    }
}

/**
 * True when turning the number upside down yields a different number that is also a legal
 * d20 roll — 6 and 9 being the pair that actually collides in that range.
 */
private fun readsAsAnotherRoll(text: String): Boolean {
    if (text.isEmpty() || !text.all { it.isDigit() }) return false
    val upsideDown = text.reversed()
        .map { if (it == '6') '9' else if (it == '9') '6' else it }
        .joinToString("")
    return upsideDown != text && (upsideDown.toIntOrNull() ?: 0) in 1..20
}

/** The result face starts here and steps down until the whole result fits on one line. */
private const val RESULT_MAX_SP = 112f
private const val RESULT_MIN_SP = 22f
private const val RESULT_SHRINK_STEP_SP = 4f
