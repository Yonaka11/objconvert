package com.mikazuki.pocketfamiliar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mikazuki.pocketfamiliar.model.PetSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.petSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pet_settings"
)

/**
 * Persists pet configuration using DataStore Preferences.
 *
 * Reads are exposed as a [Flow] so the UI can react to changes.
 * Writes are suspend functions meant to be called from a coroutine.
 */
class PetSettingsRepository(private val context: Context) {

    companion object {
        val KEY_PET_SIZE = floatPreferencesKey("pet_size_dp")
        val KEY_SPEED = floatPreferencesKey("speed_multiplier")
        val KEY_SLEEP_ENABLED = booleanPreferencesKey("sleep_enabled")
        val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val KEY_SELECTED_PET = stringPreferencesKey("selected_pet_id")
    }

    val settingsFlow: Flow<PetSettings> = context.petSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            PetSettings(
                petSizeDp = prefs[KEY_PET_SIZE] ?: 80f,
                speedMultiplier = prefs[KEY_SPEED] ?: 1.0f,
                sleepEnabled = prefs[KEY_SLEEP_ENABLED] ?: true,
                startOnBoot = prefs[KEY_START_ON_BOOT] ?: false,
                selectedPetId = prefs[KEY_SELECTED_PET] ?: "default"
            )
        }

    suspend fun saveSettings(settings: PetSettings) {
        context.petSettingsDataStore.edit { prefs ->
            prefs[KEY_PET_SIZE] = settings.petSizeDp
            prefs[KEY_SPEED] = settings.speedMultiplier
            prefs[KEY_SLEEP_ENABLED] = settings.sleepEnabled
            prefs[KEY_START_ON_BOOT] = settings.startOnBoot
            prefs[KEY_SELECTED_PET] = settings.selectedPetId
        }
    }
}
