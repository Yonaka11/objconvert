package com.mikazuki.pocketfamiliar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikazuki.pocketfamiliar.model.PetSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pet_settings")

/**
 * Persistent storage for user settings using Jetpack DataStore Preferences.
 * Call [settingsFlow] to observe changes; call the update functions to write new values.
 */
class PetSettingsRepository(private val context: Context) {

    private object Keys {
        val PET_SIZE = floatPreferencesKey("pet_size")
        val MOVEMENT_SPEED = floatPreferencesKey("movement_speed")
        val SLEEP_ENABLED = booleanPreferencesKey("sleep_enabled")
        val SELECTED_PET_ID = stringPreferencesKey("selected_pet_id")
    }

    /** Emits the current [PetSettings] and re-emits whenever any value changes. */
    val settingsFlow: Flow<PetSettings> = context.dataStore.data.map { prefs ->
        PetSettings(
            petSize = prefs[Keys.PET_SIZE] ?: PetSettings.DEFAULT.petSize,
            movementSpeed = prefs[Keys.MOVEMENT_SPEED] ?: PetSettings.DEFAULT.movementSpeed,
            sleepEnabled = prefs[Keys.SLEEP_ENABLED] ?: PetSettings.DEFAULT.sleepEnabled,
            selectedPetId = prefs[Keys.SELECTED_PET_ID] ?: PetSettings.DEFAULT.selectedPetId,
        )
    }

    suspend fun updatePetSize(size: Float) {
        context.dataStore.edit { it[Keys.PET_SIZE] = size.coerceIn(PetSettings.PET_SIZE_MIN, PetSettings.PET_SIZE_MAX) }
    }

    suspend fun updateMovementSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.MOVEMENT_SPEED] = speed.coerceIn(PetSettings.SPEED_MIN, PetSettings.SPEED_MAX) }
    }

    suspend fun updateSleepEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SLEEP_ENABLED] = enabled }
    }

    suspend fun updateSelectedPetId(id: String) {
        context.dataStore.edit { it[Keys.SELECTED_PET_ID] = id }
    }

    suspend fun updateAll(settings: PetSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PET_SIZE] = settings.petSize.coerceIn(PetSettings.PET_SIZE_MIN, PetSettings.PET_SIZE_MAX)
            prefs[Keys.MOVEMENT_SPEED] = settings.movementSpeed.coerceIn(PetSettings.SPEED_MIN, PetSettings.SPEED_MAX)
            prefs[Keys.SLEEP_ENABLED] = settings.sleepEnabled
            prefs[Keys.SELECTED_PET_ID] = settings.selectedPetId
        }
    }
}
