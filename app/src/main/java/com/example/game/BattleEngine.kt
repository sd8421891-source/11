package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundCue
import com.example.ui.theme.BlazeAmber
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EpAmber
import com.example.ui.theme.HpGreen
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.ZoneCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class BattleEngine(
    private val onSoundCue: (SoundCue) -> Unit
) {
    private val rng = Random(System.currentTimeMillis())
    private var nextEntityId = 1000
    private var nextGlooId = 50000
    private var autoPickupEnabled = true
    private var aimAssistEnabled = true
    private var aimSensitivity = 1.0f

    private val botNames = listOf(
        "Viper_X", "RAISTAR_AI", "Kobra99", "ShadowOps", "BlazeKing",
        "Nawab_OP", "ZackStorm", "GhostRider", "Sultan_BR", "Phantom77",
        "NovaStrike", "Raptor_FF", "Titan_01", "Ares_Pro", "Kairo_X",
        "Falcon9", "Bison_OP", "Mirage", "StrikerZ"
    )

    var worldState: BattleWorldState = BattleWorldState()
        private set

    fun updateSettings(autoPickup: Boolean, aimAssist: Boolean, sensitivity: Float) {
        autoPickupEnabled = autoPickup
        aimAssistEnabled = aimAssist
        aimSensitivity = sensitivity
    }

    fun startNewMatch(mode: GameMode, character: CharacterClass) {
        nextEntityId = 1000
        nextGlooId = 50000
        val generated = MapGenerator.generate(mode)
        val size = generated.worldSize
        val mid = size / 2f

        val survivors = mutableListOf<Survivor>()

        // Player spawn
        val playerStartX = when (mode) {
            GameMode.BR_SOLO -> 260f
            GameMode.CLASH_SQUAD -> mid
            GameMode.TRAINING -> mid
        }
        val playerStartY = when (mode) {
            GameMode.BR_SOLO -> mid
            GameMode.CLASH_SQUAD -> size - 190f
            GameMode.TRAINING -> size - 260f
        }
        val starterPrimary = when (mode) {
            GameMode.BR_SOLO -> null
            GameMode.CLASH_SQUAD -> WeaponSlotState(WeaponType.M4A1, WeaponType.M4A1.magSize, WeaponType.M4A1.defaultReserve)
            GameMode.TRAINING -> WeaponSlotState(WeaponType.M4A1, WeaponType.M4A1.magSize, WeaponType.M4A1.defaultReserve)
        }
        survivors += Survivor(
            id = 1,
            name = "STRIKER-01",
            isPlayer = true,
            characterClass = character,
            x = playerStartX,
            y = playerStartY,
            aimAngle = -PI.toFloat() / 2f,
            primarySlot = starterPrimary,
            usePrimary = starterPrimary != null,
            armorLevel = if (mode == GameMode.CLASH_SQUAD) 2 else 0,
            armorHp = if (mode == GameMode.CLASH_SQUAD) 120f else 0f,
            maxArmorHp = if (mode == GameMode.CLASH_SQUAD) 120f else 0f,
            medkits = if (mode == GameMode.BR_SOLO) 2 else 3,
            glooWalls = if (mode == GameMode.BR_SOLO) 2 else 3,
            grenades = 2,
            accentColor = character.accentColor
        )

        // Spawn AI bots
        val botAccentPalette = listOf(CrimsonAlert, Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFFFB923C))
        for (i in 0 until mode.botCount) {
            val botId = 2 + i
            var bx = 220f + rng.nextFloat() * (size - 440f)
            var by = 220f + rng.nextFloat() * (size - 440f)
            if (mode == GameMode.CLASH_SQUAD) {
                bx = 220f + (i + 0.5f) * ((size - 440f) / mode.botCount)
                by = 190f + rng.nextFloat() * 140f
            } else if (mode == GameMode.TRAINING) {
                bx = 240f + (i + 0.5f) * ((size - 480f) / mode.botCount)
                by = 240f + rng.nextFloat() * 260f
            } else {
                // Ensure bots don't spawn inside solid walls
                var tries = 0
                while (collidesObstacle(bx, by, 22f, generated.obstacles) && tries < 15) {
                    bx = 220f + rng.nextFloat() * (size - 440f)
                    by = 220f + rng.nextFloat() * (size - 440f)
                    tries++
                }
            }

            val botClass = CharacterClass.entries[i % CharacterClass.entries.size]
            val botPrimary = when (mode) {
                GameMode.BR_SOLO -> if (rng.nextFloat() < 0.55f) {
                    val w = if (rng.nextBoolean()) WeaponType.MP40 else WeaponType.M4A1
                    WeaponSlotState(w, w.magSize, w.defaultReserve)
                } else null
                GameMode.CLASH_SQUAD -> {
                    val w = listOf(WeaponType.M4A1, WeaponType.AK47, WeaponType.MP40)[i % 3]
                    WeaponSlotState(w, w.magSize, w.defaultReserve)
                }
                GameMode.TRAINING -> WeaponSlotState(WeaponType.MP40, WeaponType.MP40.magSize, 120)
            }
            survivors += Survivor(
                id = botId,
                name = botNames[i % botNames.size],
                isPlayer = false,
                characterClass = botClass,
                x = bx,
                y = by,
                aimAngle = rng.nextFloat() * 2f * PI.toFloat(),
                primarySlot = botPrimary,
                usePrimary = botPrimary != null,
                armorLevel = if (mode == GameMode.CLASH_SQUAD) 1 else 0,
                armorHp = if (mode == GameMode.CLASH_SQUAD) 70f else 0f,
                maxArmorHp = if (mode == GameMode.CLASH_SQUAD) 70f else 0f,
                glooWalls = if (mode == GameMode.CLASH_SQUAD) 2 else 1,
                medkits = 1,
                accentColor = botAccentPalette[i % botAccentPalette.size]
            )
        }

        val initialPhase = if (mode == GameMode.BR_SOLO) MatchPhase.AIRDROP_PLANE else MatchPhase.COMBAT
        val initialZone = ZoneState(
            centerX = mid,
            centerY = mid,
            currentRadius = if (mode == GameMode.BR_SOLO) size * 0.62f else size * 0.65f,
            targetCenterX = mid + rng.nextFloat() * 260f - 130f,
            targetCenterY = mid + rng.nextFloat() * 260f - 130f,
            targetRadius = if (mode == GameMode.BR_SOLO) size * 0.36f else size * 0.34f,
            phase = 1,
            phaseTimerSec = if (mode == GameMode.BR_SOLO) 20f else 18f,
            isShrinking = false,
            damagePerSec = if (mode == GameMode.TRAINING) 0f else 4f
        )

        worldState = BattleWorldState(
            mode = mode,
            worldSize = size,
            matchPhase = initialPhase,
            planeX = 160f,
            planeY = mid + (rng.nextFloat() * 340f - 170f),
            planeDirX = 1f,
            planeDirY = (rng.nextFloat() - 0.5f) * 0.25f,
            parachuteAltitude = 400f,
            survivors = survivors,
            obstacles = generated.obstacles,
            floors = generated.floors,
            foliage = generated.foliage,
            pois = generated.pois,
            loot = generated.loot,
            zone = initialZone,
            aliveCount = survivors.size,
            totalCount = survivors.size,
            placement = survivors.size,
            announcementBanner = if (mode == GameMode.BR_SOLO) "AIRDROP PLANE INBOUND · TAP JUMP TO DROP" else "FIGHT! ELIMINATE ALL ENEMIES"
        )
    }

    fun ejectFromPlane() {
        val state = worldState
        if (state.matchPhase != MatchPhase.AIRDROP_PLANE) return
        val updated = state.survivors.map { s ->
            if (s.isPlayer) s.copy(x = state.planeX, y = state.planeY) else s
        }
        worldState = state.copy(
            matchPhase = MatchPhase.PARACHUTING,
            parachuteAltitude = 400f,
            survivors = updated,
            announcementBanner = "STEER WITH JOYSTICK TO LAND"
        )
    }

    fun triggerReload() {
        val state = worldState
        val player = state.player ?: return
        if (!player.isAlive || player.isReloading) return
        val slot = player.activeSlot
        if (slot.ammoInMag >= slot.weapon.magSize || slot.reserveAmmo <= 0) return
        onSoundCue(SoundCue.RELOAD)
        updateSurvivor(player.id) {
            it.copy(
                isReloading = true,
                reloadTimer = slot.weapon.reloadSec,
                reloadTotal = slot.weapon.reloadSec,
                isHealing = false
            )
        }
    }

    fun switchWeaponSlot(usePrimary: Boolean) {
        val player = worldState.player ?: return
        if (!player.isAlive) return
        if (usePrimary && player.primarySlot == null) return
        if (player.usePrimary == usePrimary) return
        updateSurvivor(player.id) {
            it.copy(usePrimary = usePrimary, isReloading = false, reloadTimer = 0f)
        }
    }

    fun useMedkit() {
        val player = worldState.player ?: return
        if (!player.isAlive || player.medkits <= 0 || player.hp >= player.maxHp || player.isHealing) return
        updateSurvivor(player.id) {
            it.copy(isHealing = true, healTimer = 1.4f, isReloading = false)
        }
    }

    fun deployGlooWall() {
        val state = worldState
        val player = state.player ?: return
        if (!player.isAlive || state.matchPhase != MatchPhase.COMBAT || player.glooWalls <= 0) return

        val dist = 68f
        val gx = (player.x + cos(player.aimAngle) * dist).coerceIn(60f, state.worldSize - 60f)
        val gy = (player.y + sin(player.aimAngle) * dist).coerceIn(60f, state.worldSize - 60f)
        val isHorizontal = abs(cos(player.aimAngle)) < abs(sin(player.aimAngle))
        val w = if (isHorizontal) 96f else 22f
        val h = if (isHorizontal) 22f else 96f

        val gloo = Obstacle(
            id = nextGlooId++,
            x = gx - w / 2f,
            y = gy - h / 2f,
            width = w,
            height = h,
            type = ObstacleType.GLOO_WALL,
            hp = 240f,
            maxHp = 240f
        )
        onSoundCue(SoundCue.GLOO_WALL)
        val newParticles = spawnBurst(gx, gy, ShieldCyan, 12, 160f)
        worldState = state.copy(
            obstacles = state.obstacles + gloo,
            particles = state.particles + newParticles,
            survivors = state.survivors.map {
                if (it.id == player.id) it.copy(glooWalls = it.glooWalls - 1) else it
            }
        )
    }

    fun throwGrenade() {
        val state = worldState
        val player = state.player ?: return
        if (!player.isAlive || state.matchPhase != MatchPhase.COMBAT || player.grenades <= 0) return

        val speed = 460f
        val grenade = ThrownGrenade(
            id = nextEntityId++,
            x = player.x + cos(player.aimAngle) * 24f,
            y = player.y + sin(player.aimAngle) * 24f,
            vx = cos(player.aimAngle) * speed,
            vy = sin(player.aimAngle) * speed,
            fuseLeft = 1.45f,
            ownerId = player.id
        )
        worldState = state.copy(
            grenades = state.grenades + grenade,
            survivors = state.survivors.map {
                if (it.id == player.id) it.copy(grenades = it.grenades - 1) else it
            }
        )
    }

    fun activateCharacterSkill() {
        val state = worldState
        val player = state.player ?: return
        if (!player.isAlive || state.matchPhase != MatchPhase.COMBAT) return
        if (player.skillCooldownLeft > 0f) return

        onSoundCue(SoundCue.SKILL)
        val cls = player.characterClass
        var obstacles = state.obstacles
        var survivors = state.survivors
        val particles = state.particles.toMutableList()
        val texts = state.floatingTexts.toMutableList()
        val shockwaves = state.shockwaves.toMutableList()

        if (cls == CharacterClass.VIPER) {
            // Surge dash forward
            val dashDist = 130f
            val nx = (player.x + cos(player.aimAngle) * dashDist).coerceIn(40f, state.worldSize - 40f)
            val ny = (player.y + sin(player.aimAngle) * dashDist).coerceIn(40f, state.worldSize - 40f)
            val finalPos = if (!collidesObstacle(nx, ny, player.radius, obstacles)) nx to ny else player.x to player.y
            particles += spawnBurst(player.x, player.y, BlazeOrange, 16, 220f)
            survivors = survivors.map {
                if (it.id == player.id) {
                    it.copy(
                        x = finalPos.first,
                        y = finalPos.second,
                        skillCooldownLeft = cls.cooldownSec,
                        skillActiveLeft = cls.durationSec
                    )
                } else it
            }
        } else if (cls == CharacterClass.BREACH) {
            // Sonic Wave: destroy gloo walls within 240u, deal 45 dmg to nearby enemies, heal +30
            shockwaves += ShockwaveEffect(player.x, player.y, 240f, ZoneCyan, 0.45f, 0.45f)
            obstacles = obstacles.filterNot {
                it.type == ObstacleType.GLOO_WALL &&
                    hypot((it.x + it.width / 2f) - player.x, (it.y + it.height / 2f) - player.y) <= 250f
            }
            survivors = survivors.map { s ->
                if (s.id == player.id) {
                    s.copy(
                        hp = min(s.maxHp, s.hp + 30f),
                        skillCooldownLeft = cls.cooldownSec,
                        skillActiveLeft = cls.durationSec
                    )
                } else if (s.isAlive && hypot(s.x - player.x, s.y - player.y) <= 240f) {
                    val newHp = s.hp - 45f
                    texts += FloatingText(nextEntityId++, s.x, s.y - 26f, "-45", ZoneCyan, true, 0.85f)
                    s.copy(hp = max(0f, newHp), isAlive = newHp > 0f)
                } else s
            }
            texts += FloatingText(nextEntityId++, player.x, player.y - 30f, "+30 HP", HpGreen, false, 0.85f)
        } else {
            shockwaves += ShockwaveEffect(player.x, player.y, 135f, cls.accentColor, 0.55f, 0.55f)
            survivors = survivors.map {
                if (it.id == player.id) {
                    it.copy(skillCooldownLeft = cls.cooldownSec, skillActiveLeft = cls.durationSec)
                } else it
            }
        }

        worldState = state.copy(
            survivors = survivors,
            obstacles = obstacles,
            particles = particles,
            floatingTexts = texts,
            shockwaves = shockwaves
        )
    }

    fun manualPickupNearbyLoot() {
        val state = worldState
        val player = state.player ?: return
        val item = state.nearbyLoot ?: return
        val (updatedPlayer, consumed) = applyLootPickup(player, item, forceWeaponSwap = true)
        if (consumed) {
            worldState = state.copy(
                survivors = state.survivors.map { if (it.id == player.id) updatedPlayer else it },
                loot = state.loot.filterNot { it.id == item.id },
                floatingTexts = state.floatingTexts + FloatingText(
                    nextEntityId++,
                    player.x,
                    player.y - 28f,
                    "+ ${item.title}",
                    BlazeGold,
                    false,
                    0.9f
                )
            )
        }
    }

    fun step(dtRaw: Float, input: PlayerControlInput, nowMs: Long) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        var state = worldState
        if (state.matchPhase == MatchPhase.VICTORY_BOOYAH || state.matchPhase == MatchPhase.ELIMINATED) {
            // Still animate particles & floating text
            worldState = state.copy(
                particles = advanceParticles(state.particles, dt),
                floatingTexts = advanceFloatingTexts(state.floatingTexts, dt),
                shockwaves = advanceShockwaves(state.shockwaves, dt)
            )
            return
        }

        // Phase 1: Drop Plane
        if (state.matchPhase == MatchPhase.AIRDROP_PLANE) {
            val planeSpeed = 280f
            val nx = state.planeX + state.planeDirX * planeSpeed * dt
            val ny = (state.planeY + state.planeDirY * planeSpeed * dt).coerceIn(220f, state.worldSize - 220f)
            val updatedSurvivors = state.survivors.map { s ->
                if (s.isPlayer) s.copy(x = nx, y = ny) else s
            }
            if (nx >= state.worldSize - 240f) {
                worldState = state.copy(
                    matchPhase = MatchPhase.PARACHUTING,
                    planeX = nx,
                    planeY = ny,
                    parachuteAltitude = 400f,
                    survivors = updatedSurvivors,
                    announcementBanner = "AUTO-EJECTED! STEER TO LAND"
                )
            } else {
                worldState = state.copy(
                    matchTimeSec = state.matchTimeSec + dt,
                    planeX = nx,
                    planeY = ny,
                    survivors = updatedSurvivors
                )
            }
            return
        }

        // Phase 2: Parachuting descent
        if (state.matchPhase == MatchPhase.PARACHUTING) {
            val newAlt = (state.parachuteAltitude - 155f * dt).coerceAtLeast(0f)
            val glideSpeed = 250f
            val updated = state.survivors.map { s ->
                if (s.isPlayer) {
                    val px = (s.x + input.moveX * glideSpeed * dt).coerceIn(80f, state.worldSize - 80f)
                    val py = (s.y + input.moveY * glideSpeed * dt).coerceIn(80f, state.worldSize - 80f)
                    s.copy(x = px, y = py)
                } else s
            }
            if (newAlt <= 0f) {
                // Ensure player doesn't land inside a wall
                val player = updated.first { it.isPlayer }
                val safePos = findNearestFreePosition(player.x, player.y, player.radius, state.obstacles, state.worldSize)
                val burst = spawnBurst(safePos.first, safePos.second, Color(0xFFE2E8F0), 16, 150f)
                worldState = state.copy(
                    matchPhase = MatchPhase.COMBAT,
                    parachuteAltitude = 0f,
                    survivors = updated.map { if (it.isPlayer) it.copy(x = safePos.first, y = safePos.second) else it },
                    particles = state.particles + burst,
                    announcementBanner = "SURVIVE & LOOT · WATCH THE SAFE ZONE"
                )
            } else {
                worldState = state.copy(
                    matchTimeSec = state.matchTimeSec + dt,
                    parachuteAltitude = newAlt,
                    survivors = updated
                )
            }
            return
        }

        // Phase 3: Active Combat Simulation
        val survivors = state.survivors.toMutableList()
        val obstacles = state.obstacles.toMutableList()
        val lootList = state.loot.toMutableList()
        val projectiles = state.projectiles.toMutableList()
        val grenades = state.grenades.toMutableList()
        val floatingTexts = advanceFloatingTexts(state.floatingTexts, dt).toMutableList()
        val particles = advanceParticles(state.particles, dt).toMutableList()
        val shockwaves = advanceShockwaves(state.shockwaves, dt).toMutableList()
        val killFeed = state.killFeed.map { it.copy(ageSec = it.ageSec + dt) }.filter { it.ageSec < 6.5f }.toMutableList()

        // 1. Safe Zone Progression (skip shrinking in Training)
        var zone = state.zone
        var announcement = if (state.matchTimeSec < 4.5f) state.announcementBanner else null
        if (state.mode != GameMode.TRAINING) {
            if (!zone.isShrinking) {
                val nextTimer = zone.phaseTimerSec - dt
                if (nextTimer <= 0f) {
                    zone = zone.copy(isShrinking = true, phaseTimerSec = 14f)
                    announcement = "SAFE ZONE ${zone.phase} SHRINKING!"
                } else {
                    zone = zone.copy(phaseTimerSec = nextTimer)
                }
            } else {
                val shrinkStep = dt / 14f
                val newRadius = max(zone.targetRadius, zone.currentRadius - (zone.currentRadius - zone.targetRadius) * (shrinkStep * 2.2f).coerceAtMost(1f))
                val newCx = zone.centerX + (zone.targetCenterX - zone.centerX) * shrinkStep
                val newCy = zone.centerY + (zone.targetCenterY - zone.centerY) * shrinkStep
                val timerLeft = zone.phaseTimerSec - dt
                if (timerLeft <= 0f || abs(newRadius - zone.targetRadius) < 3f) {
                    val nextPhase = zone.phase + 1
                    val nextRad = max(120f, zone.targetRadius * 0.52f)
                    val offsetLimit = zone.targetRadius * 0.3f
                    val nextCx = (zone.targetCenterX + (rng.nextFloat() * 2f - 1f) * offsetLimit).coerceIn(250f, state.worldSize - 250f)
                    val nextCy = (zone.targetCenterY + (rng.nextFloat() * 2f - 1f) * offsetLimit).coerceIn(250f, state.worldSize - 250f)
                    zone = ZoneState(
                        centerX = zone.targetCenterX,
                        centerY = zone.targetCenterY,
                        currentRadius = zone.targetRadius,
                        targetCenterX = nextCx,
                        targetCenterY = nextCy,
                        targetRadius = nextRad,
                        phase = nextPhase,
                        phaseTimerSec = 16f,
                        isShrinking = false,
                        damagePerSec = 4f + nextPhase * 3.5f
                    )
                } else {
                    zone = zone.copy(
                        centerX = newCx,
                        centerY = newCy,
                        currentRadius = newRadius,
                        phaseTimerSec = timerLeft
                    )
                }
            }
        }

        // 2. Airdrop periodic spawn in BR_SOLO
        var airdropCountdown = state.airdropCountdownSec - dt
        if (state.mode == GameMode.BR_SOLO && airdropCountdown <= 0f) {
            airdropCountdown = 34f
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val dist = rng.nextFloat() * (zone.currentRadius * 0.55f)
            val ax = (zone.centerX + cos(angle) * dist).coerceIn(180f, state.worldSize - 180f)
            val ay = (zone.centerY + sin(angle) * dist).coerceIn(180f, state.worldSize - 180f)
            val safeDrop = findNearestFreePosition(ax, ay, 24f, obstacles, state.worldSize)
            lootList += LootItem(
                id = nextEntityId++,
                x = safeDrop.first,
                y = safeDrop.second,
                kind = LootKind.AIRDROP_CRATE,
                weaponType = WeaponType.AWM,
                title = "AIRDROP · AWM + LV.3"
            )
            shockwaves += ShockwaveEffect(safeDrop.first, safeDrop.second, 120f, BlazeGold, 0.8f, 0.8f)
            announcement = "SUPPLY AIRDROP LANDED!"
        }

        // 3. Find Smart Aim Target for player
        val playerIdx = survivors.indexOfFirst { it.isPlayer }
        var lockedTargetId: Int? = null
        if (playerIdx >= 0 && survivors[playerIdx].isAlive) {
            val p = survivors[playerIdx]
            if (aimAssistEnabled) {
                val weaponRange = p.activeSlot.weapon.range * (if (input.isScoped) 1.25f else 1.05f)
                val candidate = survivors
                    .filter { !it.isPlayer && it.isAlive }
                    .map { it to hypot(it.x - p.x, it.y - p.y) }
                    .filter { (enemy, dist) -> dist <= weaponRange && hasLineOfSight(p.x, p.y, enemy.x, enemy.y, obstacles) }
                    .minByOrNull { it.second }
                lockedTargetId = candidate?.first?.id
            }
        }

        // 4. Update all Survivors (Player + AI Bots)
        for (i in survivors.indices) {
            var s = survivors[i]
            if (!s.isAlive) continue

            // Cooldowns & timers
            val cdLeft = max(0f, s.skillCooldownLeft - dt)
            val activeLeft = max(0f, s.skillActiveLeft - dt)
            val flashLeft = max(0f, s.muzzleFlashTimer - dt)
            var hp = s.hp
            var ep = s.ep

            // EP slow conversion to HP (classic Free Fire mechanic!)
            if (hp < s.maxHp && ep > 0f) {
                val conv = min(ep, 4f * dt)
                hp = min(s.maxHp, hp + conv)
                ep -= conv
            }

            // Pulse healing aura skill
            if (s.characterClass == CharacterClass.PULSE && activeLeft > 0f) {
                hp = min(s.maxHp, hp + 14f * dt)
            }

            // Outside safe zone damage
            if (state.mode != GameMode.TRAINING) {
                val distFromZone = hypot(s.x - zone.centerX, s.y - zone.centerY)
                if (distFromZone > zone.currentRadius) {
                    hp -= zone.damagePerSec * dt
                }
            }

            if (hp <= 0f) {
                survivors[i] = s.copy(hp = 0f, isAlive = false)
                killFeed += KillFeedEntry(nextEntityId++, "SAFE ZONE", s.name, "ZONE", false, s.isPlayer)
                particles += spawnBurst(s.x, s.y, ZoneCyan, 14, 140f)
                continue
            }

            // Reloading progress
            var isReloading = s.isReloading
            var reloadTimer = s.reloadTimer
            var primarySlot = s.primarySlot
            var secondarySlot = s.secondarySlot
            if (isReloading) {
                reloadTimer -= dt
                if (reloadTimer <= 0f) {
                    isReloading = false
                    reloadTimer = 0f
                    if (s.usePrimary && primarySlot != null) {
                        val needed = primarySlot.weapon.magSize - primarySlot.ammoInMag
                        val transfer = min(needed, primarySlot.reserveAmmo)
                        primarySlot = primarySlot.copy(
                            ammoInMag = primarySlot.ammoInMag + transfer,
                            reserveAmmo = primarySlot.reserveAmmo - transfer
                        )
                    } else {
                        val needed = secondarySlot.weapon.magSize - secondarySlot.ammoInMag
                        val transfer = min(needed, secondarySlot.reserveAmmo)
                        secondarySlot = secondarySlot.copy(
                            ammoInMag = secondarySlot.ammoInMag + transfer,
                            reserveAmmo = secondarySlot.reserveAmmo - transfer
                        )
                    }
                }
            }

            // Medkit channeling
            var isHealing = s.isHealing
            var healTimer = s.healTimer
            var medkits = s.medkits
            if (isHealing) {
                healTimer -= dt
                if (healTimer <= 0f) {
                    isHealing = false
                    healTimer = 0f
                    if (medkits > 0) {
                        medkits--
                        hp = min(s.maxHp, hp + 80f)
                        if (s.isPlayer) onSoundCue(SoundCue.HEAL)
                        floatingTexts += FloatingText(nextEntityId++, s.x, s.y - 26f, "+80 HP", HpGreen, false, 0.9f)
                    }
                }
            }

            s = s.copy(
                hp = hp,
                ep = ep,
                skillCooldownLeft = cdLeft,
                skillActiveLeft = activeLeft,
                muzzleFlashTimer = flashLeft,
                isReloading = isReloading,
                reloadTimer = reloadTimer,
                primarySlot = primarySlot,
                secondarySlot = secondarySlot,
                isHealing = isHealing,
                healTimer = healTimer,
                medkits = medkits
            )

            // Movement & Firing decisions
            var moveX = 0f
            var moveY = 0f
            var aimAngle = s.aimAngle
            var wantsFire = false
            var speedMult = 1f

            if (s.characterClass == CharacterClass.PULSE && s.skillActiveLeft > 0f) speedMult *= 1.25f
            if (s.isHealing) speedMult *= 0.45f

            if (s.isPlayer) {
                moveX = input.moveX
                moveY = input.moveY
                if (input.isSprinting) speedMult *= 1.22f
                if (input.isScoped) speedMult *= 0.72f

                if (input.isManualAiming && hypot(input.aimX, input.aimY) > 0.15f) {
                    aimAngle = atan2(input.aimY, input.aimX)
                } else if (input.isFiring && lockedTargetId != null) {
                    val target = survivors.firstOrNull { it.id == lockedTargetId && it.isAlive }
                    if (target != null) {
                        aimAngle = atan2(target.y - s.y, target.x - s.x)
                    }
                } else if (hypot(moveX, moveY) > 0.2f) {
                    val targetAngle = atan2(moveY, moveX)
                    aimAngle = lerpAngle(aimAngle, targetAngle, (dt * 10f * aimSensitivity).coerceAtMost(1f))
                }
                wantsFire = input.isFiring
            } else {
                // AI Decision Making
                val aiStep = computeAiAction(s, survivors, obstacles, lootList, zone, state.mode, dt)
                moveX = aiStep.moveX
                moveY = aiStep.moveY
                aimAngle = aiStep.aimAngle
                wantsFire = aiStep.wantsFire
                s = s.copy(
                    aiDecisionTimer = aiStep.nextDecisionTimer,
                    aiMoveX = aiStep.savedMoveX,
                    aiMoveY = aiStep.savedMoveY,
                    aiTargetId = aiStep.targetId,
                    aiStrafeSign = aiStep.strafeSign
                )

                // AI bot tactical heal or gloo wall when hurt
                if (s.hp < 85f && s.medkits > 0 && !s.isHealing && !wantsFire) {
                    s = s.copy(isHealing = true, healTimer = 1.6f)
                } else if (s.hp < 95f && s.glooWalls > 0 && wantsFire && rng.nextFloat() < 0.015f) {
                    val dist = 56f
                    val gx = (s.x + cos(aimAngle) * dist).coerceIn(60f, state.worldSize - 60f)
                    val gy = (s.y + sin(aimAngle) * dist).coerceIn(60f, state.worldSize - 60f)
                    val horiz = abs(cos(aimAngle)) < abs(sin(aimAngle))
                    obstacles += Obstacle(
                        id = nextGlooId++,
                        x = gx - (if (horiz) 42f else 10f),
                        y = gy - (if (horiz) 10f else 42f),
                        width = if (horiz) 84f else 20f,
                        height = if (horiz) 20f else 84f,
                        type = ObstacleType.GLOO_WALL,
                        hp = 200f,
                        maxHp = 200f
                    )
                    s = s.copy(glooWalls = s.glooWalls - 1)
                }
            }

            // Apply movement with smooth sliding collision
            val baseSpeed = if (s.isPlayer) 215f else 172f
            val mag = hypot(moveX, moveY)
            val normMx = if (mag > 1f) moveX / mag else moveX
            val normMy = if (mag > 1f) moveY / mag else moveY
            val resolved = moveWithCollision(
                x = s.x,
                y = s.y,
                dx = normMx * baseSpeed * speedMult * dt,
                dy = normMy * baseSpeed * speedMult * dt,
                radius = s.radius,
                obstacles = obstacles,
                worldSize = state.worldSize
            )
            s = s.copy(x = resolved.first, y = resolved.second, aimAngle = aimAngle)

            // Auto-pickup nearby loot (for player if enabled, or for bots)
            if ((s.isPlayer && autoPickupEnabled) || !s.isPlayer) {
                val pickupIdx = lootList.indexOfFirst { hypot(it.x - s.x, it.y - s.y) <= 38f }
                if (pickupIdx >= 0) {
                    val item = lootList[pickupIdx]
                    val (updatedS, consumed) = applyLootPickup(s, item, forceWeaponSwap = false)
                    if (consumed) {
                        s = updatedS
                        lootList.removeAt(pickupIdx)
                        if (s.isPlayer) {
                            floatingTexts += FloatingText(nextEntityId++, s.x, s.y - 26f, "+ ${item.title}", BlazeGold, false, 0.8f)
                        }
                    }
                }
            }

            // Weapon Firing
            val activeSlot = s.activeSlot
            val weapon = activeSlot.weapon
            val fireRateMult = if (s.characterClass == CharacterClass.VIPER && s.skillActiveLeft > 0f) 0.65f else 1f
            val effectiveInterval = (weapon.fireIntervalMs * fireRateMult).toLong()

            if (wantsFire && !s.isReloading && !s.isHealing && nowMs - s.lastShotMs >= effectiveInterval) {
                if (activeSlot.ammoInMag > 0) {
                    // Fire pellets
                    val scopedSpreadMult = if (s.isPlayer && input.isScoped) 0.35f else 1f
                    repeat(weapon.pellets) {
                        val jitter = (rng.nextFloat() * 2f - 1f) * weapon.spreadRad * scopedSpreadMult
                        val shotAngle = aimAngle + jitter
                        val hsBonus = if (s.isPlayer && input.isScoped) 0.14f else 0f
                        val isHs = rng.nextFloat() < (weapon.baseHeadshotChance + hsBonus)
                        val dmg = weapon.damage * (if (isHs) weapon.headshotMult else 1f)
                        projectiles += Projectile(
                            id = nextEntityId++,
                            x = s.x + cos(aimAngle) * 22f,
                            y = s.y + sin(aimAngle) * 22f,
                            prevX = s.x,
                            prevY = s.y,
                            vx = cos(shotAngle) * weapon.bulletSpeed,
                            vy = sin(shotAngle) * weapon.bulletSpeed,
                            ownerId = s.id,
                            weapon = weapon,
                            damage = dmg,
                            isHeadshot = isHs,
                            rangeRemaining = weapon.range,
                            color = if (isHs) BlazeGold else weapon.rarity.color
                        )
                    }

                    // Consume ammo
                    val newAmmo = activeSlot.ammoInMag - 1
                    val updatedSlot = activeSlot.copy(ammoInMag = newAmmo)
                    s = if (s.usePrimary && s.primarySlot != null) {
                        s.copy(primarySlot = updatedSlot, lastShotMs = nowMs, muzzleFlashTimer = 0.06f)
                    } else {
                        s.copy(secondarySlot = updatedSlot, lastShotMs = nowMs, muzzleFlashTimer = 0.06f)
                    }

                    if (s.isPlayer) {
                        val cue = when (weapon) {
                            WeaponType.MP40 -> SoundCue.SHOT_SMG
                            WeaponType.M1887 -> SoundCue.SHOT_SHOTGUN
                            WeaponType.AWM -> SoundCue.SHOT_SNIPER
                            else -> SoundCue.SHOT_RIFLE
                        }
                        onSoundCue(cue)
                    }

                    // Auto reload on empty
                    if (newAmmo == 0 && updatedSlot.reserveAmmo > 0) {
                        s = s.copy(isReloading = true, reloadTimer = weapon.reloadSec, reloadTotal = weapon.reloadSec)
                        if (s.isPlayer) onSoundCue(SoundCue.RELOAD)
                    }
                } else if (activeSlot.reserveAmmo > 0) {
                    s = s.copy(isReloading = true, reloadTimer = weapon.reloadSec, reloadTotal = weapon.reloadSec)
                    if (s.isPlayer) onSoundCue(SoundCue.RELOAD)
                }
            }

            survivors[i] = s
        }

        // 5. Advance Projectiles & Hit Detection
        val remainingProjectiles = mutableListOf<Projectile>()
        for (proj in projectiles) {
            val stepX = proj.vx * dt
            val stepY = proj.vy * dt
            val nx = proj.x + stepX
            val ny = proj.y + stepY
            val distMoved = hypot(stepX, stepY)
            val rangeLeft = proj.rangeRemaining - distMoved

            if (nx < 0f || nx > state.worldSize || ny < 0f || ny > state.worldSize || rangeLeft <= 0f) {
                continue
            }

            // Check obstacle / gloo wall collision
            val hitObsIdx = obstacles.indexOfFirst { segmentHitsRect(proj.x, proj.y, nx, ny, it) }
            if (hitObsIdx >= 0) {
                val obs = obstacles[hitObsIdx]
                particles += spawnBurst(nx, ny, if (obs.type == ObstacleType.GLOO_WALL) ShieldCyan else Color(0xFFE2E8F0), 5, 110f)
                if (obs.type == ObstacleType.GLOO_WALL) {
                    val remHp = obs.hp - proj.damage
                    if (remHp <= 0f) {
                        obstacles.removeAt(hitObsIdx)
                        particles += spawnBurst(obs.x + obs.width / 2f, obs.y + obs.height / 2f, ShieldCyan, 16, 200f)
                    } else {
                        obstacles[hitObsIdx] = obs.copy(hp = remHp)
                    }
                }
                continue
            }

            // Check survivor collision
            val hitIdx = survivors.indexOfFirst { target ->
                target.isAlive && target.id != proj.ownerId &&
                    pointToSegmentDist(target.x, target.y, proj.x, proj.y, nx, ny) <= target.radius + 3f
            }

            if (hitIdx >= 0) {
                val victim = survivors[hitIdx]
                // Check Aegis Chrono Dome shield!
                if (victim.characterClass == CharacterClass.AEGIS && victim.skillActiveLeft > 0f) {
                    particles += spawnBurst(nx, ny, ShieldCyan, 6, 130f)
                    floatingTexts += FloatingText(nextEntityId++, victim.x, victim.y - 24f, "SHIELD", ShieldCyan, false, 0.55f)
                    continue
                }

                // Apply armor & helmet mitigation
                var actualDamage = proj.damage
                var armorHp = victim.armorHp
                var armorLevel = victim.armorLevel
                if (proj.isHeadshot && victim.helmetLevel > 0) {
                    actualDamage *= (1f - 0.16f * victim.helmetLevel)
                } else if (!proj.isHeadshot && armorHp > 0f) {
                    val mitigation = 0.14f * armorLevel
                    val absorbed = actualDamage * mitigation
                    actualDamage -= absorbed
                    armorHp = max(0f, armorHp - absorbed * 0.85f)
                    if (armorHp <= 0f) armorLevel = 0
                }

                val dmgInt = max(1, actualDamage.toInt())
                val newHp = max(0f, victim.hp - dmgInt)
                val died = newHp <= 0f

                survivors[hitIdx] = victim.copy(
                    hp = newHp,
                    armorHp = armorHp,
                    armorLevel = armorLevel,
                    isAlive = !died
                )

                // Credit attacker
                val ownerIdx = survivors.indexOfFirst { it.id == proj.ownerId }
                if (ownerIdx >= 0) {
                    val owner = survivors[ownerIdx]
                    survivors[ownerIdx] = owner.copy(
                        damageDealt = owner.damageDealt + dmgInt,
                        kills = owner.kills + if (died) 1 else 0,
                        headshots = owner.headshots + if (died && proj.isHeadshot) 1 else 0
                    )
                    if (owner.isPlayer) {
                        onSoundCue(if (proj.isHeadshot) SoundCue.HEADSHOT else SoundCue.HIT_MARKER)
                    }
                }

                particles += spawnBurst(nx, ny, if (proj.isHeadshot) BlazeGold else CrimsonAlert, 8, 150f)
                floatingTexts += FloatingText(
                    id = nextEntityId++,
                    x = victim.x + rng.nextFloat() * 18f - 9f,
                    y = victim.y - 24f,
                    text = if (proj.isHeadshot) "$dmgInt!" else "$dmgInt",
                    color = if (proj.isHeadshot) CrimsonAlert else Color.White,
                    isCritical = proj.isHeadshot,
                    life = 0.75f
                )

                if (died) {
                    val killerName = survivors.firstOrNull { it.id == proj.ownerId }?.name ?: "UNKNOWN"
                    val involvesPlayer = proj.ownerId == 1 || victim.isPlayer
                    killFeed += KillFeedEntry(
                        id = nextEntityId++,
                        killerName = killerName,
                        victimName = victim.name,
                        weaponTag = proj.weapon.shortTag,
                        isHeadshot = proj.isHeadshot,
                        involvesPlayer = involvesPlayer
                    )
                    // Drop death loot crate
                    lootList += LootItem(
                        id = nextEntityId++,
                        x = victim.x,
                        y = victim.y,
                        kind = if (victim.primarySlot != null) LootKind.WEAPON else LootKind.MEDKIT,
                        weaponType = victim.primarySlot?.weapon,
                        amount = 1,
                        title = victim.primarySlot?.weapon?.shortTag ?: "MEDKIT"
                    )
                    particles += spawnBurst(victim.x, victim.y, BlazeOrange, 18, 220f)
                }
                continue
            }

            remainingProjectiles += proj.copy(prevX = proj.x, prevY = proj.y, x = nx, y = ny, rangeRemaining = rangeLeft)
        }

        // 6. Advance Thrown Grenades
        val remainingGrenades = mutableListOf<ThrownGrenade>()
        for (g in grenades) {
            val newFuse = g.fuseLeft - dt
            val drag = 0.92f
            val nx = (g.x + g.vx * dt).coerceIn(30f, state.worldSize - 30f)
            val ny = (g.y + g.vy * dt).coerceIn(30f, state.worldSize - 30f)
            val hitWall = collidesObstacle(nx, ny, 8f, obstacles)
            val nvx = if (hitWall) -g.vx * 0.4f else g.vx * drag
            val nvy = if (hitWall) -g.vy * 0.4f else g.vy * drag

            if (newFuse <= 0f) {
                onSoundCue(SoundCue.EXPLOSION)
                val blastRadius = 175f
                shockwaves += ShockwaveEffect(g.x, g.y, blastRadius, BlazeOrange, 0.5f, 0.5f)
                particles += spawnBurst(g.x, g.y, BlazeAmber, 26, 280f)

                // Destroy gloo walls in blast
                obstacles.removeAll {
                    it.type == ObstacleType.GLOO_WALL &&
                        hypot((it.x + it.width / 2f) - g.x, (it.y + it.height / 2f) - g.y) <= blastRadius
                }

                // Damage survivors in blast
                for (idx in survivors.indices) {
                    val target = survivors[idx]
                    if (!target.isAlive) continue
                    val dist = hypot(target.x - g.x, target.y - g.y)
                    if (dist <= blastRadius) {
                        val dmg = ((1f - dist / blastRadius) * 140f).toInt().coerceAtLeast(25)
                        val newHp = max(0f, target.hp - dmg)
                        val died = newHp <= 0f
                        survivors[idx] = target.copy(hp = newHp, isAlive = !died)
                        floatingTexts += FloatingText(nextEntityId++, target.x, target.y - 26f, "-$dmg", BlazeOrange, true, 0.9f)
                        if (died) {
                            val ownerIdx = survivors.indexOfFirst { it.id == g.ownerId }
                            if (ownerIdx >= 0) {
                                survivors[ownerIdx] = survivors[ownerIdx].copy(
                                    kills = survivors[ownerIdx].kills + 1,
                                    damageDealt = survivors[ownerIdx].damageDealt + dmg
                                )
                            }
                            killFeed += KillFeedEntry(
                                id = nextEntityId++,
                                killerName = survivors.firstOrNull { it.id == g.ownerId }?.name ?: "FRAG",
                                victimName = target.name,
                                weaponTag = "FRAG",
                                isHeadshot = false,
                                involvesPlayer = g.ownerId == 1 || target.isPlayer
                            )
                        }
                    }
                }
            } else {
                remainingGrenades += g.copy(
                    x = if (hitWall) g.x else nx,
                    y = if (hitWall) g.y else ny,
                    vx = nvx,
                    vy = nvy,
                    fuseLeft = newFuse
                )
            }
        }

        // 7. Training mode respawns bots automatically!
        if (state.mode == GameMode.TRAINING) {
            for (idx in survivors.indices) {
                val s = survivors[idx]
                if (!s.isPlayer && !s.isAlive) {
                    val rx = 240f + rng.nextFloat() * (state.worldSize - 480f)
                    val ry = 200f + rng.nextFloat() * 340f
                    survivors[idx] = s.copy(x = rx, y = ry, hp = s.maxHp, isAlive = true)
                }
            }
        }

        // 8. Determine Match Outcome
        val aliveSurvivors = survivors.count { it.isAlive }
        val playerNow = survivors.firstOrNull { it.isPlayer }
        var nextPhase = state.matchPhase
        var placement = aliveSurvivors

        if (playerNow != null && !playerNow.isAlive) {
            nextPhase = MatchPhase.ELIMINATED
            placement = aliveSurvivors + 1
        } else if (state.mode != GameMode.TRAINING && aliveSurvivors <= 1 && playerNow?.isAlive == true) {
            nextPhase = MatchPhase.VICTORY_BOOYAH
            placement = 1
            onSoundCue(SoundCue.BOOYAH)
        }

        val nearbyLoot = if (playerNow != null && playerNow.isAlive) {
            lootList
                .filter { hypot(it.x - playerNow.x, it.y - playerNow.y) <= 58f }
                .minByOrNull { hypot(it.x - playerNow.x, it.y - playerNow.y) }
        } else null

        worldState = state.copy(
            matchPhase = nextPhase,
            matchTimeSec = state.matchTimeSec + dt,
            survivors = survivors,
            obstacles = obstacles,
            loot = lootList,
            projectiles = remainingProjectiles,
            grenades = remainingGrenades,
            floatingTexts = floatingTexts,
            particles = particles,
            shockwaves = shockwaves,
            killFeed = killFeed.takeLast(3),
            zone = zone,
            aliveCount = aliveSurvivors,
            placement = placement,
            lockedTargetId = lockedTargetId,
            airdropCountdownSec = airdropCountdown,
            announcementBanner = announcement,
            nearbyLoot = nearbyLoot
        )
    }

    private data class AiAction(
        val moveX: Float,
        val moveY: Float,
        val aimAngle: Float,
        val wantsFire: Boolean,
        val nextDecisionTimer: Float,
        val savedMoveX: Float,
        val savedMoveY: Float,
        val targetId: Int?,
        val strafeSign: Float
    )

    private fun computeAiAction(
        bot: Survivor,
        survivors: List<Survivor>,
        obstacles: List<Obstacle>,
        loot: List<LootItem>,
        zone: ZoneState,
        mode: GameMode,
        dt: Float
    ): AiAction {
        val distFromZone = hypot(bot.x - zone.centerX, bot.y - zone.centerY)
        val outsideZone = mode != GameMode.TRAINING && distFromZone > zone.currentRadius * 0.90f

        // Find nearest visible enemy
        val weaponRange = bot.activeSlot.weapon.range * 0.92f
        val visibleEnemy = survivors
            .filter { it.isAlive && it.id != bot.id }
            .map { it to hypot(it.x - bot.x, it.y - bot.y) }
            .filter { (enemy, dist) -> dist <= weaponRange && hasLineOfSight(bot.x, bot.y, enemy.x, enemy.y, obstacles) }
            .minByOrNull { it.second }

        var timer = bot.aiDecisionTimer - dt
        var mx = bot.aiMoveX
        var my = bot.aiMoveY
        var strafe = bot.aiStrafeSign

        if (outsideZone) {
            val angle = atan2(zone.centerY - bot.y, zone.centerX - bot.x)
            mx = cos(angle)
            my = sin(angle)
        } else if (visibleEnemy != null) {
            val (enemy, dist) = visibleEnemy
            if (timer <= 0f) {
                timer = 0.6f + rng.nextFloat() * 0.6f
                strafe = if (rng.nextBoolean()) 1f else -1f
            }
            val angleToEnemy = atan2(enemy.y - bot.y, enemy.x - bot.x)
            val perpAngle = angleToEnemy + (PI.toFloat() / 2f) * strafe
            val approachWeight = if (dist > weaponRange * 0.62f) 0.55f else if (dist < 130f) -0.4f else 0.1f
            mx = cos(perpAngle) * 0.85f + cos(angleToEnemy) * approachWeight
            my = sin(perpAngle) * 0.85f + sin(angleToEnemy) * approachWeight
            val aimJitter = (rng.nextFloat() - 0.5f) * 0.18f
            return AiAction(
                moveX = mx,
                moveY = my,
                aimAngle = angleToEnemy + aimJitter,
                wantsFire = true,
                nextDecisionTimer = timer,
                savedMoveX = mx,
                savedMoveY = my,
                targetId = enemy.id,
                strafeSign = strafe
            )
        } else {
            // Seek loot if missing primary weapon, else roam toward center/zone
            if (timer <= 0f) {
                timer = 1.2f + rng.nextFloat() * 1.4f
                val nearbyGoodLoot = if (bot.primarySlot == null) {
                    loot.filter { it.kind == LootKind.WEAPON && hypot(it.x - bot.x, it.y - bot.y) < 420f }
                        .minByOrNull { hypot(it.x - bot.x, it.y - bot.y) }
                } else null

                if (nearbyGoodLoot != null) {
                    val ang = atan2(nearbyGoodLoot.y - bot.y, nearbyGoodLoot.x - bot.x)
                    mx = cos(ang)
                    my = sin(ang)
                } else {
                    val toCenter = atan2(zone.centerY - bot.y, zone.centerX - bot.x)
                    val wander = toCenter + (rng.nextFloat() * 2f - 1f) * 1.3f
                    mx = cos(wander) * 0.7f
                    my = sin(wander) * 0.7f
                }
            }
        }

        val moveAngle = if (hypot(mx, my) > 0.1f) atan2(my, mx) else bot.aimAngle
        return AiAction(
            moveX = mx,
            moveY = my,
            aimAngle = moveAngle,
            wantsFire = false,
            nextDecisionTimer = timer,
            savedMoveX = mx,
            savedMoveY = my,
            targetId = null,
            strafeSign = strafe
        )
    }

    private fun applyLootPickup(s: Survivor, item: LootItem, forceWeaponSwap: Boolean): Pair<Survivor, Boolean> {
        return when (item.kind) {
            LootKind.WEAPON -> {
                val weapon = item.weaponType ?: return s to false
                if (s.primarySlot == null || forceWeaponSwap) {
                    val slot = WeaponSlotState(weapon, weapon.magSize, weapon.defaultReserve)
                    s.copy(primarySlot = slot, usePrimary = true, isReloading = false) to true
                } else if (s.primarySlot.weapon == weapon) {
                    val slot = s.primarySlot.copy(reserveAmmo = s.primarySlot.reserveAmmo + weapon.magSize * 2)
                    s.copy(primarySlot = slot) to true
                } else {
                    s to false
                }
            }
            LootKind.MEDKIT -> s.copy(medkits = min(5, s.medkits + item.amount)) to true
            LootKind.INHALER -> s.copy(ep = min(s.maxEp, s.ep + 75f)) to true
            LootKind.GLOO_WALL -> s.copy(glooWalls = min(6, s.glooWalls + item.amount)) to true
            LootKind.GRENADE -> s.copy(grenades = min(4, s.grenades + item.amount)) to true
            LootKind.ARMOR_LV1 -> if (s.armorLevel < 1) s.copy(armorLevel = 1, armorHp = 75f, maxArmorHp = 75f) to true else s to false
            LootKind.ARMOR_LV2 -> if (s.armorLevel < 2 || s.armorHp < 100f) s.copy(armorLevel = 2, armorHp = 130f, maxArmorHp = 130f) to true else s to false
            LootKind.ARMOR_LV3 -> s.copy(armorLevel = 3, armorHp = 200f, maxArmorHp = 200f) to true
            LootKind.HELMET_LV1 -> if (s.helmetLevel < 1) s.copy(helmetLevel = 1) to true else s to false
            LootKind.HELMET_LV2 -> if (s.helmetLevel < 2) s.copy(helmetLevel = 2) to true else s to false
            LootKind.AMMO -> {
                val primary = s.primarySlot?.let { it.copy(reserveAmmo = it.reserveAmmo + item.amount) }
                val secondary = s.secondarySlot.copy(reserveAmmo = s.secondarySlot.reserveAmmo + 20)
                s.copy(primarySlot = primary, secondarySlot = secondary) to true
            }
            LootKind.AIRDROP_CRATE -> {
                val awm = WeaponSlotState(WeaponType.AWM, WeaponType.AWM.magSize, WeaponType.AWM.defaultReserve)
                s.copy(
                    primarySlot = awm,
                    usePrimary = true,
                    armorLevel = 3,
                    armorHp = 200f,
                    maxArmorHp = 200f,
                    helmetLevel = 2,
                    medkits = min(5, s.medkits + 2),
                    glooWalls = min(6, s.glooWalls + 2)
                ) to true
            }
        }
    }

    private fun updateSurvivor(id: Int, transform: (Survivor) -> Survivor) {
        worldState = worldState.copy(
            survivors = worldState.survivors.map { if (it.id == id) transform(it) else it }
        )
    }

    private fun moveWithCollision(
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        radius: Float,
        obstacles: List<Obstacle>,
        worldSize: Float
    ): Pair<Float, Float> {
        var nx = (x + dx).coerceIn(radius + 12f, worldSize - radius - 12f)
        if (collidesObstacle(nx, y, radius, obstacles)) {
            nx = x
        }
        var ny = (y + dy).coerceIn(radius + 12f, worldSize - radius - 12f)
        if (collidesObstacle(nx, ny, radius, obstacles)) {
            ny = y
        }
        return nx to ny
    }

    private fun collidesObstacle(cx: Float, cy: Float, radius: Float, obstacles: List<Obstacle>): Boolean {
        for (o in obstacles) {
            val closestX = cx.coerceIn(o.x, o.x + o.width)
            val closestY = cy.coerceIn(o.y, o.y + o.height)
            val distSq = (cx - closestX) * (cx - closestX) + (cy - closestY) * (cy - closestY)
            if (distSq < radius * radius) return true
        }
        return false
    }

    private fun findNearestFreePosition(
        x: Float,
        y: Float,
        radius: Float,
        obstacles: List<Obstacle>,
        worldSize: Float
    ): Pair<Float, Float> {
        if (!collidesObstacle(x, y, radius, obstacles)) return x to y
        for (ring in 1..10) {
            val r = ring * 34f
            for (step in 0 until 12) {
                val ang = (step / 12f) * 2f * PI.toFloat()
                val tx = (x + cos(ang) * r).coerceIn(radius + 20f, worldSize - radius - 20f)
                val ty = (y + sin(ang) * r).coerceIn(radius + 20f, worldSize - radius - 20f)
                if (!collidesObstacle(tx, ty, radius, obstacles)) return tx to ty
            }
        }
        return x to y
    }

    private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float, obstacles: List<Obstacle>): Boolean {
        for (o in obstacles) {
            if (segmentHitsRect(x1, y1, x2, y2, o)) return false
        }
        return true
    }

    private fun segmentHitsRect(x1: Float, y1: Float, x2: Float, y2: Float, rect: Obstacle): Boolean {
        val minX = min(x1, x2)
        val maxX = max(x1, x2)
        val minY = min(y1, y2)
        val maxY = max(y1, y2)
        if (maxX < rect.x || minX > rect.x + rect.width || maxY < rect.y || minY > rect.y + rect.height) {
            return false
        }
        // Sample 5 points along segment for fast reliable intersection
        val steps = 5
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val px = x1 + (x2 - x1) * t
            val py = y1 + (y2 - y1) * t
            if (px >= rect.x && px <= rect.x + rect.width && py >= rect.y && py <= rect.y + rect.height) {
                return true
            }
        }
        return false
    }

    private fun pointToSegmentDist(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq < 0.0001f) return hypot(px - x1, py - y1)
        val t = (((px - x1) * dx + (py - y1) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        return hypot(px - projX, py - projY)
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = (b - a) % (2f * PI.toFloat())
        if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()
        if (diff < -PI.toFloat()) diff += 2f * PI.toFloat()
        return a + diff * t
    }

    private fun spawnBurst(x: Float, y: Float, color: Color, count: Int, speed: Float): List<Particle> {
        return List(count) {
            val ang = rng.nextFloat() * 2f * PI.toFloat()
            val spd = (0.3f + rng.nextFloat() * 0.7f) * speed
            val maxLife = 0.25f + rng.nextFloat() * 0.35f
            Particle(
                x = x,
                y = y,
                vx = cos(ang) * spd,
                vy = sin(ang) * spd,
                color = color,
                size = 3f + rng.nextFloat() * 4f,
                life = maxLife,
                maxLife = maxLife
            )
        }
    }

    private fun advanceParticles(list: List<Particle>, dt: Float): List<Particle> {
        return list.mapNotNull { p ->
            val nl = p.life - dt
            if (nl <= 0f) null
            else p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt, vx = p.vx * 0.92f, vy = p.vy * 0.92f, life = nl)
        }
    }

    private fun advanceFloatingTexts(list: List<FloatingText>, dt: Float): List<FloatingText> {
        return list.mapNotNull { t ->
            val nl = t.life - dt
            if (nl <= 0f) null else t.copy(y = t.y - 34f * dt, life = nl)
        }
    }

    private fun advanceShockwaves(list: List<ShockwaveEffect>, dt: Float): List<ShockwaveEffect> {
        return list.mapNotNull { s ->
            val nl = s.life - dt
            if (nl <= 0f) null else s.copy(life = nl)
        }
    }
}
