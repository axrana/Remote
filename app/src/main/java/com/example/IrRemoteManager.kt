package com.example

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Abstraction layer for managing infrared transmissions on Android devices.
 * Uses ConsumerIrManager API, and provides seamless virtual simulation mode
 * when running on devices without a physical IR Blaster.
 */
class IrRemoteManager(private val context: Context) {

    private val tag = "IrRemoteManager"
    private val irManager: ConsumerIrManager? = 
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    // Coroutine scope for running transmissions on IO dispatcher
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // Flow to emit events to the UI, e.g. for toast messages or logging transmitted codes
    private val _transmissionEvents = MutableSharedFlow<String>()
    val transmissionEvents: SharedFlow<String> = _transmissionEvents.asSharedFlow()

    /**
     * Checks if the device has a physical infrared transmitter.
     */
    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() == true
    }

    /**
     * Gets the carrier frequencies supported by the device's IR emitter.
     */
    fun getSupportedFrequencies(): String {
        if (!hasIrEmitter()) return "N/A (Simulation Mode)"
        val ranges = irManager?.carrierFrequencies ?: return "None"
        return ranges.joinToString { "${it.minFrequency / 1000}kHz - ${it.maxFrequency / 1000}kHz" }
    }

    /**
     * Transmits a raw pattern at a specific carrier frequency.
     * All transmission executes asynchronously on [Dispatchers.IO] to avoid UI stutters.
     */
    fun transmit(pattern: IntArray, carrierFrequency: Int = GeneralAcIrCodes.CARRIER_FREQUENCY_HZ) {
        ioScope.launch {
            if (pattern.isEmpty()) {
                Log.w(tag, "Attempted to transmit an empty pulse array.")
                return@launch
            }

            if (hasIrEmitter() && irManager != null) {
                try {
                    Log.d(tag, "Transmitting IR pattern on ${carrierFrequency}Hz, pattern size: ${pattern.size}")
                    irManager.transmit(carrierFrequency, pattern)
                    _transmissionEvents.emit("IR Transmitted (${pattern.size} pulses)")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to transmit IR code", e)
                    _transmissionEvents.emit("Transmission Error: ${e.message}")
                }
            } else {
                // Simulation mode
                Log.d(tag, "[SIMULATION] Transmitting IR pattern on ${carrierFrequency}Hz, size: ${pattern.size}")
                _transmissionEvents.emit("Simulated IR Burst (${pattern.size} pulses)")
            }
        }
    }

    /**
     * Sends the Power ON/OFF command.
     */
    fun sendPower() {
        transmit(GeneralAcIrCodes.POWER)
    }

    /**
     * Sends the Temperature set command.
     */
    fun sendTemperature(temp: Int) {
        val pattern = GeneralAcIrCodes.getTempCode(temp)
        transmit(pattern)
    }

    /**
     * Sends the Mode select command.
     * Accepted values: "Auto", "Cool", "Dry", "Fan", "Heat"
     */
    fun sendMode(mode: String) {
        val pattern = when (mode.lowercase()) {
            "auto" -> GeneralAcIrCodes.MODE_AUTO
            "cool" -> GeneralAcIrCodes.MODE_COOL
            "dry" -> GeneralAcIrCodes.MODE_DRY
            "fan" -> GeneralAcIrCodes.MODE_FAN
            "heat" -> GeneralAcIrCodes.MODE_HEAT
            else -> GeneralAcIrCodes.MODE_COOL
        }
        transmit(pattern)
    }

    /**
     * Sends the Fan speed command.
     * Accepted values: "Auto", "Low", "Medium", "High"
     */
    fun sendFan(speed: String) {
        val pattern = when (speed.lowercase()) {
            "auto" -> GeneralAcIrCodes.FAN_AUTO
            "low" -> GeneralAcIrCodes.FAN_LOW
            "medium" -> GeneralAcIrCodes.FAN_MEDIUM
            "high" -> GeneralAcIrCodes.FAN_HIGH
            else -> GeneralAcIrCodes.FAN_AUTO
        }
        transmit(pattern)
    }

    /**
     * Sends the Swing toggle command.
     */
    fun sendSwing(enable: Boolean) {
        val pattern = if (enable) GeneralAcIrCodes.SWING_ON else GeneralAcIrCodes.SWING_OFF
        transmit(pattern)
    }

    /**
     * Sends the Turbo boost command.
     */
    fun sendTurbo() {
        transmit(GeneralAcIrCodes.TURBO)
    }

    /**
     * Sends the Sleep mode command.
     */
    fun sendSleep() {
        transmit(GeneralAcIrCodes.SLEEP)
    }
    
    /**
     * Sends the Timer command.
     */
    fun sendTimer() {
        transmit(GeneralAcIrCodes.TIMER)
    }
}
