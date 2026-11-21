package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundFxManager
import com.example.data.PlayerPreferencesRepository
import com.example.data.PlayerSettings
import com.example.game.BattleEngine
import com.example.game.BattleWorldState
import com.example.game.CharacterClass
import com.example.game.GameMode
import com.example.game.MatchPhase
import com.example.game.PlayerControlInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LOBBY,
    BATTLE
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PlayerPreferencesRepository(application.applicationContext)
    private val soundFx = SoundFxManager()
    private val engine = BattleEngine(onSoundCue = { soundFx.play(it) })

    val settings: StateFlow<PlayerSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerSettings())

    private val _currentScreen = MutableStateFlow(AppScreen.LOBBY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _worldState = MutableStateFlow(BattleWorldState())
    val worldState: StateFlow<BattleWorldState> = _worldState.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isScoped = MutableStateFlow(false)
    val isScoped: StateFlow<Boolean> = _isScoped.asStateFlow()

    private val _showExpandedMap = MutableStateFlow(false)
    val showExpandedMap: StateFlow<Boolean> = _showExpandedMap.asStateFlow()

    private var controlInput = PlayerControlInput()
    private var matchResultRecorded = false

    init {
        viewModelScope.launch {
            settings.collect { s ->
                soundFx.enabled = s.soundEnabled
                engine.updateSettings(
                    autoPickup = s.autoPickupEnabled,
                    aimAssist = s.aimAssistEnabled,
                    sensitivity = s.aimSensitivity
                )
            }
        }
    }

    fun selectCharacter(character: CharacterClass) {
        viewModelScope.launch { prefsRepo.setSelectedCharacter(character) }
    }

    fun selectMode(mode: GameMode) {
        viewModelScope.launch { prefsRepo.setSelectedMode(mode) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSoundEnabled(enabled) }
    }

    fun setAutoPickup(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoPickup(enabled) }
    }

    fun setAimAssist(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAimAssist(enabled) }
    }

    fun setAimSensitivity(value: Float) {
        viewModelScope.launch { prefsRepo.setAimSensitivity(value) }
    }

    fun startMatch() {
        val s = settings.value
        engine.updateSettings(s.autoPickupEnabled, s.aimAssistEnabled, s.aimSensitivity)
        engine.startNewMatch(s.selectedMode, s.selectedCharacter)
        matchResultRecorded = false
        controlInput = PlayerControlInput()
        _isPaused.value = false
        _isScoped.value = false
        _showExpandedMap.value = false
        _worldState.value = engine.worldState
        _currentScreen.value = AppScreen.BATTLE
    }

    fun exitToLobby() {
        _isPaused.value = false
        _isScoped.value = false
        _showExpandedMap.value = false
        _currentScreen.value = AppScreen.LOBBY
    }

    fun togglePause(paused: Boolean? = null) {
        _isPaused.value = paused ?: !_isPaused.value
    }

    fun toggleScope() {
        _isScoped.value = !_isScoped.value
        controlInput = controlInput.copy(isScoped = _isScoped.value)
    }

    fun toggleExpandedMap(show: Boolean? = null) {
        _showExpandedMap.value = show ?: !_showExpandedMap.value
    }

    fun updateMovement(mx: Float, my: Float, sprinting: Boolean) {
        controlInput = controlInput.copy(moveX = mx, moveY = my, isSprinting = sprinting)
    }

    fun updateAimAndFire(ax: Float, ay: Float, manualAim: Boolean, firing: Boolean) {
        controlInput = controlInput.copy(
            aimX = ax,
            aimY = ay,
            isManualAiming = manualAim,
            isFiring = firing
        )
    }

    fun ejectFromPlane() {
        engine.ejectFromPlane()
        _worldState.value = engine.worldState
    }

    fun reload() {
        engine.triggerReload()
        _worldState.value = engine.worldState
    }

    fun switchWeapon(usePrimary: Boolean) {
        engine.switchWeaponSlot(usePrimary)
        _worldState.value = engine.worldState
    }

    fun useMedkit() {
        engine.useMedkit()
        _worldState.value = engine.worldState
    }

    fun deployGlooWall() {
        engine.deployGlooWall()
        _worldState.value = engine.worldState
    }

    fun throwGrenade() {
        engine.throwGrenade()
        _worldState.value = engine.worldState
    }

    fun activateSkill() {
        engine.activateCharacterSkill()
        _worldState.value = engine.worldState
    }

    fun pickupNearbyLoot() {
        engine.manualPickupNearbyLoot()
        _worldState.value = engine.worldState
    }

    fun tickFrame(dtSec: Float, nowMs: Long) {
        if (_currentScreen.value != AppScreen.BATTLE || _isPaused.value) return
        engine.step(dtSec, controlInput.copy(isScoped = _isScoped.value), nowMs)
        val newState = engine.worldState
        _worldState.value = newState

        if (!matchResultRecorded &&
            (newState.matchPhase == MatchPhase.VICTORY_BOOYAH || newState.matchPhase == MatchPhase.ELIMINATED)
        ) {
            matchResultRecorded = true
            val isBooyah = newState.matchPhase == MatchPhase.VICTORY_BOOYAH
            val kills = newState.player?.kills ?: 0
            val coinsEarned = (if (isBooyah) 150 else 35) + kills * 20
            viewModelScope.launch {
                prefsRepo.recordMatchResult(kills = kills, isBooyah = isBooyah, coinsEarned = coinsEarned)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundFx.release()
    }
}
