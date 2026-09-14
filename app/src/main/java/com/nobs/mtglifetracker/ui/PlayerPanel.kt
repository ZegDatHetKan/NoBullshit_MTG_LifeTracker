package com.nobs.mtglifetracker.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nobs.mtglifetracker.model.CounterType
import com.nobs.mtglifetracker.model.PlayerState
import com.nobs.mtglifetracker.ui.theme.LocalIsDark
import com.nobs.mtglifetracker.ui.theme.paletteFor

/**
 * One player's half of the table. The whole surface is the control: the left half
 * removes a life, the right half adds one, and holding either repeats with acceleration.
 * Everything drawn on top (life total, name, counters) sits in a pass-through overlay so
 * it never steals those taps — only the small chips and buttons claim their own area.
 */
@Composable
fun PlayerPanel(
    player: PlayerState,
    pendingDelta: Int?,
    rotated: Boolean,
    compact: Boolean,
    haptics: Boolean,
    onLifeChange: (Int) -> Unit,
    onCounterChange: (CounterType, Int) -> Unit,
    onOpenPlayerMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCounters by remember(player.id) { mutableStateOf(false) }
    val palette = paletteFor(player.colorIndex)
    val background = if (LocalIsDark.current) palette.dark else palette.light
    // Defeat is signalled by draining the colour out of that half, not by blocking it.
    val defeatFade by animateFloatAsState(
        targetValue = if (player.isDefeated) 0.45f else 1f,
        label = "defeatFade",
    )

    Box(
        modifier
            .clipToBounds()
            .background(background)
            .then(if (rotated) Modifier.rotate(180f) else Modifier),
    ) {
        if (!compact) {
            CardArtworks.find(player.artworkId)?.let { artwork ->
                Image(
                    painter = painterResource(artwork.resource),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = player.artworkOpacity.coerceIn(0.2f, 0.8f) * defeatFade,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
            }
        }
        Box(Modifier.fillMaxSize().alpha(defeatFade)) {
            if (showCounters) {
                CounterPanel(
                    player = player,
                    compact = compact,
                    haptics = haptics,
                    onCounterChange = onCounterChange,
                    onClose = { showCounters = false },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LifeSurface(
                    player = player,
                    compact = compact,
                    pendingDelta = pendingDelta,
                    haptics = haptics,
                    onLifeChange = onLifeChange,
                    onOpenCounters = { showCounters = true },
                    onOpenPlayerMenu = onOpenPlayerMenu,
                )
            }
        }
    }
}

@Composable
private fun LifeSurface(
    player: PlayerState,
    compact: Boolean,
    pendingDelta: Int?,
    haptics: Boolean,
    onLifeChange: (Int) -> Unit,
    onOpenCounters: () -> Unit,
    onOpenPlayerMenu: () -> Unit,
) {
    // Retained so the bubble still has text to show during its fade-out.
    var lastDelta by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingDelta) { if (pendingDelta != null) lastDelta = pendingDelta }

    Box(Modifier.fillMaxSize()) {
        // Tap layer: two full-height halves, so the targets are as large as the screen allows.
        Row(Modifier.fillMaxSize()) {
            LifeTapZone(
                glyph = "−",
                compact = compact,
                alignment = Alignment.CenterStart,
                description = "Lose one life, ${player.name}",
                haptics = haptics,
                onStep = { onLifeChange(-1) },
            )
            LifeTapZone(
                glyph = "+",
                compact = compact,
                alignment = Alignment.CenterEnd,
                description = "Gain one life, ${player.name}",
                haptics = haptics,
                onStep = { onLifeChange(1) },
            )
        }

        // Read-out layer: no pointer modifiers, so taps fall through to the zones below.
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = player.name.uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClickLabel = "Customize ${player.name}") {
                        onOpenPlayerMenu()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                var lifeSize by remember(player.life, compact) {
                    mutableFloatStateOf(if (compact) 68f else 100f)
                }
                Text(
                    text = player.life.toString(),
                    fontSize = lifeSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { if (it.didOverflowWidth && lifeSize > 24f) lifeSize -= 4f },
                    modifier = Modifier.padding(horizontal = if (compact) 16.dp else 56.dp)
                        .semantics { contentDescription = "${player.name}, ${player.life} life" },
                )
                val bubbleAlpha by animateFloatAsState(
                    targetValue = if (pendingDelta != null) 1f else 0f,
                    label = "bubbleAlpha",
                )
                Text(
                    text = lastDelta?.let { if (it > 0) "+$it" else "$it" }.orEmpty(),
                    fontSize = if (compact) 20.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).alpha(bubbleAlpha),
                )
            }

            Spacer(Modifier.weight(1f))

            CounterStrip(
                player = player,
                onOpenCounters = onOpenCounters,
            )
        }

        if (player.isDefeated) {
            Text(
                text = player.defeatReason?.uppercase().orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * Half of the panel. A press fires immediately, then repeats after a short hold and
 * speeds up, so large swings don't mean forty taps.
 *
 * The repeat is driven from inside the gesture rather than by a job launched alongside it.
 * A job on the composition scope outlives a cancelled gesture, and rapid tapping cancels
 * gestures often — one orphan is then stepping the total 26 times a second for the rest of
 * the game, with nothing left holding a handle to stop it.
 */
@Composable
private fun RowScope.LifeTapZone(
    glyph: String,
    compact: Boolean,
    alignment: Alignment,
    description: String,
    haptics: Boolean,
    onStep: () -> Unit,
) {
    val view = LocalView.current
    val currentStep by rememberUpdatedState(onStep)
    val currentHaptics by rememberUpdatedState(haptics)
    var pressed by remember { mutableStateOf(false) }

    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (pressed) Color.White.copy(alpha = 0.09f) else Color.Transparent)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val fire = {
                        if (currentHaptics) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        currentStep()
                    }
                    pressed = true
                    // However this gesture ends — lifted, cancelled, torn down mid-hold —
                    // the repeat ends with it and the highlight always clears.
                    try {
                        fire()
                        var wait = HOLD_BEFORE_REPEAT_MS
                        var interval = REPEAT_START_MS
                        // AwaitPointerEventScope's own withTimeoutOrNull, not the
                        // kotlinx one: it times out by resuming the pending
                        // awaitPointerEvent instead of cancelling a child coroutine
                        // out from under the pointer handler.
                        // Null means the wait ran out with the finger still down, so
                        // repeat. Anything else means the press is over.
                        while (withTimeoutOrNull(wait) { awaitRelease(down.id) } == null) {
                            fire()
                            wait = interval
                            interval = (interval - REPEAT_ACCELERATION_MS)
                                .coerceAtLeast(REPEAT_MIN_MS)
                        }
                    } finally {
                        pressed = false
                    }
                }
            },
        contentAlignment = alignment,
    ) {
        Text(
            text = glyph,
            fontSize = if (compact) 28.sp else 40.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 24.dp),
        )
    }
}

