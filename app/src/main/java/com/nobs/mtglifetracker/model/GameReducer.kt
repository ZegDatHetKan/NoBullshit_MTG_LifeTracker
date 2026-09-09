package com.nobs.mtglifetracker.model

/**
 * Every change to a game goes through here. The functions are pure — state in, state
 * out — which is what makes undo a plain stack of previous states and makes the rules
 * straightforward to unit test.
 */
object GameReducer {

    /**
     * Taps landing on the same target inside this window are folded into one log entry,
     * so a five-point swing reads as one line instead of five.
     */
    const val MERGE_WINDOW_MS = 2_000L

    val PLAYER_COLORS = listOf(0, 1, 2, 3, 4, 5)

    fun newGame(
        startingLife: Int,
        previous: GameState? = null,
        now: Long = 0L,
    ): GameState {
        // Names and colours are part of the table setup, not the game, so they survive a reset.
        val names = previous?.players?.map { it.name } ?: listOf("Player 1", "Player 2")
        val colors = previous?.players?.map { it.colorIndex } ?: listOf(3, 1)
        return GameState(
            startingLife = startingLife,
            players = listOf(
                PlayerState(id = 0, name = names[0], life = startingLife, colorIndex = colors[0]),
                PlayerState(id = 1, name = names[1], life = startingLife, colorIndex = colors[1]),
            ),
            history = emptyList(),
            startedAt = now,
        )
    }

    fun adjustLife(state: GameState, playerId: Int, delta: Int, now: Long): GameState {
        if (delta == 0) return state
        // Life is allowed to go negative; players routinely track how far below zero they went.
        val updated = state.mapPlayer(playerId) { it.copy(life = it.life + delta) }
        return updated.log(
            playerId = playerId,
            kind = EventKind.LIFE,
            counter = null,
            delta = delta,
            resulting = updated.player(playerId).life,
            now = now,
        )
    }

    fun adjustCounter(
        state: GameState,
        playerId: Int,
        type: CounterType,
        delta: Int,
        now: Long,
    ): GameState {
        if (delta == 0) return state
        val current = state.player(playerId).counter(type)
        // Counters have no meaning below zero, so clamp rather than log a phantom change.
        val next = (current + delta).coerceAtLeast(0)
        if (next == current) return state
        val updated = state.mapPlayer(playerId) {
            it.copy(counters = it.counters + (type to next))
        }
        return updated.log(
            playerId = playerId,
            kind = EventKind.COUNTER,
            counter = type,
            delta = next - current,
            resulting = next,
            now = now,
        )
    }

    fun rename(state: GameState, playerId: Int, name: String): GameState =
        state.mapPlayer(playerId) { it.copy(name = name.trim().ifEmpty { "Player ${playerId + 1}" }) }

    fun setColor(state: GameState, playerId: Int, colorIndex: Int): GameState =
        state.mapPlayer(playerId) { it.copy(colorIndex = colorIndex) }

    private fun GameState.mapPlayer(playerId: Int, block: (PlayerState) -> PlayerState) =
        copy(players = players.map { if (it.id == playerId) block(it) else it })

    private fun GameState.log(
        playerId: Int,
        kind: EventKind,
        counter: CounterType?,
        delta: Int,
        resulting: Int,
        now: Long,
    ): GameState {
        val last = history.lastOrNull()
        val mergeable = last != null &&
            last.playerId == playerId &&
            last.kind == kind &&
            last.counter == counter &&
            now - last.at <= MERGE_WINDOW_MS

        val event = GameEvent(playerId, kind, counter, delta, resulting, now)
        val newHistory = if (mergeable) {
            val merged = last!!.copy(delta = last.delta + delta, resulting = resulting, at = now)
            // A burst that nets out to zero (tap down, tap back up) leaves no trace.
            if (merged.delta == 0) history.dropLast(1) else history.dropLast(1) + merged
        } else {
            history + event
        }
        return copy(history = newHistory)
    }
}
