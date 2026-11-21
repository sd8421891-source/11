package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BlazeAmber
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.LootCommon
import com.example.ui.theme.LootEpic
import com.example.ui.theme.LootLegendary
import com.example.ui.theme.LootRare
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.ZoneCyan

enum class GameMode(
    val title: String,
    val subtitle: String,
    val botCount: Int,
    val worldSize: Float
) {
    BR_SOLO("BR SOLO", "20 Survivors · Shrinking Zone · Airdrops", 19, 2400f),
    CLASH_SQUAD("CLASH 1v5", "Compact Arena · Elite Tactical Squad", 5, 1300f),
    TRAINING("TRAINING", "Target Bots · Fast Respawns · All Guns", 6, 1400f)
}

enum class CharacterClass(
    val codename: String,
    val roleTitle: String,
    val skillName: String,
    val skillDesc: String,
    val accentColor: Color,
    val cooldownSec: Float,
    val durationSec: Float
) {
    PULSE(
        codename = "PULSE",
        roleTitle = "Combat Medic",
        skillName = "Beat Aura",
        skillDesc = "+14 HP/s aura & +25% sprint speed for 5s",
        accentColor = BlazeAmber,
        cooldownSec = 20f,
        durationSec = 5f
    ),
    VIPER(
        codename = "VIPER",
        roleTitle = "Rusher",
        skillName = "Surge Dash",
        skillDesc = "Instant dash burst & +35% fire rate for 4s",
        accentColor = BlazeOrange,
        cooldownSec = 15f,
        durationSec = 4f
    ),
    AEGIS(
        codename = "AEGIS",
        roleTitle = "Vanguard",
        skillName = "Chrono Dome",
        skillDesc = "Bulletproof energy dome shield for 3.5s",
        accentColor = ShieldCyan,
        cooldownSec = 22f,
        durationSec = 3.5f
    ),
    BREACH(
        codename = "BREACH",
        roleTitle = "Demolitionist",
        skillName = "Sonic Wave",
        skillDesc = "45 AoE dmg, shatters gloo walls & +30 HP",
        accentColor = ZoneCyan,
        cooldownSec = 18f,
        durationSec = 0.6f
    )
}

enum class WeaponRarity(val label: String, val color: Color) {
    COMMON("COMMON", LootCommon),
    RARE("RARE", LootRare),
    EPIC("EPIC", LootEpic),
    LEGENDARY("LEGENDARY", LootLegendary)
}

