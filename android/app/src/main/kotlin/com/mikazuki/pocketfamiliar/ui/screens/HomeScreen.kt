package com.mikazuki.pocketfamiliar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.model.BatteryMood
import com.mikazuki.pocketfamiliar.model.BatteryState
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.ui.components.SettingSlider
import com.mikazuki.pocketfamiliar.ui.theme.PocketFamiliarTheme
import com.mikazuki.pocketfamiliar.viewmodel.HomeViewModel
import com.mikazuki.pocketfamiliar.viewmodel.HomeViewModelFactory

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current.applicationContext)),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val overlayGranted by vm.overlayPermissionGranted.collectAsStateWithLifecycle()
    val petRunning by vm.petRunning.collectAsStateWithLifecycle()
    val batteryState by vm.batteryState.collectAsStateWithLifecycle()

    HomeScreenContent(
        settings = settings,
        overlayGranted = overlayGranted,
        petRunning = petRunning,
        batteryState = batteryState,
        onStartPet = { vm.startPet() },
        onStopPet = { vm.stopPet() },
        onPetSizeChange = { vm.updatePetSize(it) },
        onSpeedChange = { vm.updateMovementSpeed(it) },
        onSleepToggle = { vm.updateSleepEnabled(it) },
        onGrantOverlayPermission = { vm.openOverlayPermissionSettings() },
        modifier = modifier,
    )
}

@Composable
private fun HomeScreenContent(
    settings: PetSettings,
    overlayGranted: Boolean,
    petRunning: Boolean,
    batteryState: BatteryState,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit,
    onPetSizeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSleepToggle: (Boolean) -> Unit,
    onGrantOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Header ─────────────────────────────────────────────────────
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Pet preview ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_pet_placeholder),
                        contentDescription = "Pet preview",
                        modifier = Modifier.size(96.dp),
                    )
                    // Status badge
                    if (petRunning) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }

            // ── Overlay permission ─────────────────────────────────────────
            PermissionStatusRow(
                granted = overlayGranted,
                onGrant = onGrantOverlayPermission,
            )

            // ── Controls ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStartPet,
                    enabled = overlayGranted && !petRunning,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.label_start_pet))
                }
                OutlinedButton(
                    onClick = onStopPet,
                    enabled = petRunning,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.label_stop_pet))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Settings ───────────────────────────────────────────────────
            SettingSlider(
                label = stringResource(R.string.label_pet_size),
                value = settings.petSize,
                valueRange = PetSettings.PET_SIZE_MIN..PetSettings.PET_SIZE_MAX,
                onValueChange = onPetSizeChange,
                valueLabel = "%.1f×".format(settings.petSize),
            )

            SettingSlider(
                label = stringResource(R.string.label_movement_speed),
                value = settings.movementSpeed,
                valueRange = PetSettings.SPEED_MIN..PetSettings.SPEED_MAX,
                onValueChange = onSpeedChange,
                valueLabel = "%.1f×".format(settings.movementSpeed),
            )

            SettingRow(
                label = stringResource(R.string.label_enable_sleep),
                control = {
                    Switch(
                        checked = settings.sleepEnabled,
                        onCheckedChange = onSleepToggle,
                    )
                }
            )

            SettingRow(
                label = stringResource(R.string.label_auto_start),
                control = {
                    Switch(
                        checked = false,
                        onCheckedChange = {},
                        enabled = false,
                    )
                },
                caption = "Coming in a future release",
            )

            SettingRow(
                label = stringResource(R.string.label_pet_selector),
                control = {},
                caption = "Default Ghost — more coming soon",
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Battery info ───────────────────────────────────────────────
            BatteryInfoRow(batteryState = batteryState)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun PermissionStatusRow(
    granted: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (granted)
                        stringResource(R.string.overlay_permission_granted)
                    else
                        stringResource(R.string.overlay_permission_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (granted)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                )
                AnimatedVisibility(
                    visible = !granted,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(R.string.overlay_permission_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (!granted) {
                FilledTonalButton(
                    onClick = onGrant,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.btn_grant_overlay_permission))
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    control: @Composable () -> Unit,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        control()
    }
}

@Composable
private fun BatteryInfoRow(
    batteryState: BatteryState,
    modifier: Modifier = Modifier,
) {
    val moodLabel = when (batteryState.mood) {
        BatteryMood.EXCITED -> "Excited (charging!)"
        BatteryMood.HAPPY -> "Happy"
        BatteryMood.NORMAL -> "Normal"
        BatteryMood.TIRED -> "Tired"
        BatteryMood.WORRIED -> "Worried"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Battery",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${batteryState.levelPercent}% · " +
                    (if (batteryState.isCharging) "Charging" else "Not charging") +
                    " · Familiar mood: $moodLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PocketFamiliarTheme {
        HomeScreenContent(
            settings = PetSettings(),
            overlayGranted = false,
            petRunning = false,
            batteryState = BatteryState(levelPercent = 72, isCharging = false),
            onStartPet = {},
            onStopPet = {},
            onPetSizeChange = {},
            onSpeedChange = {},
            onSleepToggle = {},
            onGrantOverlayPermission = {},
        )
    }
}
