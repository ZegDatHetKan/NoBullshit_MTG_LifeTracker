package com.nobs.mtglifetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nobs.mtglifetracker.data.GameRepository
import com.nobs.mtglifetracker.model.CounterType
import com.nobs.mtglifetracker.model.GameReducer
import com.nobs.mtglifetracker.model.GameState
import com.nobs.mtglifetracker.model.GameMode
import com.nobs.mtglifetracker.model.Settings
import com.nobs.mtglifetracker.model.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiState(
    val game: GameState,
    val settings: Settings = Settings(),
    val canUndo: Boolean = false,
    /** Per-player running total of the burst in progress, shown as a floating bubble. */
    val pendingDeltas: Map<Int, Int> = emptyMap(),
    val loaded: Boolean = false,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GameRepository(app)

    private val _state = MutableStateFlow(
        UiState(game = GameReducer.newGame(GameState.DEFAULT_STARTING_LIFE)),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Snapshots taken before each new log entry, so one undo reverses one log line. */
    private val undoStack = ArrayDeque<GameState>()

    private var saveJob: Job? = null
    private val bubbleJobs = mutableMapOf<Int, Job>()

    init {
        viewModelScope.launch {
            val settings = repository.settings.first()
            val saved = repository.game.first()
            _state.value = UiState(
                game = saved ?: GameReducer.newGame(settings.startingLife, now = now(),
                    playerCount = settings.gameMode.playerCount),
                settings = settings,
                loaded = true,
            )
        }
    }

    fun changeLife(playerId: Int, delta: Int) {
        if (delta == 0) return
        mutate { GameReducer.adjustLife(it, playerId, delta, now()) }
        showBubble(playerId, delta)
    }

    fun changeCounter(playerId: Int, type: CounterType, delta: Int) =
        mutate { GameReducer.adjustCounter(it, playerId, type, delta, now()) }

    fun rename(playerId: Int, name: String) = mutate(track = false) {
        GameReducer.rename(it, playerId, name)
    }

    fun setColor(playerId: Int, colorIndex: Int) = mutate(track = false) {
        GameReducer.setColor(it, playerId, colorIndex)
    }

    fun setArtwork(playerId: Int, artworkId: String?, opacity: Float) = mutate(track = false) {
        GameReducer.setArtwork(it, playerId, artworkId, opacity)
    }

    fun setGameMode(mode: GameMode) {
        if (mode.playerCount == _state.value.game.players.size) return
        updateSettings { it.copy(gameMode = mode, startingLife = mode.startingLife) }
        resetGame()
    }

    fun resetGame() {
        val current = _state.value
        undoStack.addLast(current.game)
        trimUndo()
        updateGame(GameReducer.newGame(current.settings.startingLife, current.game, now(),
            playerCount = current.settings.gameMode.playerCount))
        clearBubbles()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        updateSettings { it.copy(
            gameMode = if (previous.players.size == 4) GameMode.COMMANDER else GameMode.DUEL,
            startingLife = previous.startingLife,
        ) }
        updateGame(previous)
        clearBubbles()
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_state.value.settings)
        _state.value = _state.value.copy(settings = next)
        viewModelScope.launch { repository.saveSettings(next) }
    }

    fun setStartingLife(life: Int) {
        updateSettings { it.copy(startingLife = life) }
        resetGame()
    }

    fun setTheme(mode: ThemeMode) = updateSettings { it.copy(theme = mode) }

    /**
     * Applies a reducer. When the change opens a new log entry rather than merging into
     * the one in progress, the pre-change state is pushed for undo — so undo and the
     * history list always agree on what "one change" means.
     */
    private fun mutate(track: Boolean = true, transform: (GameState) -> GameState) {
        val before = _state.value.game
        val after = transform(before)
        if (after == before) return
        if (track) {
            if (after.history.size != before.history.size) {
                undoStack.addLast(before)
                trimUndo()
            }
        }
        updateGame(after)
    }

    private fun updateGame(game: GameState) {
        _state.value = _state.value.copy(game = game, canUndo = undoStack.isNotEmpty())
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            repository.saveGame(game)
        }
    }

    private fun showBubble(playerId: Int, delta: Int) {
        val current = _state.value.pendingDeltas[playerId] ?: 0
        val next = current + delta
        _state.value = _state.value.copy(
            pendingDeltas = _state.value.pendingDeltas + (playerId to next),
        )
        bubbleJobs[playerId]?.cancel()
        bubbleJobs[playerId] = viewModelScope.launch {
            delay(GameReducer.MERGE_WINDOW_MS)
            _state.value = _state.value.copy(
                pendingDeltas = _state.value.pendingDeltas - playerId,
            )
        }
    }

    private fun clearBubbles() {
        bubbleJobs.values.forEach { it.cancel() }
        bubbleJobs.clear()
        _state.value = _state.value.copy(pendingDeltas = emptyMap())
    }

    private fun trimUndo() {
        while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
    }

    private fun now() = System.currentTimeMillis()

    private companion object {
        const val UNDO_LIMIT = 100
        const val SAVE_DEBOUNCE_MS = 300L
    }
}
