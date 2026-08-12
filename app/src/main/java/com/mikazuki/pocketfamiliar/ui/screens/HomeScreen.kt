package com.mikazuki.pocketfamiliar.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikazuki.pocketfamiliar.model.AppBrand
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onGrantOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var petSize by rememberSaveable { mutableFloatStateOf(96f) }
    var movementSpeed by rememberSaveable { mutableFloatStateOf(1f) }
    var sleepEnabled by rememberSaveable { mutableStateOf(true) }
    var startAfterReboot by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Header()
        PetPreview()
        PermissionCard(
            overlayPermissionGranted = overlayPermissionGranted,
            notificationPermissionGranted = notificationPermissionGranted,
            onGrantOverlayPermission = onGrantOverlayPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
        )
        ControlsCard(
            petSize = petSize,
            onPetSizeChange = { petSize = it },
            movementSpeed = movementSpeed,
            onMovementSpeedChange = { movementSpeed = it },
            sleepEnabled = sleepEnabled,
            onSleepEnabledChange = { sleepEnabled = it },
            startAfterReboot = startAfterReboot,
            onStartAfterRebootChange = { startAfterReboot = it },
            canStartPet = overlayPermissionGranted && notificationPermissionGranted,
            onStartPet = onStartPet,
            onStopPet = onStopPet,
        )
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = AppBrand.appName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = AppBrand.subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PetPreview() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp),
                contentAlignment = Alignment.Center,
            ) {
                PlaceholderPet(modifier = Modifier.size(96.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Starter familiar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Static original placeholder art for the first overlay test. Sprite animation comes next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    overlayPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onGrantOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            StatusLine(
                label = "Overlay permission",
                granted = overlayPermissionGranted,
            )
            if (!overlayPermissionGranted) {
                Button(onClick = onGrantOverlayPermission) {
                    Text("Grant Overlay Permission")
                }
            }
            StatusLine(
                label = "Notification permission",
                granted = notificationPermissionGranted,
            )
            if (!notificationPermissionGranted) {
                OutlinedButton(onClick = onRequestNotificationPermission) {
                    Text("Grant Notification Permission")
                }
            }
        }
    }
}

@Composable
private fun ControlsCard(
    petSize: Float,
    onPetSizeChange: (Float) -> Unit,
    movementSpeed: Float,
    onMovementSpeedChange: (Float) -> Unit,
    sleepEnabled: Boolean,
    onSleepEnabledChange: (Boolean) -> Unit,
    startAfterReboot: Boolean,
    onStartAfterRebootChange: (Boolean) -> Unit,
    canStartPet: Boolean,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Main controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStartPet,
                    enabled = canStartPet,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start Pet")
                }
                OutlinedButton(
                    onClick = onStopPet,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Stop Pet")
                }
            }
            SliderRow(
                label = "Pet size",
                value = petSize,
                valueLabel = "${petSize.roundToInt()} px",
                range = 64f..160f,
                onValueChange = onPetSizeChange,
            )
            SliderRow(
                label = "Movement speed",
                value = movementSpeed,
                valueLabel = "${"%.1f".format(movementSpeed)}x",
                range = 0.5f..2f,
                onValueChange = onMovementSpeedChange,
            )
            ToggleRow(
                label = "Enable sleep behavior",
                checked = sleepEnabled,
                onCheckedChange = onSleepEnabledChange,
            )
            ToggleRow(
                label = "Start automatically after reboot",
                supportingText = "Placeholder for a later boot receiver.",
                checked = startAfterReboot,
                onCheckedChange = onStartAfterRebootChange,
            )
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pet selector: Starter familiar")
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (granted) "Granted" else "Missing",
            color = if (granted) Color(0xFF78D99E) else Color(0xFFFFB86B),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (supportingText != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PlaceholderPet(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val body = Color(0xFFB8A7FF)
        val accent = Color(0xFF8DCCFF)
        val line = Color(0xFF32215F)
        val blush = Color(0xFFFF9FBE)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawOval(
            color = body,
            topLeft = Offset(size.width * 0.18f, size.height * 0.24f),
            size = Size(size.width * 0.64f, size.height * 0.62f),
        )
        drawCircle(accent, radius = size.minDimension * 0.16f, center = Offset(size.width * 0.36f, size.height * 0.28f))
        drawCircle(accent, radius = size.minDimension * 0.16f, center = Offset(size.width * 0.64f, size.height * 0.28f))
        drawCircle(line, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.4f, size.height * 0.5f))
        drawCircle(line, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.6f, size.height * 0.5f))
        drawCircle(blush, radius = size.minDimension * 0.045f, center = Offset(size.width * 0.32f, size.height * 0.6f))
        drawCircle(blush, radius = size.minDimension * 0.045f, center = Offset(size.width * 0.68f, size.height * 0.6f))

        val smile = Path().apply {
            moveTo(center.x - size.width * 0.08f, center.y + size.height * 0.11f)
            quadraticTo(center.x, center.y + size.height * 0.18f, center.x + size.width * 0.08f, center.y + size.height * 0.11f)
        }
        drawPath(smile, color = line, style = Stroke(width = size.minDimension * 0.025f))
    }
}