enum class WeaponType(
    val displayName: String,
    val shortTag: String,
    val category: String,
    val rarity: WeaponRarity,
    val damage: Int,
    val headshotMult: Float,
    val baseHeadshotChance: Float,
    val fireIntervalMs: Long,
    val magSize: Int,
    val defaultReserve: Int,
    val reloadSec: Float,
    val range: Float,
    val spreadRad: Float,
    val bulletSpeed: Float,
    val pellets: Int,
    val isSecondary: Boolean
) {
    DESERT_EAGLE(
        displayName = "Desert Eagle",
        shortTag = "DEAGLE",
        category = "Pistol",
        rarity = WeaponRarity.COMMON,
        damage = 34,
        headshotMult = 1.9f,
        baseHeadshotChance = 0.22f,
        fireIntervalMs = 340L,
        magSize = 7,
        defaultReserve = 42,
        reloadSec = 1.3f,
        range = 460f,
        spreadRad = 0.04f,
        bulletSpeed = 1150f,
        pellets = 1,
        isSecondary = true
    ),
    MP40(
        displayName = "MP40 Cobra",
        shortTag = "MP40",
        category = "SMG",
        rarity = WeaponRarity.RARE,
        damage = 17,
        headshotMult = 1.75f,
        baseHeadshotChance = 0.16f,
        fireIntervalMs = 85L,
        magSize = 28,
        defaultReserve = 140,
        reloadSec = 1.4f,
        range = 390f,
        spreadRad = 0.11f,
        bulletSpeed = 1080f,
        pellets = 1,
        isSecondary = false
    ),
    M4A1(
        displayName = "M4A1 Tactical",
        shortTag = "M4A1",
        category = "AR",
        rarity = WeaponRarity.RARE,
        damage = 25,
        headshotMult = 1.85f,
        baseHeadshotChance = 0.20f,
        fireIntervalMs = 135L,
        magSize = 30,
        defaultReserve = 150,
        reloadSec = 1.6f,
        range = 620f,
        spreadRad = 0.055f,
        bulletSpeed = 1280f,
        pellets = 1,
        isSecondary = false
    ),
    AK47(
        displayName = "AK47 Flame",
        shortTag = "AK47",
        category = "AR",
        rarity = WeaponRarity.EPIC,
        damage = 32,
        headshotMult = 1.95f,
        baseHeadshotChance = 0.24f,
        fireIntervalMs = 165L,
        magSize = 30,
        defaultReserve = 120,
        reloadSec = 1.8f,
        range = 650f,
        spreadRad = 0.075f,
        bulletSpeed = 1320f,
        pellets = 1,
        isSecondary = false
    ),
    M1887(
        displayName = "M1887 Dual",
        shortTag = "M1887",
        category = "Shotgun",
        rarity = WeaponRarity.EPIC,
        damage = 19,
        headshotMult = 1.65f,
        baseHeadshotChance = 0.18f,
        fireIntervalMs = 520L,
        magSize = 2,
        defaultReserve = 24,
        reloadSec = 1.4f,
        range = 250f,
        spreadRad = 0.24f,
        bulletSpeed = 1020f,
        pellets = 6,
        isSecondary = false
    ),
    AWM(
        displayName = "AWM Dragon",
        shortTag = "AWM",
        category = "Sniper",
        rarity = WeaponRarity.LEGENDARY,
        damage = 98,
        headshotMult = 2.2f,
        baseHeadshotChance = 0.35f,
        fireIntervalMs = 950L,
        magSize = 5,
        defaultReserve = 25,
        reloadSec = 2.0f,
        range = 920f,
        spreadRad = 0.01f,
        bulletSpeed = 1850f,
        pellets = 1,
        isSecondary = false
    )
}

enum class ObstacleType {
    BUILDING_WALL,
    CRATE,
    ROCK,
    GLOO_WALL
}

data class Obstacle(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: ObstacleType,
    val hp: Float = Float.MAX_VALUE,
    val maxHp: Float = Float.MAX_VALUE
)

data class FoliagePatch(
    val x: Float,
    val y: Float,
    val radius: Float,
    val isTree: Boolean
)

data class MapPoi(
    val name: String,
    val x: Float,
    val y: Float
)

data class BuildingFloor(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val label: String
)

enum class LootKind {
    WEAPON,
    MEDKIT,
    INHALER,
    ARMOR_LV1,
    ARMOR_LV2,
    ARMOR_LV3,
    HELMET_LV1,
    HELMET_LV2,
    GLOO_WALL,
    GRENADE,
    AMMO,
    AIRDROP_CRATE
}

data class LootItem(
    val id: Int,
    val x: Float,
    val y: Float,
    val kind: LootKind,
    val weaponType: WeaponType? = null,
    val amount: Int = 1,
    val title: String
)

data class WeaponSlotState(
    val weapon: WeaponType,
    val ammoInMag: Int,
    val reserveAmmo: Int
)

