package com.nobs.mtglifetracker

import com.nobs.mtglifetracker.model.CounterType
import com.nobs.mtglifetracker.model.EventKind
import com.nobs.mtglifetracker.model.GameReducer
import com.nobs.mtglifetracker.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReducerTest {

    private fun game(life: Int = 20) = GameReducer.newGame(life, now = 0L)

    @Test
    fun `new game puts both players on the starting total`() {
        val state = game(40)
        assertEquals(2, state.players.size)
        assertTrue(state.players.all { it.life == 40 })
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun `losing and gaining life moves the total`() {
        var state = game()
        state = GameReducer.adjustLife(state, 0, -3, now = 0L)
        assertEquals(17, state.player(0).life)
        state = GameReducer.adjustLife(state, 0, 5, now = 10_000L)
        assertEquals(22, state.player(0).life)
        assertEquals(20, state.player(1).life)
    }

    @Test
    fun `life is allowed below zero because players track how far down they went`() {
        val state = GameReducer.adjustLife(game(), 0, -25, now = 0L)
        assertEquals(-5, state.player(0).life)
    }

    @Test
    fun `taps inside the merge window collapse into one log entry`() {
        var state = game()
        state = GameReducer.adjustLife(state, 0, -1, now = 1_000L)
        state = GameReducer.adjustLife(state, 0, -1, now = 1_400L)
        state = GameReducer.adjustLife(state, 0, -1, now = 1_900L)

        assertEquals(1, state.history.size)
        assertEquals(-3, state.history.single().delta)
        assertEquals(17, state.history.single().resulting)
        assertEquals(17, state.player(0).life)
    }

    @Test
    fun `taps after the merge window start a new log entry`() {
        var state = game()
        state = GameReducer.adjustLife(state, 0, -1, now = 0L)
        state = GameReducer.adjustLife(state, 0, -1, now = GameReducer.MERGE_WINDOW_MS + 1)
        assertEquals(2, state.history.size)
    }

    @Test
    fun `a burst that nets to zero leaves no trace in the log`() {
        var state = game()
        state = GameReducer.adjustLife(state, 0, -1, now = 100L)
        state = GameReducer.adjustLife(state, 0, 1, now = 200L)
        assertTrue(state.history.isEmpty())
        assertEquals(20, state.player(0).life)
    }

    @Test
    fun `each player accumulates separately`() {
        var state = game()
        state = GameReducer.adjustLife(state, 0, -1, now = 100L)
        state = GameReducer.adjustLife(state, 1, -1, now = 200L)
        assertEquals(2, state.history.size)
        assertEquals(19, state.player(0).life)
        assertEquals(19, state.player(1).life)
    }

    @Test
    fun `counters cannot go negative and log nothing when already at zero`() {
        val state = GameReducer.adjustCounter(game(), 0, CounterType.POISON, -1, now = 0L)
        assertEquals(0, state.player(0).counter(CounterType.POISON))
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun `ten poison counters defeat a player who is still on full life`() {
        var state = game()
        repeat(10) { i ->
            state = GameReducer.adjustCounter(
                state, 0, CounterType.POISON, 1,
                now = i * (GameReducer.MERGE_WINDOW_MS + 1),
            )
        }
        assertEquals(10, state.player(0).counter(CounterType.POISON))
        assertEquals(20, state.player(0).life)
        assertTrue(state.player(0).isDefeated)
        assertEquals("Poisoned", state.player(0).defeatReason)
        assertFalse(state.player(1).isDefeated)
    }

    @Test
    fun `nine poison counters are not yet lethal`() {
        var state = game()
        repeat(9) { i ->
            state = GameReducer.adjustCounter(
                state, 0, CounterType.POISON, 1,
                now = i * (GameReducer.MERGE_WINDOW_MS + 1),
            )
        }
        assertFalse(state.player(0).isDefeated)
    }

    @Test
    fun `reaching zero life defeats a player`() {
        val state = GameReducer.adjustLife(game(), 0, -20, now = 0L)
        assertTrue(state.player(0).isDefeated)
        assertEquals("No life", state.player(0).defeatReason)
    }

    @Test
    fun `other counters are tracked but never lethal`() {
        var state = game()
        state = GameReducer.adjustCounter(state, 0, CounterType.ENERGY, 12, now = 0L)
        assertEquals(12, state.player(0).counter(CounterType.ENERGY))
        assertFalse(state.player(0).isDefeated)
        assertNull(CounterType.ENERGY.losesAt)
    }

    @Test
    fun `life and counter changes never merge with each other`() {
        var state = game()
        state = GameReducer.adjustLife(state, 0, -1, now = 100L)
        state = GameReducer.adjustCounter(state, 0, CounterType.POISON, 1, now = 200L)
        assertEquals(2, state.history.size)
        assertEquals(EventKind.LIFE, state.history[0].kind)
        assertEquals(EventKind.COUNTER, state.history[1].kind)
    }

    @Test
    fun `a new game clears life counters and history but keeps names and colours`() {
        var state = game()
        state = GameReducer.rename(state, 0, "Giulio")
        state = GameReducer.setColor(state, 0, 4)
        state = GameReducer.adjustLife(state, 0, -7, now = 0L)
        state = GameReducer.adjustCounter(state, 0, CounterType.POISON, 3, now = 5_000L)

        val fresh = GameReducer.newGame(state.startingLife, state, now = 9_000L)

        assertEquals(20, fresh.player(0).life)
        assertEquals(0, fresh.player(0).counter(CounterType.POISON))
        assertTrue(fresh.history.isEmpty())
        assertEquals("Giulio", fresh.player(0).name)
        assertEquals(4, fresh.player(0).colorIndex)
    }

    @Test
    fun `an empty name falls back to the default instead of blanking the panel`() {
        val state = GameReducer.rename(game(), 1, "   ")
        assertEquals("Player 2", state.player(1).name)
    }

    @Test
    fun `state is a value so a snapshot taken before a change is a working undo`() {
        val before = GameReducer.adjustLife(game(), 0, -5, now = 0L)
        val after = GameReducer.adjustLife(before, 0, -5, now = 90_000L)
        assertEquals(10, after.player(0).life)
        // The snapshot is untouched by the later change, which is what undo relies on.
        assertEquals(15, before.player(0).life)
        assertEquals(1, before.history.size)
    }
}
