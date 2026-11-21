package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.PlayerSettings
import com.example.game.CharacterClass
import com.example.game.GameMode
import com.example.ui.theme.BlazeAmber
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LobbyScreen(
    settings: PlayerSettings,
    onSelectCharacter: (CharacterClass) -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onStartMatch: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleAutoPickup: (Boolean) -> Unit,
    onToggleAimAssist: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Hero key-art background
        Image(
            painter = painterResource(id = R.drawable.img_lobby_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Atmospheric tactical gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.72f),
                            ObsidianBg.copy(alpha = 0.45f),
                            ObsidianBg.copy(alpha = 0.88f),
                            ObsidianBg.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val isWide = maxWidth >= 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 960.dp)
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Operator Identity + Stats + Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalSurface.copy(alpha = 0.85f))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_blaze_emblem),
                            contentDescription = "Operator Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, BlazeOrange, RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "STRIKER-01",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MilitaryTech,
                                    contentDescription = null,
                                    tint = BlazeAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.lobby_rank_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlazeAmber
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadgePill(
                            icon = Icons.Filled.EmojiEvents,
                            value = "${settings.careerBooyahs}",
                            tint = BlazeGold
                        )
                        if (isWide) {
                            StatBadgePill(
                                icon = Icons.Filled.GpsFixed,
                                value = "${settings.careerKills}",
                                tint = BlazeOrange
                            )
                        }
                        StatBadgePill(
                            icon = Icons.Filled.MonetizationOn,
                            value = "${settings.careerCoins}",
                            tint = BlazeAmber
                        )

                        Surface(
                            color = TacticalSurface.copy(alpha = 0.88f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.lobby_settings),
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Hero Callout
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BLAZE DROP",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (isWide) 48.sp else 40.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "DROP IN · GEAR UP · LAST SURVIVOR WINS BOOYAH",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlazeAmber,
                        letterSpacing = 1.4.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Operator / Character Skill Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.lobby_operator_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(CharacterClass.entries) { character ->
                            val isSelected = settings.selectedCharacter == character
                            OperatorCard(
                                character = character,
                                selected = isSelected,
                                onClick = { onSelectCharacter(character) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Operator Skill Detail Strip
                val activeChar = settings.selectedCharacter
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.90f)),
                    border = BorderStroke(1.dp, activeChar.accentColor.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeChar.accentColor.copy(alpha = 0.16f))
                                .border(1.dp, activeChar.accentColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconForCharacter(activeChar),
                                contentDescription = null,
                                tint = activeChar.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${activeChar.codename} · ${activeChar.skillName.uppercase()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CD ${activeChar.cooldownSec.toInt()}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeChar.accentColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(activeChar.accentColor.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeChar.skillDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector + Start Match CTA
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.lobby_mode_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GameMode.entries.forEach { mode ->
                            val selected = settings.selectedMode == mode
                            ModeChipCard(
                                mode = mode,
                                selected = selected,
                                onClick = { onSelectMode(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = settings.selectedMode.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onStartMatch,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("start_match_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(BlazeOrange, BlazeAmber)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(1.5.dp, BlazeGold.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF140B00),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.lobby_start_match),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF140B00),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            TacticalSettingsDialog(
                settings = settings,
                onDismiss = { showSettings = false },
                onToggleSound = onToggleSound,
                onToggleAutoPickup = onToggleAutoPickup,
                onToggleAimAssist = onToggleAimAssist,
                onSensitivityChange = onSensitivityChange
            )
        }
    }
}

@Composable
private fun StatBadgePill(
    icon: ImageVector,
    value: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurface.copy(alpha = 0.88f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
    }
}

@Composable
private fun OperatorCard(
    character: CharacterClass,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) character.accentColor else BorderSubtle,
        label = "operatorBorder"
    )
    val bgColor = if (selected) CardSurfaceElevated.copy(alpha = 0.95f) else CardSurface.copy(alpha = 0.82f)

    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("operator_${character.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(character.accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCharacter(character),
                    contentDescription = character.codename,
                    tint = character.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (selected) {
                Text(
                    text = "EQUIPPED",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = character.accentColor
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = character.codename,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = character.roleTitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModeChipCard(
    mode: GameMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) BlazeOrange else BorderSubtle
    val bg = if (selected) BlazeOrange.copy(alpha = 0.18f) else CardSurface.copy(alpha = 0.85f)
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag("mode_${mode.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) BlazeAmber else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TacticalSettingsDialog(
    settings: PlayerSettings,
    onDismiss: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleAutoPickup: (Boolean) -> Unit,
    onToggleAimAssist: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            border = BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow(
                    label = stringResource(R.string.settings_sound),
                    checked = settings.soundEnabled,
                    onCheckedChange = onToggleSound,
                    tag = "switch_sound"
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_auto_pickup),
                    checked = settings.autoPickupEnabled,
                    onCheckedChange = onToggleAutoPickup,
                    tag = "switch_auto_pickup"
                )
                SettingSwitchRow(
                    label = stringResource(R.string.settings_aim_assist),
                    checked = settings.aimAssistEnabled,
                    onCheckedChange = onToggleAimAssist,
                    tag = "switch_aim_assist"
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_sensitivity),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${(settings.aimSensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = BlazeAmber
                    )
                }
                Slider(
                    value = settings.aimSensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = BlazeOrange,
                        activeTrackColor = BlazeOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_done_button")
                ) {
                    Text(
                        text = stringResource(R.string.settings_close),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BlazeOrange
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

private fun iconForCharacter(character: CharacterClass): ImageVector {
    return when (character) {
        CharacterClass.PULSE -> Icons.Filled.Favorite
        CharacterClass.VIPER -> Icons.Filled.FlashOn
        CharacterClass.AEGIS -> Icons.Filled.Shield
        CharacterClass.BREACH -> Icons.Filled.Waves
    }
}
