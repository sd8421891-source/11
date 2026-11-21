package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.BattleScreen
import com.example.ui.GameViewModel
import com.example.ui.LobbyScreen
import com.example.ui.theme.BlazeDropTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            BlazeDropTheme {
                val vm: GameViewModel = viewModel()
                val screen by vm.currentScreen.collectAsStateWithLifecycle()
                val settings by vm.settings.collectAsStateWithLifecycle()
                val world by vm.worldState.collectAsStateWithLifecycle()
                val isPaused by vm.isPaused.collectAsStateWithLifecycle()
                val isScoped by vm.isScoped.collectAsStateWithLifecycle()
                val showExpandedMap by vm.showExpandedMap.collectAsStateWithLifecycle()

                when (screen) {
                    AppScreen.LOBBY -> LobbyScreen(
                        settings = settings,
                        onSelectCharacter = vm::selectCharacter,
                        onSelectMode = vm::selectMode,
                        onStartMatch = vm::startMatch,
                        onToggleSound = vm::setSoundEnabled,
                        onToggleAutoPickup = vm::setAutoPickup,
                        onToggleAimAssist = vm::setAimAssist,
                        onSensitivityChange = vm::setAimSensitivity
                    )
                    AppScreen.BATTLE -> BattleScreen(
                        world = world,
                        isPaused = isPaused,
                        isScoped = isScoped,
                        showExpandedMap = showExpandedMap,
                        onTick = vm::tickFrame,
                        onMoveInput = vm::updateMovement,
                        onAimFireInput = vm::updateAimAndFire,
                        onEjectPlane = vm::ejectFromPlane,
                        onReload = vm::reload,
                        onSwitchWeapon = vm::switchWeapon,
                        onUseMedkit = vm::useMedkit,
                        onDeployGloo = vm::deployGlooWall,
                        onThrowGrenade = vm::throwGrenade,
                        onActivateSkill = vm::activateSkill,
                        onToggleScope = vm::toggleScope,
                        onPickupLoot = vm::pickupNearbyLoot,
                        onTogglePause = { vm.togglePause() },
                        onToggleExpandedMap = { vm.toggleExpandedMap() },
                        onPlayAgain = vm::startMatch,
                        onExitToLobby = vm::exitToLobby
                    )
                }
            }
        }
    }
}
