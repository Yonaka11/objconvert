package com.mikazuki.pocketfamiliar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikazuki.pocketfamiliar.service.PetOverlayService
import com.mikazuki.pocketfamiliar.ui.PetViewModel
import com.mikazuki.pocketfamiliar.ui.screens.HomeScreen
import com.mikazuki.pocketfamiliar.ui.theme.PocketFamiliarTheme
import com.mikazuki.pocketfamiliar.util.OverlayPermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: PetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PocketFamiliarTheme {
                val settings by viewModel.settings.collectAsStateWithLifecycle()

                // Re-check overlay permission and pet active state on every resume
                var hasOverlayPermission by remember {
                    mutableStateOf(OverlayPermissionHelper.hasPermission(this))
                }
                var isPetActive by remember {
                    mutableStateOf(PetOverlayService.isRunning)
                }

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasOverlayPermission = OverlayPermissionHelper.hasPermission(this@MainActivity)
                            isPetActive = PetOverlayService.isRunning
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                HomeScreen(
                    isPetActive = isPetActive,
                    hasOverlayPermission = hasOverlayPermission,
                    settings = settings,
                    onGrantPermissionClick = { openOverlayPermissionSettings() },
                    onStartPet = {
                        viewModel.startPet()
                        isPetActive = true
                    },
                    onStopPet = {
                        viewModel.stopPet()
                        isPetActive = false
                    },
                    onSettingsChanged = viewModel::updateSettings
                )
            }
        }
    }

    private fun openOverlayPermissionSettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }
}
