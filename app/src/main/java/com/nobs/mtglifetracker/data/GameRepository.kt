package com.nobs.mtglifetracker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nobs.mtglifetracker.model.GameState
import com.nobs.mtglifetracker.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by
    preferencesDataStore(name = "mtg_life_tracker")

/**
 * Stores the game locally so a mid-match app kill, rotation, or accidental swipe-away
 * doesn't cost anyone their life total. Nothing leaves the device.
 */
class GameRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val game: Flow<GameState?> = context.dataStore.data.map { prefs ->
        prefs[KEY_GAME]?.let { runCatching { json.decodeFromString<GameState>(it) }.getOrNull() }
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETTINGS]?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() }
            ?: Settings()
    }

    suspend fun saveGame(state: GameState) {
        context.dataStore.edit { it[KEY_GAME] = json.encodeToString(state) }
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { it[KEY_SETTINGS] = json.encodeToString(settings) }
    }

    private companion object {
        val KEY_GAME = stringPreferencesKey("game")
        val KEY_SETTINGS = stringPreferencesKey("settings")
    }
}
