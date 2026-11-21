package com.example

import com.example.game.BattleEngine
import com.example.game.CharacterClass
import com.example.game.GameMode
import com.example.game.MatchPhase
import com.example.game.ObstacleType
import com.example.game.PlayerControlInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleEngineTest {

    @Test
    fun brSoloStartsInAirdropPlaneWith20Survivors() {
        val engine = BattleEngine(onSoundCue = {})
        engine.startNewMatch(GameMode.BR_SOLO, CharacterClass.PULSE)
        val state = engine.worldState
        assertEquals(MatchPhase.AIRDROP_PLANE, state.matchPhase)
        assertEquals(20, state.aliveCount)
        assertNotNull(state.player)
    }

    @Test
    fun ejectAndLandEntersCombatAndAllowsGlooWallDeployment() {
        val engine = BattleEngine(onSoundCue = {})
        engine.startNewMatch(GameMode.CLASH_SQUAD, CharacterClass.AEGIS)
        assertEquals(MatchPhase.COMBAT, engine.worldState.matchPhase)

        val initialGloo = engine.worldState.player!!.glooWalls
        val initialGlooObs = engine.worldState.obstacles.count { it.type == ObstacleType.GLOO_WALL }
        engine.deployGlooWall()

        val after = engine.worldState
        assertEquals(initialGloo - 1, after.player!!.glooWalls)
        assertEquals(initialGlooObs + 1, after.obstacles.count { it.type == ObstacleType.GLOO_WALL })
    }

    @Test
    fun firingConsumesAmmoAndTriggersStepWithoutErrors() {
        val engine = BattleEngine(onSoundCue = {})
        engine.startNewMatch(GameMode.TRAINING, CharacterClass.VIPER)
        val startAmmo = engine.worldState.player!!.activeSlot.ammoInMag

        engine.step(
            dtRaw = 0.016f,
            input = PlayerControlInput(isFiring = true),
            nowMs = 2000L
        )

        assertTrue(engine.worldState.player!!.activeSlot.ammoInMag < startAmmo)
        assertTrue(engine.worldState.projectiles.isNotEmpty())
    }
}
