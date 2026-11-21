package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.game.BattleWorldState
import com.example.game.CharacterClass
import com.example.game.LootKind
import com.example.game.MatchPhase
import com.example.game.ObstacleType
import com.example.game.Survivor
import com.example.ui.theme.BlazeAmber
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.HpGreen
import com.example.ui.theme.LootCommon
import com.example.ui.theme.LootEpic
import com.example.ui.theme.LootLegendary
import com.example.ui.theme.LootRare
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.ZoneCyan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun BattleWorldCanvas(
    world: BattleWorldState,
    isScoped: Boolean,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val poiPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color(0x55E2E8F0).toArgb()
            textSize = 26f
            letterSpacing = 0.12f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseUnit = min(w, h)
        val player = world.player

        val camX = when (world.matchPhase) {
            MatchPhase.AIRDROP_PLANE -> world.planeX
            else -> player?.x ?: (world.worldSize / 2f)
        }
        val camY = when (world.matchPhase) {
            MatchPhase.AIRDROP_PLANE -> world.planeY
            else -> player?.y ?: (world.worldSize / 2f)
        }

        val zoom = when {
            world.matchPhase == MatchPhase.AIRDROP_PLANE -> baseUnit / 940f
            world.matchPhase == MatchPhase.PARACHUTING -> {
                val altFrac = (world.parachuteAltitude / 400f).coerceIn(0f, 1f)
                baseUnit / (560f + altFrac * 360f)
            }
            isScoped -> baseUnit / 760f
            else -> baseUnit / 540f
        }

        // Sea background outside island
        drawRect(color = Color(0xFF0A1D2C))

        withTransform({
            translate(left = w / 2f, top = h / 2f)
            scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
            translate(left = -camX, top = -camY)
        }) {
            val worldSize = world.worldSize

            // 1. Sandy shoreline + Tactical Island Ground
            drawRect(
                color = Color(0xFF8C7A5B),
                topLeft = Offset(-28f, -28f),
                size = Size(worldSize + 56f, worldSize + 56f)
            )
            drawRect(
                color = Color(0xFF1F2B21),
                topLeft = Offset.Zero,
                size = Size(worldSize, worldSize)
            )

            // Tactical ground grid
            val gridStep = 160f
            var gx = gridStep
            while (gx < worldSize) {
                drawLine(Color(0x14FFFFFF), Offset(gx, 0f), Offset(gx, worldSize), strokeWidth = 1.5f)
                drawLine(Color(0x14FFFFFF), Offset(0f, gx), Offset(worldSize, gx), strokeWidth = 1.5f)
                gx += gridStep
            }

            // Crossroads dirt highways
            drawLine(
                color = Color(0xFF39362B),
                start = Offset(0f, worldSize * 0.5f),
                end = Offset(worldSize, worldSize * 0.5f),
                strokeWidth = 44f
            )
            drawLine(
                color = Color(0xFF39362B),
                start = Offset(worldSize * 0.5f, 0f),
                end = Offset(worldSize * 0.5f, worldSize),
                strokeWidth = 44f
            )

            // 2. Building Floors
            world.floors.forEach { floor ->
                drawRect(
                    color = Color(0xFF2D333E),
                    topLeft = Offset(floor.x, floor.y),
                    size = Size(floor.width, floor.height)
                )
                drawRect(
                    color = Color(0xFF424B59),
                    topLeft = Offset(floor.x, floor.y),
                    size = Size(floor.width, floor.height),
                    style = Stroke(width = 2f)
                )
            }

            // POI Labels
            world.pois.forEach { poi ->
                drawContext.canvas.nativeCanvas.drawText(poi.name, poi.x, poi.y + 8f, poiPaint)
            }

            // 3. Loot Items on Ground
            world.loot.forEach { item ->
                val color = when (item.kind) {
                    LootKind.WEAPON -> item.weaponType?.rarity?.color ?: LootCommon
                    LootKind.MEDKIT -> HpGreen
                    LootKind.INHALER -> BlazeAmber
                    LootKind.GLOO_WALL -> ShieldCyan
                    LootKind.GRENADE -> BlazeOrange
                    LootKind.ARMOR_LV3, LootKind.AIRDROP_CRATE -> LootLegendary
                    LootKind.ARMOR_LV2, LootKind.HELMET_LV2 -> LootEpic
                    LootKind.ARMOR_LV1, LootKind.HELMET_LV1 -> LootRare
                    LootKind.AMMO -> Color(0xFFE2E8F0)
                }

                if (item.kind == LootKind.AIRDROP_CRATE) {
                    drawCircle(color = BlazeGold.copy(alpha = 0.25f), radius = 34f, center = Offset(item.x, item.y))
                    drawRect(
                        color = Color(0xFF991B1B),
                        topLeft = Offset(item.x - 16f, item.y - 16f),
                        size = Size(32f, 32f)
                    )
                    drawRect(
                        color = BlazeGold,
                        topLeft = Offset(item.x - 16f, item.y - 16f),
                        size = Size(32f, 32f),
                        style = Stroke(width = 3f)
                    )
                } else {
                    drawCircle(color = color.copy(alpha = 0.22f), radius = 17f, center = Offset(item.x, item.y))
                    drawCircle(color = color, radius = 8f, center = Offset(item.x, item.y))
                    drawCircle(color = Color.White, radius = 8f, center = Offset(item.x, item.y), style = Stroke(width = 1.5f))
                }
            }

            // 4. Thrown Grenades + Warning Blast Rings
            world.grenades.forEach { g ->
                drawCircle(
                    color = CrimsonAlert.copy(alpha = 0.15f),
                    radius = 175f,
                    center = Offset(g.x, g.y)
                )
                drawCircle(
                    color = CrimsonAlert,
                    radius = 175f,
                    center = Offset(g.x, g.y),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )
                drawCircle(color = BlazeOrange, radius = 8f, center = Offset(g.x, g.y))
            }

            // 5. Shockwaves
            world.shockwaves.forEach { sw ->
                val progress = 1f - (sw.life / sw.maxLife).coerceIn(0f, 1f)
                val r = sw.maxRadius * progress
                drawCircle(
                    color = sw.color.copy(alpha = (1f - progress) * 0.7f),
                    radius = r,
                    center = Offset(sw.x, sw.y),
                    style = Stroke(width = 5f)
                )
            }

            // 6. Obstacles (Buildings, Crates, Rocks, Gloo Walls)
            world.obstacles.forEach { obs ->
                when (obs.type) {
                    ObstacleType.BUILDING_WALL -> {
                        drawRect(
                            color = Color(0xFF141820),
                            topLeft = Offset(obs.x + 4f, obs.y + 5f),
                            size = Size(obs.width, obs.height)
                        )
                        drawRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height)
                        )
                        drawRect(
                            color = Color(0xFF94A3B8),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height),
                            style = Stroke(width = 1.5f)
                        )
                    }
                    ObstacleType.CRATE -> {
                        drawRoundRect(
                            color = Color(0xFF785233),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color(0xFFB08256),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2.5f)
                        )
                        drawLine(
                            color = Color(0xFF5A3C22),
                            start = Offset(obs.x + 4f, obs.y + 4f),
                            end = Offset(obs.x + obs.width - 4f, obs.y + obs.height - 4f),
                            strokeWidth = 3f
                        )
                    }
                    ObstacleType.ROCK -> {
                        drawRoundRect(
                            color = Color(0xFF475569),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(18f, 18f)
                        )
                        drawRoundRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(obs.x + 4f, obs.y + 4f),
                            size = Size(obs.width - 8f, obs.height - 10f),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                    }
                    ObstacleType.GLOO_WALL -> {
                        val hpFrac = (obs.hp / obs.maxHp).coerceIn(0f, 1f)
                        drawRoundRect(
                            color = ShieldCyan.copy(alpha = 0.35f + hpFrac * 0.45f),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = Color(0xFFE0F7FA),
                            topLeft = Offset(obs.x, obs.y),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }
            }

            // 7. Projectiles / Tracers
            world.projectiles.forEach { proj ->
                drawLine(
                    color = proj.color.copy(alpha = 0.85f),
                    start = Offset(proj.prevX, proj.prevY),
                    end = Offset(proj.x, proj.y),
                    strokeWidth = if (proj.isHeadshot) 4.5f else 3f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color.White, radius = 3f, center = Offset(proj.x, proj.y))
            }

            // 8. Survivors (Player & Bots)
            if (world.matchPhase != MatchPhase.AIRDROP_PLANE) {
                world.survivors.filter { it.isAlive }.forEach { s ->
                    drawSurvivor(
                        s = s,
                        isLockedTarget = s.id == world.lockedTargetId,
                        isParachuting = s.isPlayer && world.matchPhase == MatchPhase.PARACHUTING,
                        altitude = world.parachuteAltitude
                    )
                }
            }

            // 9. Foliage Canopies (translucent cover over units)
            world.foliage.forEach { f ->
                drawCircle(
                    color = if (f.isTree) Color(0xAA1B4327) else Color(0x99255632),
                    radius = f.radius,
                    center = Offset(f.x, f.y)
                )
                drawCircle(
                    color = Color(0x66408250),
                    radius = f.radius * 0.65f,
                    center = Offset(f.x - f.radius * 0.16f, f.y - f.radius * 0.16f)
                )
            }

            // 10. Particles
            world.particles.forEach { p ->
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * alpha,
                    center = Offset(p.x, p.y)
                )
            }

            // 11. Shrinking Safe Zone & Storm Overlay
            if (world.mode != com.example.game.GameMode.TRAINING) {
                val stormPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(-400f, -400f, worldSize + 400f, worldSize + 400f))
                    addOval(
                        Rect(
                            left = world.zone.centerX - world.zone.currentRadius,
                            top = world.zone.centerY - world.zone.currentRadius,
                            right = world.zone.centerX + world.zone.currentRadius,
                            bottom = world.zone.centerY + world.zone.currentRadius
                        )
                    )
                }
                drawPath(path = stormPath, color = Color(0x447E22CE))

                // Current Safe Zone circle
                drawCircle(
                    color = ZoneCyan,
                    radius = world.zone.currentRadius,
                    center = Offset(world.zone.centerX, world.zone.centerY),
                    style = Stroke(width = 4f)
                )
                // Next Zone dashed target circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = world.zone.targetRadius,
                    center = Offset(world.zone.targetCenterX, world.zone.targetCenterY),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )
            }

            // 12. Drop Plane Silhouette when in AIRDROP_PLANE phase
            if (world.matchPhase == MatchPhase.AIRDROP_PLANE) {
                drawDropPlane(world.planeX, world.planeY, world.planeDirX, world.planeDirY)
            }

            // 13. Floating Damage / Loot Numbers
            world.floatingTexts.forEach { ft ->
                val alpha = (ft.life / 0.8f).coerceIn(0f, 1f)
                textPaint.textSize = if (ft.isCritical) 24f else 17f
                textPaint.color = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    0, 0, 0
                )
                drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x + 1.5f, ft.y + 1.5f, textPaint)
                textPaint.color = ft.color.copy(alpha = alpha).toArgb()
                drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x, ft.y, textPaint)
            }
        }
    }
}

