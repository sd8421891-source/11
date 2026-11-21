package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.game.BattleWorldState
import com.example.game.GameMode
import com.example.game.MatchPhase
import com.example.game.Survivor
import com.example.game.WeaponSlotState
import com.example.ui.theme.BlazeAmber
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EpAmber
import com.example.ui.theme.HpGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ShieldCyan
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZoneCyan
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun BattleScreen(
    world: BattleWorldState,
    isPaused: Boolean,
    isScoped: Boolean,
    showExpandedMap: Boolean,
    onTick: (Float, Long) -> Unit,
    onMoveInput: (Float, Float, Boolean) -> Unit,
    onAimFireInput: (Float, Float, Boolean, Boolean) -> Unit,
    onEjectPlane: () -> Unit,
    onReload: () -> Unit,
    onSwitchWeapon: (Boolean) -> Unit,
    onUseMedkit: () -> Unit,
    onDeployGloo: () -> Unit,
    onThrowGrenade: () -> Unit,
    onActivateSkill: () -> Unit,
    onToggleScope: () -> Unit,
    onPickupLoot: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleExpandedMap: () -> Unit,
    onPlayAgain: () -> Unit,
    onExitToLobby: () -> Unit
) {
    BackHandler {
        if (world.matchPhase == MatchPhase.VICTORY_BOOYAH || world.matchPhase == MatchPhase.ELIMINATED) {
            onExitToLobby()
        } else if (showExpandedMap) {
            onToggleExpandedMap()
        } else {
            onTogglePause()
        }
    }

    // 60FPS Frame Loop
    LaunchedEffect(isPaused) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    onTick(dt, System.currentTimeMillis())
                }
                lastNanos = frameNanos
            }
        }
    }

    val player = world.player

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Live 60FPS Tactical Battlefield Canvas
        BattleWorldCanvas(
            world = world,
            isScoped = isScoped,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Safe-area HUD Overlay
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val isCompactWidth = maxWidth < 520.dp

            // TOP HUD ROW: Minimap (Left), Alive/Kills (Center), Kill Feed + Pause (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top-Left: Radar Minimap & Zone Status
                Column(horizontalAlignment = Alignment.Start) {
                    val mapSize = if (isCompactWidth) 84.dp else 108.dp
                    Box(
                        modifier = Modifier
                            .size(mapSize)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleExpandedMap)
                            .testTag("minimap_button")
                    ) {
                        MinimapCanvas(world = world, modifier = Modifier.fillMaxSize())
                        Text(
                            text = "MAP",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color(0x88000000))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (world.mode != GameMode.TRAINING && player != null) {
                        val inZone = hypot(
                            player.x - world.zone.centerX,
                            player.y - world.zone.centerY
                        ) <= world.zone.currentRadius
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (inZone) TacticalSurface.copy(alpha = 0.85f)
                                    else CrimsonAlert.copy(alpha = 0.85f)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (inZone) ZoneCyan else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (world.zone.isShrinking) "SHRINKING ${world.zone.phaseTimerSec.toInt()}s"
                                else "ZONE ${world.zone.phaseTimerSec.toInt()}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-Center: ALIVE & KILLS Counters
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalSurface.copy(alpha = 0.86f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.hud_alive),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextSecondary
                            )
                            Text(
                                text = "${world.aliveCount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = BlazeAmber
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(BorderSubtle)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.hud_kills),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = TextSecondary
                            )
                            Text(
                                text = "${player?.kills ?: 0}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                    }

                    // Dynamic Match Announcement
                    if (world.announcementBanner != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = world.announcementBanner,
                            style = MaterialTheme.typography.labelSmall,
                            color = BlazeGold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xAA111318))
                                .border(1.dp, BlazeOrange.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Top-Right: Pause Button + Live Kill Feed
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = TacticalSurface.copy(alpha = 0.88f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePause,
                            modifier = Modifier.testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = "Pause Match",
                                tint = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    world.killFeed.forEach { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (entry.involvesPlayer) BlazeOrange.copy(alpha = 0.85f)
                                    else Color(0xAA0F141D)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${entry.killerName} [${entry.weaponTag}] ${entry.victimName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // AIRDROP PLANE JUMP OVERLAY
            if (world.matchPhase == MatchPhase.AIRDROP_PLANE) {
                Button(
                    onClick = onEjectPlane,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                        .height(56.dp)
                        .testTag("eject_drop_button")
                ) {
                    Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.hud_eject_drop),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }

            // PARACHUTING ALTITUDE BAR + LEFT STEER JOYSTICK
            if (world.matchPhase == MatchPhase.PARACHUTING) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurface.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.hud_landing)} ${world.parachuteAltitude.toInt()}m",
                        style = MaterialTheme.typography.labelLarge,
                        color = BlazeAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (world.parachuteAltitude / 400f).coerceIn(0f, 1f) },
                        color = BlazeOrange,
                        trackColor = CardSurface,
                        modifier = Modifier.width(150.dp)
                    )
                }
            }

            // ACTIVE COMBAT & PARACHUTE CONTROLS
            if ((world.matchPhase == MatchPhase.COMBAT || world.matchPhase == MatchPhase.PARACHUTING) && player != null) {
                // LEFT SIDE: Movement Joystick + Quick Medkit / Frag / Loot Pickup
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    if (world.matchPhase == MatchPhase.COMBAT) {
                        // Manual Loot Pickup Prompt
                        if (world.nearbyLoot != null) {
                            Surface(
                                color = BlazeAmber,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clickable(onClick = onPickupLoot)
                                    .testTag("pickup_loot_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF1A1000), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = world.nearbyLoot.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF1A1000)
                                    )
                                }
                            }
                        }

                        // Medkit + Frag Row above left joystick
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            TacticalActionChip(
                                icon = Icons.Filled.LocalHospital,
                                label = "${player.medkits}",
                                accent = HpGreen,
                                enabled = player.medkits > 0 && player.hp < player.maxHp,
                                onClick = onUseMedkit,
                                tag = "medkit_button"
                            )
                            TacticalActionChip(
                                icon = Icons.Filled.Whatshot,
                                label = "${player.grenades}",
                                accent = BlazeOrange,
                                enabled = player.grenades > 0,
                                onClick = onThrowGrenade,
                                tag = "grenade_button"
                            )
                        }
                    }

                    VirtualMovementJoystick(
                        onMove = onMoveInput,
                        modifier = Modifier.size(if (isCompactWidth) 124.dp else 142.dp)
                    )
                }

                // BOTTOM CENTER: Player Vitals (HP, EP, Armor) + 2-Slot Weapon Switcher
                if (world.matchPhase == MatchPhase.COMBAT) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp)
                            .width(if (isCompactWidth) 148.dp else 240.dp)
                    ) {
                        // Reload / Heal Channel Progress Bar
                        if (player.isReloading || player.isHealing) {
                            val label = if (player.isReloading) stringResource(R.string.hud_reloading)
                            else stringResource(R.string.hud_healing)
                            val frac = if (player.isReloading) {
                                1f - (player.reloadTimer / player.reloadTotal).coerceIn(0f, 1f)
                            } else {
                                1f - (player.healTimer / 1.4f).coerceIn(0f, 1f)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xBB0B0D12))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelSmall, color = BlazeGold)
                                LinearProgressIndicator(
                                    progress = { frac },
                                    color = if (player.isReloading) BlazeAmber else HpGreen,
                                    trackColor = CardSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Weapon Slots Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WeaponSlotPill(
                                slot = player.primarySlot,
                                isSelected = player.usePrimary && player.primarySlot != null,
                                onClick = { onSwitchWeapon(true) },
                                modifier = Modifier.weight(1f),
                                tag = "weapon_slot_primary"
                            )
                            WeaponSlotPill(
                                slot = player.secondarySlot,
                                isSelected = !player.usePrimary || player.primarySlot == null,
                                onClick = { onSwitchWeapon(false) },
                                modifier = Modifier.weight(1f),
                                tag = "weapon_slot_secondary"
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Armor + EP + HP Vitals Card
                        PlayerVitalsBar(player = player)
                    }

                    // RIGHT SIDE: Skill, Gloo Wall, Scope, Reload + Fire/Aim Pad
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            // Active Character Skill Button
                            val cd = player.skillCooldownLeft
                            val skillReady = cd <= 0f
                            TacticalCircleButton(
                                icon = Icons.Filled.Security,
                                label = if (skillReady) player.characterClass.skillName.take(5).uppercase() else "${cd.toInt()}s",
                                color = if (skillReady) player.characterClass.accentColor else TextSecondary,
                                active = player.skillActiveLeft > 0f,
                                onClick = onActivateSkill,
                                tag = "skill_button"
                            )

                            // Gloo Wall Button
                            TacticalCircleButton(
                                icon = Icons.Filled.Shield,
                                label = "${player.glooWalls}",
                                color = if (player.glooWalls > 0) ShieldCyan else TextSecondary,
                                active = false,
                                onClick = onDeployGloo,
                                tag = "gloo_wall_button"
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                TacticalCircleButton(
                                    icon = Icons.Filled.CenterFocusStrong,
                                    label = "2X",
                                    color = if (isScoped) BlazeGold else TextPrimary,
                                    active = isScoped,
                                    onClick = onToggleScope,
                                    tag = "scope_button"
                                )
                                TacticalCircleButton(
                                    icon = Icons.Filled.Refresh,
                                    label = "RLD",
                                    color = TextPrimary,
                                    active = player.isReloading,
                                    onClick = onReload,
                                    tag = "reload_button"
                                )
                            }

                            FireAimJoystick(
                                onAimFire = onAimFireInput,
                                modifier = Modifier.size(if (isCompactWidth) 114.dp else 132.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Pause Dialog
        if (isPaused) {
            Dialog(onDismissRequest = onTogglePause) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.pause_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onTogglePause,
                            colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("resume_button")
                        ) {
                            Text(
                                text = stringResource(R.string.pause_resume),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onExitToLobby,
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("exit_lobby_button")
                        ) {
                            Text(
                                text = stringResource(R.string.pause_exit),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. Expanded Tactical Island Map Modal
        if (showExpandedMap) {
            Dialog(onDismissRequest = onToggleExpandedMap) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.5.dp, BlazeOrange),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TACTICAL MAP · ${world.mode.title}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            IconButton(onClick = onToggleExpandedMap) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Map", tint = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            MinimapCanvas(world = world, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        // 5. BOOYAH! / ELIMINATED Match Result Overlay
        if (world.matchPhase == MatchPhase.VICTORY_BOOYAH || world.matchPhase == MatchPhase.ELIMINATED) {
            val isBooyah = world.matchPhase == MatchPhase.VICTORY_BOOYAH
            val coinsEarned = (if (isBooyah) 150 else 35) + (player?.kills ?: 0) * 20

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD907090D)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
                    border = BorderStroke(1.5.dp, if (isBooyah) BlazeGold else CrimsonAlert),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .testTag("match_result_card")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (isBooyah) R.drawable.img_booyah_banner else R.drawable.img_lobby_hero
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0x44000000), TacticalSurface)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isBooyah) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = BlazeGold,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Text(
                                    text = if (isBooyah) stringResource(R.string.result_booyah)
                                    else stringResource(R.string.result_eliminated),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = if (isBooyah) BlazeGold else CrimsonAlert
                                )
                                Text(
                                    text = "RANK #${world.placement} / ${world.totalCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ResultStatColumn("KILLS", "${player?.kills ?: 0}", BlazeOrange)
                                ResultStatColumn("DAMAGE", "${player?.damageDealt ?: 0}", BlazeAmber)
                                ResultStatColumn("HEADSHOTS", "${player?.headshots ?: 0}", CrimsonAlert)
                                ResultStatColumn("COINS", "+$coinsEarned", BlazeGold)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onExitToLobby,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("result_lobby_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.result_return_lobby),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                }
                                Button(
                                    onClick = onPlayAgain,
                                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(50.dp)
                                        .testTag("result_play_again_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.result_play_again),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualMovementJoystick(
    onMove: (Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x55141821))
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
            .pointerInput(Unit) {
                val maxRadiusPx = with(density) { 46.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            knobOffset = Offset.Zero
                            onMove(0f, 0f, false)
                            break
                        }
                        change.consume()
                        val delta = change.position - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadiusPx && dist > 0f) {
                            delta * (maxRadiusPx / dist)
                        } else delta
                        knobOffset = clamped
                        val normX = clamped.x / maxRadiusPx
                        val normY = clamped.y / maxRadiusPx
                        val sprinting = dist >= maxRadiusPx * 0.88f
                        onMove(normX, normY, sprinting)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(BlazeAmber.copy(alpha = 0.9f), BlazeOrange.copy(alpha = 0.85f))
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
        )
    }
}

@Composable
private fun FireAimJoystick(
    onAimFire: (Float, Float, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isFiring by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isFiring) BlazeOrange.copy(alpha = 0.35f) else Color(0x55141821))
            .border(
                2.dp,
                if (isFiring) BlazeGold else BlazeOrange.copy(alpha = 0.75f),
                CircleShape
            )
            .pointerInput(Unit) {
                val maxRadiusPx = with(density) { 42.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    isFiring = true
                    onAimFire(0f, 0f, false, true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            knobOffset = Offset.Zero
                            isFiring = false
                            onAimFire(0f, 0f, false, false)
                            break
                        }
                        change.consume()
                        val delta = change.position - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadiusPx && dist > 0f) {
                            delta * (maxRadiusPx / dist)
                        } else delta
                        knobOffset = clamped
                        val manualAim = dist > maxRadiusPx * 0.25f
                        onAimFire(
                            clamped.x / maxRadiusPx,
                            clamped.y / maxRadiusPx,
                            manualAim,
                            true
                        )
                    }
                }
            }
            .testTag("fire_button"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CrimsonAlert, BlazeOrange)
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.GpsFixed,
                contentDescription = "Fire Weapon",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TacticalCircleButton(
    icon: ImageVector,
    label: String,
    color: Color,
    active: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (active) color.copy(alpha = 0.28f) else TacticalSurface.copy(alpha = 0.88f))
            .border(1.5.dp, if (active) color else BorderSubtle, CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun TacticalActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurface.copy(alpha = if (enabled) 0.9f else 0.45f))
            .border(1.dp, if (enabled) accent else BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tag,
            tint = if (enabled) accent else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) TextPrimary else TextSecondary
        )
    }
}

