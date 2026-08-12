package com.mikazuki.pocketfamiliar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mikazuki.pocketfamiliar.service.PetOverlayService
import com.mikazuki.pocketfamiliar.ui.screens.HomeScreen
import com.mikazuki.pocketfamiliar.ui.theme.PocketFamiliarTheme
import com.mikazuki.pocketfamiliar.util.OverlayPermission

class MainActivity : ComponentActivity() {
    private var overlayPermissionGranted = androidx.compose.runtime.mutableStateOf(false)
    private var notificationPermissionGranted = androidx.compose.runtime.mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionGranted.value = granted || !requiresNotificationPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()

        setContent {
            PocketFamiliarTheme {
                HomeScreen(
                    overlayPermissionGranted = overlayPermissionGranted.value,
                    notificationPermissionGranted = notificationPermissionGranted.value,
                    onGrantOverlayPermission = ::openOverlayPermissionSettings,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onStartPet = ::startPet,
                    onStopPet = ::stopPet,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        overlayPermissionGranted.value = OverlayPermission.isGranted(this)
        notificationPermissionGranted.value = hasNotificationPermission()
    }

    private fun openOverlayPermissionSettings() {
        startActivity(OverlayPermission.settingsIntent(this))
    }

    private fun requestNotificationPermission() {
        if (!requiresNotificationPermission()) {
            notificationPermissionGranted.value = true
            return
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startPet() {
        refreshPermissionState()
        if (!overlayPermissionGranted.value) {
            openOverlayPermissionSettings()
            return
        }
        if (!notificationPermissionGranted.value) {
            requestNotificationPermission()
            return
        }

        ContextCompat.startForegroundService(
            this,
            PetOverlayService.startIntent(this),
        )
    }

    private fun stopPet() {
        startService(PetOverlayService.stopIntent(this))
    }

    private fun hasNotificationPermission(): Boolean =
        !requiresNotificationPermission() ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun requiresNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