private fun DrawScope.drawSurvivor(
    s: Survivor,
    isLockedTarget: Boolean,
    isParachuting: Boolean,
    altitude: Float
) {
    val center = Offset(s.x, s.y)

    // Active skill aura
    if (s.skillActiveLeft > 0f) {
        when (s.characterClass) {
            CharacterClass.PULSE -> {
                drawCircle(color = BlazeAmber.copy(alpha = 0.18f), radius = 78f, center = center)
                drawCircle(color = BlazeAmber, radius = 78f, center = center, style = Stroke(width = 2.5f))
            }
            CharacterClass.AEGIS -> {
                drawCircle(color = ShieldCyan.copy(alpha = 0.24f), radius = 44f, center = center)
                drawCircle(color = ShieldCyan, radius = 44f, center = center, style = Stroke(width = 3.5f))
            }
            CharacterClass.VIPER -> {
                drawCircle(color = BlazeOrange.copy(alpha = 0.22f), radius = 34f, center = center)
            }
            else -> {}
        }
    }

    // Target lock reticle for auto-aim feedback
    if (isLockedTarget) {
        drawCircle(
            color = CrimsonAlert,
            radius = s.radius + 11f,
            center = center,
            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
        )
    }

    // Ground shadow
    drawCircle(color = Color(0x55000000), radius = s.radius + 3f, center = Offset(s.x + 3f, s.y + 5f))

    // Gun barrel & muzzle flash
    val barrelLen = if (s.activeSlot.weapon.shortTag == "AWM") 36f else 28f
    val tipX = s.x + cos(s.aimAngle) * barrelLen
    val tipY = s.y + sin(s.aimAngle) * barrelLen
    drawLine(
        color = Color(0xFFCBD5E1),
        start = center,
        end = Offset(tipX, tipY),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )
    if (s.muzzleFlashTimer > 0f) {
        drawCircle(color = BlazeGold, radius = 11f, center = Offset(tipX, tipY))
        drawCircle(color = Color.White, radius = 5f, center = Offset(tipX, tipY))
    }

    // Body circle
    val bodyColor = if (s.isPlayer) s.accentColor else Color(0xFFDC2626)
    drawCircle(color = bodyColor, radius = s.radius, center = center)
    drawCircle(
        color = if (s.isPlayer) Color.White else Color(0xFFFCA5A5),
        radius = s.radius,
        center = center,
        style = Stroke(width = if (s.isPlayer) 3f else 2f)
    )

    // Helmet ring
    if (s.helmetLevel > 0) {
        drawCircle(
            color = if (s.helmetLevel >= 2) LootEpic else LootRare,
            radius = s.radius * 0.55f,
            center = center
        )
    } else {
        drawCircle(color = Color(0xFF1E293B), radius = s.radius * 0.48f, center = center)
    }

    // Parachute canopy while dropping
    if (isParachuting) {
        val chuteScale = 0.7f + (altitude / 400f) * 0.6f
        drawArc(
            color = BlazeOrange,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = true,
            topLeft = Offset(s.x - 36f * chuteScale, s.y - 54f * chuteScale),
            size = Size(72f * chuteScale, 52f * chuteScale)
        )
        drawArc(
            color = BlazeGold,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(s.x - 36f * chuteScale, s.y - 54f * chuteScale),
            size = Size(72f * chuteScale, 52f * chuteScale),
            style = Stroke(width = 3f)
        )
    }

    // Enemy HP bar when damaged
    if (!s.isPlayer && s.hp < s.maxHp) {
        val barW = 38f
        val hpFrac = (s.hp / s.maxHp).coerceIn(0f, 1f)
        drawRect(
            color = Color(0xAA000000),
            topLeft = Offset(s.x - barW / 2f, s.y - s.radius - 12f),
            size = Size(barW, 5f)
        )
        drawRect(
            color = CrimsonAlert,
            topLeft = Offset(s.x - barW / 2f, s.y - s.radius - 12f),
            size = Size(barW * hpFrac, 5f)
        )
    }
}

