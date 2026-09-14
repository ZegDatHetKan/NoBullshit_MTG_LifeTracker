package com.nobs.mtglifetracker.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nobs.mtglifetracker.model.CounterType
import com.nobs.mtglifetracker.model.PlayerState

/**
 * The counter editor lives inside the player's own half rather than in a shared sheet:
 * it stays the right way up for whoever opened it, and the opponent's side keeps working.
 */
@Composable
fun CounterPanel(
    player: PlayerState,
    compact: Boolean,
    haptics: Boolean,
    onCounterChange: (CounterType, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(horizontal = if (compact) 6.dp else 12.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "COUNTERS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(start = 6.dp),
            )
            Box(Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Close counters") { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        CounterType.entries.forEach { type ->
            CounterRow(
                type = type,
                value = player.counter(type),
                haptics = haptics,
                onChange = { onCounterChange(type, it) },
                compact = compact,
                modifier = Modifier.heightIn(min = if (compact) 76.dp else 62.dp),
            )
        }
    }
}

@Composable
private fun CounterRow(
    type: CounterType,
    compact: Boolean,
    value: Int,
    haptics: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lethal = type.losesAt?.let { value >= it } == true
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (lethal) Color.Black.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        if (compact) {
            Text(type.label, color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!compact) {
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(type.label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    type.losesAt?.let { Text("$it to lose", fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)) }
                }
            }
            StepButton("−", "Remove one ${type.label} counter", haptics) { onChange(-1) }
            Text(
                text = value.toString(),
                fontSize = if (compact) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = if (compact) Modifier.weight(1f) else Modifier.width(46.dp),
            )
            StepButton("+", "Add one ${type.label} counter", haptics) { onChange(1) }
        }
    }
}

@Composable
private fun StepButton(
    glyph: String,
    description: String,
    haptics: Boolean,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClickLabel = description) {
                if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
