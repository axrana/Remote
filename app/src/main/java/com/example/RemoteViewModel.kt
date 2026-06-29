package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for O General AC Remote application.
 * Manages reactive state flow, interacts with persistence, and controls the IR blaster.
 */
class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = RemoteSettingsDataStore(application)
    val irRemoteManager = IrRemoteManager(application)

    init {
        viewModelScope.launch {
            dataStore.settingsFlow.collect { settings ->
                irRemoteManager.setModel(settings.model)
            }
        }
    }

    // Collect settings from DataStore and convert to a hot StateFlow for the UI
    val uiState: StateFlow<RemoteSettingsDataStore.AcSettings> = dataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RemoteSettingsDataStore.AcSettings(
                powerState = false,
                temperature = 24,
                mode = "Cool",
                fanSpeed = "Auto",
                swingState = false,
                turboState = false,
                sleepState = false,
                timerHours = 0,
                model = "ARRAH2E"
            )
        )

    // SharedFlow to forward IR blaster event messages to the Compose snackbar or toasts
    val feedbackEvents: SharedFlow<String> = irRemoteManager.transmissionEvents

    /**
     * Toggles AC Power State.
     */
    fun togglePower() {
        val current = uiState.value.powerState
        val next = !current
        viewModelScope.launch {
            dataStore.savePowerState(next)
            irRemoteManager.sendPower()
        }
    }

    /**
     * Increments temperature. (Max 30°C)
     */
    fun incrementTemperature() {
        val current = uiState.value.temperature
        if (current < 30) {
            val next = current + 1
            viewModelScope.launch {
                dataStore.saveTemperature(next)
                irRemoteManager.sendTemperature(next)
            }
        }
    }

    /**
     * Decrements temperature. (Min 16°C)
     */
    fun decrementTemperature() {
        val current = uiState.value.temperature
        if (current > 16) {
            val next = current - 1
            viewModelScope.launch {
                dataStore.saveTemperature(next)
                irRemoteManager.sendTemperature(next)
            }
        }
    }

    /**
     * Set Operation Mode.
     * Accepted values: "Auto", "Cool", "Dry", "Fan", "Heat"
     */
    fun setMode(mode: String) {
        viewModelScope.launch {
            dataStore.saveMode(mode)
            irRemoteManager.sendMode(mode)
            
            // Auto/Fan modes might disable or lock sleep/turbo. Reset them or let them toggle
            if (mode.lowercase() == "fan" || mode.lowercase() == "dry") {
                if (uiState.value.turboState) dataStore.saveTurboState(false)
            }
        }
    }

    /**
     * Set Fan Speed.
     * Accepted values: "Auto", "Low", "Medium", "High"
     */
    fun setFanSpeed(speed: String) {
        viewModelScope.launch {
            dataStore.saveFanSpeed(speed)
            irRemoteManager.sendFan(speed)
        }
    }

    /**
     * Toggles L/R & U/D Swing movement.
     */
    fun toggleSwing() {
        val next = !uiState.value.swingState
        viewModelScope.launch {
            dataStore.saveSwingState(next)
            irRemoteManager.sendSwing(next)
        }
    }

    /**
     * Toggles Turbo super-cool mode.
     */
    fun toggleTurbo() {
        val currentMode = uiState.value.mode.lowercase()
        // Turbo is typically only available in Cool/Heat modes
        if (currentMode == "cool" || currentMode == "heat" || currentMode == "auto") {
            val next = !uiState.value.turboState
            viewModelScope.launch {
                dataStore.saveTurboState(next)
                if (next && uiState.value.sleepState) {
                    dataStore.saveSleepState(false) // Turbo usually cancels sleep
                }
                irRemoteManager.sendTurbo()
            }
        }
    }

    /**
     * Toggles eco Sleep/night mode.
     */
    fun toggleSleep() {
        val next = !uiState.value.sleepState
        viewModelScope.launch {
            dataStore.saveSleepState(next)
            if (next && uiState.value.turboState) {
                dataStore.saveTurboState(false) // Sleep cancels turbo
            }
            irRemoteManager.sendSleep()
        }
    }

    /**
     * Sets sleep timer (from 0 to 12 hours)
     */
    fun cycleTimer() {
        val current = uiState.value.timerHours
        val next = if (current >= 12) 0 else current + 1
        viewModelScope.launch {
            dataStore.saveTimerHours(next)
            irRemoteManager.sendTimer(next)
        }
    }

    /**
     * Sets the active remote model.
     */
    fun setModel(model: String) {
        viewModelScope.launch {
            dataStore.saveModel(model)
            irRemoteManager.setModel(model)
        }
    }
}
