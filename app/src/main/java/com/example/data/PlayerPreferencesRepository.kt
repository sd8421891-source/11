package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.CharacterClass
import com.example.game.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.blazeDataStore: DataStore<Preferences> by preferencesDataStore(name = "blaze_drop_prefs")

data class PlayerSettings(
    val soundEnabled: Boolean = true,
    val autoPickupEnabled: Boolean = true,
    val aimAssistEnabled: Boolean = true,
    val aimSensitivity: Float = 1.0f,
    val selectedCharacter: CharacterClass = CharacterClass.PULSE,
    val selectedMode: GameMode = GameMode.BR_SOLO,
    val careerKills: Int = 0,
    val careerBooyahs: Int = 0,
    val careerMatches: Int = 0,
    val careerCoins: Int = 500
)

class PlayerPreferencesRepository(private val context: Context) {

    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val AUTO_PICKUP = booleanPreferencesKey("auto_pickup")
        val AIM_ASSIST = booleanPreferencesKey("aim_assist")
        val AIM_SENSITIVITY = floatPreferencesKey("aim_sensitivity")
        val SELECTED_CHARACTER = stringPreferencesKey("selected_character")
        val SELECTED_MODE = stringPreferencesKey("selected_mode")
        val CAREER_KILLS = intPreferencesKey("career_kills")
        val CAREER_BOOYAHS = intPreferencesKey("career_booyahs")
        val CAREER_MATCHES = intPreferencesKey("career_matches")
        val CAREER_COINS = intPreferencesKey("career_coins")
    }

    val settingsFlow: Flow<PlayerSettings> = context.blazeDataStore.data.map { prefs ->
        val charName = prefs[Keys.SELECTED_CHARACTER] ?: CharacterClass.PULSE.name
        val modeName = prefs[Keys.SELECTED_MODE] ?: GameMode.BR_SOLO.name
        PlayerSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            autoPickupEnabled = prefs[Keys.AUTO_PICKUP] ?: true,
            aimAssistEnabled = prefs[Keys.AIM_ASSIST] ?: true,
            aimSensitivity = (prefs[Keys.AIM_SENSITIVITY] ?: 1.0f).coerceIn(0.5f, 1.5f),
            selectedCharacter = CharacterClass.entries.firstOrNull { it.name == charName } ?: CharacterClass.PULSE,
            selectedMode = GameMode.entries.firstOrNull { it.name == modeName } ?: GameMode.BR_SOLO,
            careerKills = prefs[Keys.CAREER_KILLS] ?: 0,
            careerBooyahs = prefs[Keys.CAREER_BOOYAHS] ?: 0,
            careerMatches = prefs[Keys.CAREER_MATCHES] ?: 0,
            careerCoins = prefs[Keys.CAREER_COINS] ?: 500
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.blazeDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setAutoPickup(enabled: Boolean) {
        context.blazeDataStore.edit { it[Keys.AUTO_PICKUP] = enabled }
    }

    suspend fun setAimAssist(enabled: Boolean) {
        context.blazeDataStore.edit { it[Keys.AIM_ASSIST] = enabled }
    }

    suspend fun setAimSensitivity(value: Float) {
        context.blazeDataStore.edit { it[Keys.AIM_SENSITIVITY] = value.coerceIn(0.5f, 1.5f) }
    }

    suspend fun setSelectedCharacter(character: CharacterClass) {
        context.blazeDataStore.edit { it[Keys.SELECTED_CHARACTER] = character.name }
    }

    suspend fun setSelectedMode(mode: GameMode) {
        context.blazeDataStore.edit { it[Keys.SELECTED_MODE] = mode.name }
    }

    suspend fun recordMatchResult(kills: Int, isBooyah: Boolean, coinsEarned: Int) {
        context.blazeDataStore.edit { prefs ->
            prefs[Keys.CAREER_KILLS] = (prefs[Keys.CAREER_KILLS] ?: 0) + kills
            prefs[Keys.CAREER_MATCHES] = (prefs[Keys.CAREER_MATCHES] ?: 0) + 1
            prefs[Keys.CAREER_COINS] = (prefs[Keys.CAREER_COINS] ?: 500) + coinsEarned
            if (isBooyah) {
                prefs[Keys.CAREER_BOOYAHS] = (prefs[Keys.CAREER_BOOYAHS] ?: 0) + 1
            }
        }
    }
}
