package com.mikazuki.pocketfamiliar.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.model.BatteryMood

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val battery by viewModel.batteryState.collectAsStateWithLifecycle()

    // Re-check overlay permission on every recomposition (user may have toggled it)
    var hasOverlayPermission by remember { mutableStateOf(viewModel.isOverlayPermissionGranted()) }
    LaunchedEffect(Unit) {
        hasOverlayPermission = viewModel.isOverlayPermissionGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Pet Preview ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_pet_idle),
                        contentDescription = "Pet preview",
                        modifier = Modifier.size(80.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.label_default_pet),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.label_select_pet),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "(coming soon)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Permission Status ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasOverlayPermission)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (hasOverlayPermission)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasOverlayPermission)
                            stringResource(R.string.label_overlay_permission_granted)
                        else
                            stringResource(R.string.label_overlay_permission_denied),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasOverlayPermission)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (!hasOverlayPermission) {
                    FilledTonalButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.btn_grant_permission))
                    }
                }
            }
        }

        // ── Main Controls ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    hasOverlayPermission = viewModel.isOverlayPermissionGranted()
                    if (hasOverlayPermission) viewModel.startPet()
                },
                enabled = hasOverlayPermission,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.btn_start_pet))
            }
            OutlinedButton(
                onClick = { viewModel.stopPet() },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.btn_stop_pet))
            }
        }

        HorizontalDivider()

        // ── Settings ─────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Pet Settings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Pet size
                SettingSlider(
                    label = stringResource(R.string.label_pet_size),
                    value = settings.petSize,
                    valueRange = 0.5f..2.0f,
                    displayValue = "×${"%.1f".format(settings.petSize)}",
                    onValueChangeFinished = viewModel::setPetSize,
                )

                // Movement speed
                SettingSlider(
                    label = stringResource(R.string.label_movement_speed),
                    value = settings.movementSpeed,
                    valueRange = 30f..200f,
                    displayValue = "${settings.movementSpeed.toInt()} px/s",
                    onValueChangeFinished = viewModel::setMovementSpeed,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Sleep toggle
                SettingToggle(
                    label = stringResource(R.string.label_sleep_behavior),
                    checked = settings.sleepEnabled,
                    onCheckedChange = viewModel::setSleepEnabled,
                )

                // Auto-start toggle
                SettingToggle(
                    label = stringResource(R.string.label_auto_start),
                    checked = settings.autoStartOnBoot,
                    onCheckedChange = viewModel::setAutoStartOnBoot,
                )
            }
        }

        // ── Battery Info ──────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.BatteryFull, contentDescription = null)
                Column {
                    Text(
                        stringResource(R.string.label_battery),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "${battery.levelPercent}% · ${batteryMoodLabel(battery.mood)}" +
                                if (battery.isCharging) " · Charging" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderValue by rememberSaveable { mutableStateOf(value) }

    // Keep in sync if settings reload from DataStore
    LaunchedEffect(value) { sliderValue = value }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(displayValue, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = valueRange,
            onValueChangeFinished = { onValueChangeFinished(sliderValue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun batteryMoodLabel(mood: BatteryMood): String = when (mood) {
    BatteryMood.HAPPY -> "Happy"
    BatteryMood.NORMAL -> "Normal"
    BatteryMood.TIRED -> "Tired"
    BatteryMood.SLEEPY -> "Sleepy"
    BatteryMood.CHARGING -> "Charging"
}
