package com.mikazuki.pocketfamiliar.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.service.PetOverlayService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetSettingsRepository(application)

    val settings: StateFlow<PetSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PetSettings()
    )

    fun updateSettings(updated: PetSettings) {
        viewModelScope.launch { repository.saveSettings(updated) }
    }

    fun startPet() {
        val intent = Intent(getApplication(), PetOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopPet() {
        val intent = Intent(getApplication(), PetOverlayService::class.java)
        getApplication<Application>().stopService(intent)
    }
}
