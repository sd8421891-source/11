package com.example.game

import kotlin.math.hypot
import kotlin.random.Random

data class GeneratedMap(
    val worldSize: Float,
    val obstacles: List<Obstacle>,
    val floors: List<BuildingFloor>,
    val foliage: List<FoliagePatch>,
    val pois: List<MapPoi>,
    val loot: List<LootItem>
)

object MapGenerator {

    fun generate(mode: GameMode, seed: Int = Random.nextInt()): GeneratedMap {
        val rng = Random(seed)
        return when (mode) {
            GameMode.BR_SOLO -> generateBattleRoyaleMap(mode.worldSize, rng)
            GameMode.CLASH_SQUAD -> generateClashArena(mode.worldSize, rng)
            GameMode.TRAINING -> generateTrainingRange(mode.worldSize, rng)
        }
    }

    private fun generateBattleRoyaleMap(worldSize: Float, rng: Random): GeneratedMap {
        val obstacles = mutableListOf<Obstacle>()
        val floors = mutableListOf<BuildingFloor>()
        val foliage = mutableListOf<FoliagePatch>()
        val loot = mutableListOf<LootItem>()
        var nextObsId = 1
        var nextLootId = 1

        val pois = listOf(
            MapPoi("CLOCK TOWER", 640f, 820f),
            MapPoi("PEAK", 1220f, 560f),
            MapPoi("MILL", 1840f, 640f),
            MapPoi("BIMASAKTI", 1200f, 1240f),
            MapPoi("FACTORY", 1820f, 1580f),
            MapPoi("POCHINOK", 660f, 1740f)
        )

        fun addBuilding(cx: Float, cy: Float, w: Float, h: Float, label: String, lootCount: Int) {
            val left = cx - w / 2f
            val top = cy - h / 2f
            val wallT = 18f
            val doorW = 68f
            floors += BuildingFloor(left, top, w, h, label)

            // Top wall with doorway
            val topSeg = (w - doorW) / 2f
            obstacles += Obstacle(nextObsId++, left, top, topSeg, wallT, ObstacleType.BUILDING_WALL)
            obstacles += Obstacle(nextObsId++, left + topSeg + doorW, top, topSeg, wallT, ObstacleType.BUILDING_WALL)

            // Bottom wall with doorway
            obstacles += Obstacle(nextObsId++, left, top + h - wallT, topSeg, wallT, ObstacleType.BUILDING_WALL)
            obstacles += Obstacle(nextObsId++, left + topSeg + doorW, top + h - wallT, topSeg, wallT, ObstacleType.BUILDING_WALL)

            // Left wall with doorway
            val sideSeg = (h - doorW) / 2f
            obstacles += Obstacle(nextObsId++, left, top + wallT, wallT, sideSeg - wallT, ObstacleType.BUILDING_WALL)
            obstacles += Obstacle(nextObsId++, left, top + sideSeg + doorW, wallT, sideSeg - wallT, ObstacleType.BUILDING_WALL)

            // Right wall with doorway
            obstacles += Obstacle(nextObsId++, left + w - wallT, top + wallT, wallT, sideSeg - wallT, ObstacleType.BUILDING_WALL)
            obstacles += Obstacle(nextObsId++, left + w - wallT, top + sideSeg + doorW, wallT, sideSeg - wallT, ObstacleType.BUILDING_WALL)

            repeat(lootCount) {
                val lx = cx + rng.nextFloat() * (w * 0.55f) - w * 0.275f
                val ly = cy + rng.nextFloat() * (h * 0.55f) - h * 0.275f
                loot += randomLootItem(nextLootId++, lx, ly, rng, highTier = label == "PEAK" || label == "CLOCK TOWER")
            }
        }

        // Major POI compounds
        addBuilding(640f, 820f, 250f, 220f, "CLOCK TOWER", 5)
        addBuilding(460f, 650f, 170f, 150f, "OUTPOST", 3)
        addBuilding(1220f, 560f, 270f, 210f, "PEAK", 6)
        addBuilding(1000f, 440f, 160f, 140f, "MANSION", 3)
        addBuilding(1840f, 640f, 240f, 200f, "MILL", 5)
        addBuilding(1200f, 1240f, 220f, 220f, "BIMASAKTI", 5)
        addBuilding(1430f, 1140f, 160f, 150f, "DEPOT", 3)
        addBuilding(1820f, 1580f, 290f, 230f, "FACTORY", 6)
        addBuilding(660f, 1740f, 230f, 200f, "POCHINOK", 5)
        addBuilding(890f, 1860f, 170f, 150f, "BARRACKS", 3)
        addBuilding(1240f, 1900f, 190f, 160f, "RIVERSIDE", 4)

        // Tactical crate clusters & rocks in open areas
        repeat(42) {
            val rx = 180f + rng.nextFloat() * (worldSize - 360f)
            val ry = 180f + rng.nextFloat() * (worldSize - 360f)
            val nearBuilding = floors.any {
                rx > it.x - 45f && rx < it.x + it.width + 45f &&
                    ry > it.y - 45f && ry < it.y + it.height + 45f
            }
            if (!nearBuilding) {
                val isCrate = rng.nextFloat() < 0.55f
                val size = if (isCrate) 46f + rng.nextFloat() * 18f else 54f + rng.nextFloat() * 34f
                obstacles += Obstacle(
                    id = nextObsId++,
                    x = rx - size / 2f,
                    y = ry - size / 2f,
                    width = size,
                    height = if (isCrate) size else size * 0.85f,
                    type = if (isCrate) ObstacleType.CRATE else ObstacleType.ROCK
                )
                if (isCrate && rng.nextFloat() < 0.65f) {
                    loot += randomLootItem(nextLootId++, rx + size * 0.85f, ry, rng, highTier = false)
                }
            }
        }

        // Lush trees & bushes for atmosphere
        repeat(65) {
            val tx = 140f + rng.nextFloat() * (worldSize - 280f)
            val ty = 140f + rng.nextFloat() * (worldSize - 280f)
            val inBuilding = floors.any {
                tx > it.x - 20f && tx < it.x + it.width + 20f &&
                    ty > it.y - 20f && ty < it.y + it.height + 20f
            }
            if (!inBuilding) {
                val isTree = rng.nextFloat() < 0.6f
                foliage += FoliagePatch(
                    x = tx,
                    y = ty,
                    radius = if (isTree) 44f + rng.nextFloat() * 18f else 26f + rng.nextFloat() * 10f,
                    isTree = isTree
                )
            }
        }

        // Scattered field loot
        repeat(24) {
            val lx = 220f + rng.nextFloat() * (worldSize - 440f)
            val ly = 220f + rng.nextFloat() * (worldSize - 440f)
            if (!isInsideAnyObstacle(lx, ly, obstacles)) {
                loot += randomLootItem(nextLootId++, lx, ly, rng, highTier = false)
            }
        }

        return GeneratedMap(worldSize, obstacles, floors, foliage, pois, loot)
    }