@Composable
private fun WeaponSlotPill(
    slot: WeaponSlotState?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    val border = if (isSelected) BlazeAmber else BorderSubtle
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CardSurface.copy(alpha = 0.95f) else TacticalSurface.copy(alpha = 0.80f))
            .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = slot != null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center
    ) {
        if (slot == null) {
            Text(
                text = "EMPTY",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        } else {
            Text(
                text = slot.weapon.shortTag,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = slot.weapon.rarity.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${slot.ammoInMag}/${slot.reserveAmmo}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun PlayerVitalsBar(player: Survivor) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (player.armorLevel > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalPolice, contentDescription = null, tint = ShieldCyan, modifier = Modifier.size(12.dp))
                        Text(
                            text = "V${player.armorLevel}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ShieldCyan
                        )
                    }
                }
                if (player.helmetLevel > 0) {
                    Text(
                        text = "H${player.helmetLevel}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = BlazeAmber
                    )
                }
            }
            Text(
                text = "HP ${player.hp.toInt()}/${player.maxHp.toInt()}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        // EP bar
        LinearProgressIndicator(
            progress = { (player.ep / player.maxEp).coerceIn(0f, 1f) },
            color = EpAmber,
            trackColor = Color(0xFF1F2430),
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(3.dp))
        // HP bar
        val hpFrac = (player.hp / player.maxHp).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { hpFrac },
            color = if (hpFrac > 0.35f) Color.White else CrimsonAlert,
            trackColor = Color(0xFF2B1B1D),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ResultStatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