private fun DrawScope.drawDropPlane(x: Float, y: Float, dx: Float, dy: Float) {
    val angleDeg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    withTransform({
        translate(x, y)
        rotate(angleDeg, pivot = Offset.Zero)
    }) {
        // Plane shadow
        drawOval(
            color = Color(0x44000000),
            topLeft = Offset(-55f, 35f),
            size = Size(110f, 28f)
        )
        // Wings
        drawRect(
            color = Color(0xFF475569),
            topLeft = Offset(-18f, -72f),
            size = Size(34f, 144f)
        )
        // Fuselage
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(-64f, -15f),
            size = Size(128f, 30f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        // Blaze Orange tail stripe
        drawRect(
            color = BlazeOrange,
            topLeft = Offset(-58f, -14f),
            size = Size(18f, 28f)
        )
    }
}

@Composable
fun MinimapCanvas(
    world: BattleWorldState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val mapScale = min(size.width, size.height) / world.worldSize
        // Island background
        drawRect(color = Color(0xFF1A251D))

        // Floors / buildings
        world.floors.forEach { f ->
            drawRect(
                color = Color(0xFF475569),
                topLeft = Offset(f.x * mapScale, f.y * mapScale),
                size = Size(f.width * mapScale, f.height * mapScale)
            )
        }

        // Zone rings
        if (world.mode != com.example.game.GameMode.TRAINING) {
            drawCircle(
                color = ZoneCyan,
                radius = world.zone.currentRadius * mapScale,
                center = Offset(world.zone.centerX * mapScale, world.zone.centerY * mapScale),
                style = Stroke(width = 1.8f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = world.zone.targetRadius * mapScale,
                center = Offset(world.zone.targetCenterX * mapScale, world.zone.targetCenterY * mapScale),
                style = Stroke(width = 1.2f)
            )
        }

        // Airdrops on radar
        world.loot.filter { it.kind == LootKind.AIRDROP_CRATE }.forEach { drop ->
            drawCircle(
                color = BlazeGold,
                radius = 4f,
                center = Offset(drop.x * mapScale, drop.y * mapScale)
            )
        }

        // Firing enemy blips (enemies shooting recently)
        world.survivors.filter { !it.isPlayer && it.isAlive && it.muzzleFlashTimer > 0f }.forEach { enemy ->
            drawCircle(
                color = CrimsonAlert,
                radius = 3.5f,
                center = Offset(enemy.x * mapScale, enemy.y * mapScale)
            )
        }

        // Player dot + look direction
        val player = world.player
        if (player != null && player.isAlive) {
            val px = player.x * mapScale
            val py = player.y * mapScale
            drawLine(
                color = BlazeAmber,
                start = Offset(px, py),
                end = Offset(px + cos(player.aimAngle) * 10f, py + sin(player.aimAngle) * 10f),
                strokeWidth = 2f
            )
            drawCircle(color = Color.White, radius = 4.2f, center = Offset(px, py))
            drawCircle(color = BlazeOrange, radius = 2.6f, center = Offset(px, py))
        }
    }
}