    private fun generateClashArena(worldSize: Float, rng: Random): GeneratedMap {
        val obstacles = mutableListOf<Obstacle>()
        val floors = mutableListOf<BuildingFloor>()
        val foliage = mutableListOf<FoliagePatch>()
        val loot = mutableListOf<LootItem>()
        var nextObsId = 1
        var nextLootId = 1
        val mid = worldSize / 2f

        val pois = listOf(
            MapPoi("ARENA CORE", mid, mid),
            MapPoi("NORTH WAREHOUSE", mid, 260f),
            MapPoi("SOUTH DOCK", mid, worldSize - 260f)
        )

        // Central symmetrical cover blocks
        floors += BuildingFloor(mid - 140f, mid - 110f, 280f, 220f, "ARENA CORE")
        val wallT = 20f
        obstacles += Obstacle(nextObsId++, mid - 140f, mid - 110f, 95f, wallT, ObstacleType.BUILDING_WALL)
        obstacles += Obstacle(nextObsId++, mid + 45f, mid - 110f, 95f, wallT, ObstacleType.BUILDING_WALL)
        obstacles += Obstacle(nextObsId++, mid - 140f, mid + 90f, 95f, wallT, ObstacleType.BUILDING_WALL)
        obstacles += Obstacle(nextObsId++, mid + 45f, mid + 90f, 95f, wallT, ObstacleType.BUILDING_WALL)

        // Flanking tactical crates
        val offsets = listOf(
            -320f to -220f, 320f to -220f,
            -320f to 220f, 320f to 220f,
            -260f to 0f, 260f to 0f,
            0f to -340f, 0f to 340f
        )
        offsets.forEach { (ox, oy) ->
            obstacles += Obstacle(nextObsId++, mid + ox - 32f, mid + oy - 32f, 64f, 64f, ObstacleType.CRATE)
            loot += randomLootItem(nextLootId++, mid + ox + 56f, mid + oy, rng, highTier = true)
        }

        // Extra healing & gloo walls in arena
        repeat(10) {
            val lx = 200f + rng.nextFloat() * (worldSize - 400f)
            val ly = 200f + rng.nextFloat() * (worldSize - 400f)
            if (!isInsideAnyObstacle(lx, ly, obstacles)) {
                loot += randomLootItem(nextLootId++, lx, ly, rng, highTier = true)
            }
        }

        repeat(14) {
            val tx = 140f + rng.nextFloat() * (worldSize - 280f)
            val ty = 140f + rng.nextFloat() * (worldSize - 280f)
            if (hypot(tx - mid, ty - mid) > 180f) {
                foliage += FoliagePatch(tx, ty, 34f, isTree = true)
            }
        }

        return GeneratedMap(worldSize, obstacles, floors, foliage, pois, loot)
    }

