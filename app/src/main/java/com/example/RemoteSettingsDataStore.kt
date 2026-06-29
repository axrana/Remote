package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore Extension on Context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ac_remote_settings")

/**
 * DataStore manager to persist and restore the state of the AC Remote control.
 */
class RemoteSettingsDataStore(private val context: Context) {

    companion object {
        val KEY_POWER_STATE = booleanPreferencesKey("power_state")
        val KEY_TEMPERATURE = intPreferencesKey("temperature")
        val KEY_MODE = stringPreferencesKey("mode")
        val KEY_FAN_SPEED = stringPreferencesKey("fan_speed")
        val KEY_SWING_STATE = booleanPreferencesKey("swing_state")
        val KEY_TURBO_STATE = booleanPreferencesKey("turbo_state")
        val KEY_SLEEP_STATE = booleanPreferencesKey("sleep_state")
        val KEY_TIMER_HOURS = intPreferencesKey("timer_hours")
        val KEY_MODEL = stringPreferencesKey("model")
    }

    /**
     * AC remote settings state model
     */
    data class AcSettings(
        val powerState: Boolean,
        val temperature: Int,
        val mode: String,
        val fanSpeed: String,
        val swingState: Boolean,
        val turboState: Boolean,
        val sleepState: Boolean,
        val timerHours: Int,
        val model: String
    )

    /**
     * Flows of setting configurations.
     */
    val settingsFlow: Flow<AcSettings> = context.dataStore.data.map { preferences ->
        AcSettings(
            powerState = preferences[KEY_POWER_STATE] ?: false,
            temperature = preferences[KEY_TEMPERATURE] ?: 24,
            mode = preferences[KEY_MODE] ?: "Cool",
            fanSpeed = preferences[KEY_FAN_SPEED] ?: "Auto",
            swingState = preferences[KEY_SWING_STATE] ?: false,
            turboState = preferences[KEY_TURBO_STATE] ?: false,
            sleepState = preferences[KEY_SLEEP_STATE] ?: false,
            timerHours = preferences[KEY_TIMER_HOURS] ?: 0,
            model = preferences[KEY_MODEL] ?: "ARRAH2E"
        )
    }

    // Individual setters
    suspend fun savePowerState(power: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POWER_STATE] = power
        }
    }

    suspend fun saveTemperature(temp: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TEMPERATURE] = temp
        }
    }

    suspend fun saveMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODE] = mode
        }
    }

    suspend fun saveFanSpeed(speed: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FAN_SPEED] = speed
        }
    }

    suspend fun saveSwingState(swing: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SWING_STATE] = swing
        }
    }

    suspend fun saveTurboState(turbo: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TURBO_STATE] = turbo
        }
    }

    suspend fun saveSleepState(sleep: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SLEEP_STATE] = sleep
        }
    }

    suspend fun saveTimerHours(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TIMER_HOURS] = hours
        }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODEL] = model
        }
    }
}
