package com.mikazuki.pocketfamiliar.ui.screens

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.model.PetSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isPetActive: Boolean,
    hasOverlayPermission: Boolean,
    settings: PetSettings,
    onGrantPermissionClick: () -> Unit,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit,
    onSettingsChanged: (PetSettings) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overlay permission status card
            PermissionCard(
                hasPermission = hasOverlayPermission,
                onGrantClick = onGrantPermissionClick
            )

            // Animated pet preview
            PetPreviewCard(isPetActive = isPetActive)

            // Start / Stop controls
            ControlsCard(
                isPetActive = isPetActive,
                hasOverlayPermission = hasOverlayPermission,
                onStartPet = onStartPet,
                onStopPet = onStopPet
            )

            // Pet settings
            SettingsCard(
                settings = settings,
                onSettingsChanged = onSettingsChanged
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Permission card
// ---------------------------------------------------------------------------

@Composable
private fun PermissionCard(
    hasPermission: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (hasPermission) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasPermission)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_overlay_permission),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasPermission)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (hasPermission)
                        stringResource(R.string.label_granted)
                    else
                        stringResource(R.string.label_not_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasPermission)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (!hasPermission) {
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.label_grant_permission))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pet preview
// ---------------------------------------------------------------------------

@Composable
private fun PetPreviewCard(isPetActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pet_idle")

    // Gentle breathing bob animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    // Subtle scale pulse while active
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPetActive) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_pet_placeholder),
                    contentDescription = "Pet preview",
                    modifier = Modifier
                        .size(120.dp)
                        .offset(y = offsetY.dp)
                        .scale(scale)
                )
                Text(
                    text = if (isPetActive)
                        stringResource(R.string.label_pet_active)
                    else
                        stringResource(R.string.label_pet_inactive),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Start / Stop controls
// ---------------------------------------------------------------------------

@Composable
private fun ControlsCard(
    isPetActive: Boolean,
    hasOverlayPermission: Boolean,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Controls",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartPet,
                    enabled = !isPetActive && hasOverlayPermission,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.label_start_pet))
                }

                OutlinedButton(
                    onClick = onStopPet,
                    enabled = isPetActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.label_stop_pet))
                }
            }

            if (!hasOverlayPermission) {
                Text(
                    text = "Grant overlay permission to start the pet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings card
// ---------------------------------------------------------------------------

@Composable
private fun SettingsCard(
    settings: PetSettings,
    onSettingsChanged: (PetSettings) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium
            )

            // Pet size slider
            LabeledSlider(
                label = stringResource(R.string.label_pet_size),
                value = settings.petSizeDp,
                valueRange = 40f..160f,
                displayValue = "${settings.petSizeDp.roundToInt()}dp",
                onValueChange = { onSettingsChanged(settings.copy(petSizeDp = it)) }
            )

            // Movement speed slider
            LabeledSlider(
                label = stringResource(R.string.label_movement_speed),
                value = settings.speedMultiplier,
                valueRange = 0.5f..2.5f,
                displayValue = "${(settings.speedMultiplier * 100).roundToInt()}%",
                onValueChange = { onSettingsChanged(settings.copy(speedMultiplier = it)) }
            )

            HorizontalDivider()

            // Sleep behavior toggle
            SettingsToggle(
                label = stringResource(R.string.label_sleep_behavior),
                description = "Your pet will occasionally fall asleep.",
                icon = Icons.Default.Bedtime,
                checked = settings.sleepEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(sleepEnabled = it)) }
            )

            // Start on boot toggle (placeholder)
            SettingsToggle(
                label = stringResource(R.string.label_start_on_boot),
                description = "Coming soon.",
                icon = Icons.Default.RestartAlt,
                checked = settings.startOnBoot,
                onCheckedChange = { onSettingsChanged(settings.copy(startOnBoot = it)) },
                enabled = false
            )

            HorizontalDivider()

            // Pet selector placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_select_pet),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Default Familiar — more coming soon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Reusable composables
// ---------------------------------------------------------------------------

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.6f
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