    private fun generateTrainingRange(worldSize: Float, rng: Random): GeneratedMap {
        val obstacles = mutableListOf<Obstacle>()
        val floors = mutableListOf<BuildingFloor>()
        val foliage = mutableListOf<FoliagePatch>()
        val loot = mutableListOf<LootItem>()
        var nextObsId = 1
        var nextLootId = 1
        val mid = worldSize / 2f

        val pois = listOf(
            MapPoi("ARMORY SPAWN", mid, worldSize - 260f),
            MapPoi("COMBAT RANGE", mid, mid - 80f)
        )

        floors += BuildingFloor(mid - 220f, worldSize - 340f, 440f, 140f, "ARMORY")
        // Lay out all weapons cleanly in the Armory
        val weapons = listOf(WeaponType.M4A1, WeaponType.AK47, WeaponType.MP40, WeaponType.M1887, WeaponType.AWM)
        weapons.forEachIndexed { idx, w ->
            loot += LootItem(
                id = nextLootId++,
                x = mid - 160f + idx * 80f,
                y = worldSize - 270f,
                kind = LootKind.WEAPON,
                weaponType = w,
                title = w.shortTag
            )
        }
        loot += LootItem(nextLootId++, mid - 120f, worldSize - 215f, LootKind.ARMOR_LV3, title = "LV.3 VEST")
        loot += LootItem(nextLootId++, mid, worldSize - 215f, LootKind.GLOO_WALL, amount = 4, title = "GLOO x4")
        loot += LootItem(nextLootId++, mid + 120f, worldSize - 215f, LootKind.MEDKIT, amount = 3, title = "MEDKIT x3")

        // Shooting range cover
        val coverCoords = listOf(
            mid - 240f to mid, mid + 180f to mid,
            mid - 40f to mid - 160f, mid - 280f to mid - 260f,
            mid + 220f to mid - 260f, mid to mid + 150f
        )
        coverCoords.forEach { (cx, cy) ->
            obstacles += Obstacle(nextObsId++, cx, cy, 68f, 48f, ObstacleType.CRATE)
        }

        repeat(10) {
            foliage += FoliagePatch(
                x = 160f + rng.nextFloat() * (worldSize - 320f),
                y = 160f + rng.nextFloat() * (worldSize - 520f),
                radius = 36f,
                isTree = true
            )
        }

        return GeneratedMap(worldSize, obstacles, floors, foliage, pois, loot)
    }

    private fun isInsideAnyObstacle(x: Float, y: Float, obstacles: List<Obstacle>): Boolean {
        return obstacles.any {
            x >= it.x - 16f && x <= it.x + it.width + 16f &&
                y >= it.y - 16f && y <= it.y + it.height + 16f
        }
    }

    fun randomLootItem(id: Int, x: Float, y: Float, rng: Random, highTier: Boolean): LootItem {
        val roll = rng.nextFloat()
        return when {
            roll < 0.36f -> {
                val weapon = when {
                    highTier && rng.nextFloat() < 0.20f -> WeaponType.AWM
                    rng.nextFloat() < 0.28f -> WeaponType.AK47
                    rng.nextFloat() < 0.56f -> WeaponType.M4A1
                    rng.nextFloat() < 0.80f -> WeaponType.MP40
                    else -> WeaponType.M1887
                }
                LootItem(id, x, y, LootKind.WEAPON, weaponType = weapon, title = weapon.shortTag)
            }
            roll < 0.54f -> LootItem(id, x, y, LootKind.MEDKIT, amount = 1, title = "MEDKIT")
            roll < 0.65f -> LootItem(id, x, y, LootKind.INHALER, amount = 1, title = "EP BOOST")
            roll < 0.77f -> LootItem(id, x, y, LootKind.GLOO_WALL, amount = 2, title = "GLOO x2")
            roll < 0.85f -> LootItem(id, x, y, LootKind.GRENADE, amount = 1, title = "FRAG")
            roll < 0.92f -> {
                val kind = if (highTier) LootKind.ARMOR_LV3 else if (rng.nextBoolean()) LootKind.ARMOR_LV2 else LootKind.ARMOR_LV1
                val label = when (kind) {
                    LootKind.ARMOR_LV3 -> "LV.3 VEST"
                    LootKind.ARMOR_LV2 -> "LV.2 VEST"
                    else -> "LV.1 VEST"
                }
                LootItem(id, x, y, kind, title = label)
            }
            roll < 0.96f -> {
                val kind = if (highTier) LootKind.HELMET_LV2 else LootKind.HELMET_LV1
                LootItem(id, x, y, kind, title = if (kind == LootKind.HELMET_LV2) "LV.2 HELM" else "LV.1 HELM")
            }
            else -> LootItem(id, x, y, LootKind.AMMO, amount = 45, title = "AMMO +45")
        }
    }
}