/**
 * Waits for one specific pointer to leave. Following the id matters when a second finger
 * lands in the same half: `waitForUpOrCancellation` ends the press on whichever pointer
 * departs first, which strands the finger that is still holding.
 */
private suspend fun AwaitPointerEventScope.awaitRelease(pointer: PointerId) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
        // Gone from the stream, lifted, or claimed by a parent: the press is over.
        if (change == null || !change.pressed || change.isConsumed) return
    }
}

/** Active counters at a glance, plus the way into the full counter editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CounterStrip(player: PlayerState, onOpenCounters: () -> Unit) {
    val active = CounterType.entries.filter { player.counter(it) > 0 }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        active.forEach { type ->
            val value = player.counter(type)
            val lethal = type.losesAt?.let { value >= it } == true
            Text(
                text = "${type.short} $value",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (lethal) Color.Black.copy(alpha = 0.45f)
                        else Color.White.copy(alpha = 0.16f),
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
        // A word beats a glyph here: no icon reads unambiguously as "counters" at this size.
        Text(
            text = if (active.isEmpty()) "COUNTERS" else "EDIT",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(onClickLabel = "Counters for ${player.name}") { onOpenCounters() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}


private const val HOLD_BEFORE_REPEAT_MS = 420L
private const val REPEAT_START_MS = 130L
private const val REPEAT_ACCELERATION_MS = 7L
private const val REPEAT_MIN_MS = 38L