data class Survivor(
    val id: Int,
    val name: String,
    val isPlayer: Boolean,
    val characterClass: CharacterClass,
    val x: Float,
    val y: Float,
    val radius: Float = 20f,
    val aimAngle: Float = 0f,
    val hp: Float = 200f,
    val maxHp: Float = 200f,
    val ep: Float = 40f,
    val maxEp: Float = 200f,
    val armorLevel: Int = 0,
    val armorHp: Float = 0f,
    val maxArmorHp: Float = 0f,
    val helmetLevel: Int = 0,
    val primarySlot: WeaponSlotState? = null,
    val secondarySlot: WeaponSlotState = WeaponSlotState(
        WeaponType.DESERT_EAGLE,
        WeaponType.DESERT_EAGLE.magSize,
        WeaponType.DESERT_EAGLE.defaultReserve
    ),
    val usePrimary: Boolean = false,
    val medkits: Int = 2,
    val glooWalls: Int = 2,
    val grenades: Int = 1,
    val isReloading: Boolean = false,
    val reloadTimer: Float = 0f,
    val reloadTotal: Float = 1f,
    val isHealing: Boolean = false,
    val healTimer: Float = 0f,
    val skillCooldownLeft: Float = 0f,
    val skillActiveLeft: Float = 0f,
    val lastShotMs: Long = 0L,
    val muzzleFlashTimer: Float = 0f,
    val kills: Int = 0,
    val headshots: Int = 0,
    val damageDealt: Int = 0,
    val isAlive: Boolean = true,
    val accentColor: Color = BlazeOrange,
    // AI bookkeeping
    val aiDecisionTimer: Float = 0f,
    val aiMoveX: Float = 0f,
    val aiMoveY: Float = 0f,
    val aiTargetId: Int? = null,
    val aiStrafeSign: Float = 1f
) {
    val activeSlot: WeaponSlotState
        get() = if (usePrimary && primarySlot != null) primarySlot else secondarySlot
}

data class Projectile(
    val id: Int,
    val x: Float,
    val y: Float,
    val prevX: Float,
    val prevY: Float,
    val vx: Float,
    val vy: Float,
    val ownerId: Int,
    val weapon: WeaponType,
    val damage: Float,
    val isHeadshot: Boolean,
    val rangeRemaining: Float,
    val color: Color
)

data class ThrownGrenade(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val fuseLeft: Float,
    val ownerId: Int
)

data class FloatingText(
    val id: Int,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val isCritical: Boolean,
    val life: Float
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val life: Float,
    val maxLife: Float
)

data class ShockwaveEffect(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

data class KillFeedEntry(
    val id: Int,
    val killerName: String,
    val victimName: String,
    val weaponTag: String,
    val isHeadshot: Boolean,
    val involvesPlayer: Boolean,
    val ageSec: Float = 0f
)

data class ZoneState(
    val centerX: Float,
    val centerY: Float,
    val currentRadius: Float,
    val targetCenterX: Float,
    val targetCenterY: Float,
    val targetRadius: Float,
    val phase: Int,
    val phaseTimerSec: Float,
    val isShrinking: Boolean,
    val damagePerSec: Float
)

enum class MatchPhase {
    AIRDROP_PLANE,
    PARACHUTING,
    COMBAT,
    VICTORY_BOOYAH,
    ELIMINATED
}

data class PlayerControlInput(
    val moveX: Float = 0f,
    val moveY: Float = 0f,
    val aimX: Float = 0f,
    val aimY: Float = 0f,
    val isManualAiming: Boolean = false,
    val isFiring: Boolean = false,
    val isSprinting: Boolean = false,
    val isScoped: Boolean = false
)

data class BattleWorldState(
    val mode: GameMode = GameMode.BR_SOLO,
    val worldSize: Float = 2400f,
    val matchPhase: MatchPhase = MatchPhase.AIRDROP_PLANE,
    val matchTimeSec: Float = 0f,
    val planeX: Float = 0f,
    val planeY: Float = 0f,
    val planeDirX: Float = 1f,
    val planeDirY: Float = 0f,
    val parachuteAltitude: Float = 400f,
    val survivors: List<Survivor> = emptyList(),
    val obstacles: List<Obstacle> = emptyList(),
    val floors: List<BuildingFloor> = emptyList(),
    val foliage: List<FoliagePatch> = emptyList(),
    val pois: List<MapPoi> = emptyList(),
    val loot: List<LootItem> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val grenades: List<ThrownGrenade> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val shockwaves: List<ShockwaveEffect> = emptyList(),
    val killFeed: List<KillFeedEntry> = emptyList(),
    val zone: ZoneState = ZoneState(1200f, 1200f, 1450f, 1200f, 1200f, 950f, 1, 22f, false, 4f),
    val aliveCount: Int = 20,
    val totalCount: Int = 20,
    val placement: Int = 20,
    val lockedTargetId: Int? = null,
    val airdropCountdownSec: Float = 28f,
    val announcementBanner: String? = null,
    val nearbyLoot: LootItem? = null
) {
    val player: Survivor?
        get() = survivors.firstOrNull { it.isPlayer }
}
