package com.nobs.mtglifetracker.model

import kotlinx.serialization.Serializable

/** Counter types a game of Magic actually asks you to track alongside life. */
@Serializable
enum class CounterType(val label: String, val short: String) {
    POISON("Poison", "PSN"),
    ENERGY("Energy", "NRG"),
    EXPERIENCE("Experience", "EXP"),
    RAD("Rad", "RAD"),
    ;

    /** Ten poison counters lose the game; the others have no intrinsic limit. */
    val losesAt: Int?
        get() = if (this == POISON) POISON_LOSS_THRESHOLD else null

    companion object {
        const val POISON_LOSS_THRESHOLD = 10
    }
}

@Serializable
data class PlayerState(
    val id: Int,
    val name: String,
    val life: Int,
    val colorIndex: Int,
    val counters: Map<CounterType, Int> = emptyMap(),
    val artworkId: String? = null,
    val artworkOpacity: Float = 0.55f,
) {
    fun counter(type: CounterType): Int = counters[type] ?: 0

    /**
     * A player is out at zero life or ten poison. This is shown, not enforced: life can
     * be restored, so the game is never blocked by it.
     */
    val isDefeated: Boolean
        get() = life <= 0 || counter(CounterType.POISON) >= CounterType.POISON_LOSS_THRESHOLD

    val defeatReason: String?
        get() = when {
            counter(CounterType.POISON) >= CounterType.POISON_LOSS_THRESHOLD -> "Poisoned"
            life <= 0 -> "No life"
            else -> null
        }
}

@Serializable
enum class EventKind { LIFE, COUNTER }

/**
 * One line of the game log. Rapid taps on the same target are merged into a single
 * event (see [GameReducer.MERGE_WINDOW_MS]) so the log reads as "Player 1 took 5"
 * rather than five separate entries.
 */
@Serializable
data class GameEvent(
    val playerId: Int,
    val kind: EventKind,
    val counter: CounterType? = null,
    val delta: Int,
    val resulting: Int,
    val at: Long,
)

@Serializable
data class GameState(
    val startingLife: Int = DEFAULT_STARTING_LIFE,
    val players: List<PlayerState>,
    val history: List<GameEvent> = emptyList(),
    val startedAt: Long = 0L,
) {
    fun player(id: Int): PlayerState = players.first { it.id == id }

    companion object {
        const val DEFAULT_STARTING_LIFE = 20
    }
}

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class GameMode(val label: String, val playerCount: Int, val startingLife: Int) {
    DUEL("1v1", 2, 20),
    COMMANDER("Commander · 4", 4, 40),
}

@Serializable
data class Settings(
    val startingLife: Int = GameState.DEFAULT_STARTING_LIFE,
    val keepScreenOn: Boolean = true,
    val haptics: Boolean = true,
    val theme: ThemeMode = ThemeMode.DARK,
    val gameMode: GameMode = GameMode.DUEL,
)
