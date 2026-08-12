package com.mikazuki.pocketfamiliar.viewmodel

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.BatteryState
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.service.PetOverlayService
import com.mikazuki.pocketfamiliar.util.BatteryMonitor
import com.mikazuki.pocketfamiliar.util.OverlayPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "HomeViewModel"

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val settingsRepo = PetSettingsRepository(context)
    private val batteryMonitor = BatteryMonitor(context)

    val settings: StateFlow<PetSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PetSettings.DEFAULT,
    )

    val batteryState: StateFlow<BatteryState> = batteryMonitor.state

    private val _overlayPermissionGranted = MutableStateFlow(OverlayPermission.isGranted(context))
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    private val _petRunning = MutableStateFlow(false)
    val petRunning: StateFlow<Boolean> = _petRunning.asStateFlow()

    init {
        batteryMonitor.start()
    }

    /** Re-check whether overlay permission has been granted. Call from onResume. */
    fun refreshPermissionState() {
        _overlayPermissionGranted.value = OverlayPermission.isGranted(context)
    }

    fun openOverlayPermissionSettings() {
        try {
            val intent = OverlayPermission.buildSettingsIntent(context).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Could not open overlay permission settings", e)
        }
    }

    fun startPet() {
        if (!OverlayPermission.isGranted(context)) {
            Log.w(TAG, "Cannot start pet: overlay permission not granted")
            return
        }
        context.startForegroundService(PetOverlayService.startIntent(context))
        _petRunning.value = true
    }

    fun stopPet() {
        context.startService(PetOverlayService.stopIntent(context))
        _petRunning.value = false
    }

    fun updatePetSize(size: Float) {
        viewModelScope.launch { settingsRepo.updatePetSize(size) }
    }

    fun updateMovementSpeed(speed: Float) {
        viewModelScope.launch { settingsRepo.updateMovementSpeed(speed) }
    }

    fun updateSleepEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateSleepEnabled(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.stop()
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(context.applicationContext as Application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
