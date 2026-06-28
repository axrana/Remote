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

    // Stateful protocol and state tracking
    private val protocol = GeneralAcProtocol(context)
    private var currentState = AcState(
        power = false,
        temperature = 24,
        mode = AcMode.COOL,
        fan = FanSpeed.AUTO,
        swing = false
    )

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
    fun transmit(pattern: IntArray, carrierFrequency: Int = GeneralAcProtocol.CARRIER_FREQUENCY_HZ) {
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
     * Helper to transmit the current state.
     */
    private fun transmitCurrentState() {
        val packet = protocol.buildPacket(currentState)
        val pulses = protocol.encodeBytes(packet)
        transmit(pulses)
    }

    /**
     * Sends the Power ON/OFF command.
     */
    fun sendPower() {
        currentState = currentState.copy(power = !currentState.power)
        transmitCurrentState()
    }

    /**
     * Sends the Temperature set command.
     */
    fun sendTemperature(temp: Int) {
        currentState = currentState.copy(temperature = temp)
        transmitCurrentState()
    }

    /**
     * Sends the Mode select command.
     * Accepted values: "Auto", "Cool", "Dry", "Fan", "Heat"
     */
    fun sendMode(mode: String) {
        val parsedMode = when (mode.lowercase()) {
            "auto" -> AcMode.AUTO
            "cool" -> AcMode.COOL
            "dry" -> AcMode.DRY
            "fan" -> AcMode.FAN
            "heat" -> AcMode.HEAT
            else -> AcMode.COOL
        }
        currentState = currentState.copy(mode = parsedMode)
        transmitCurrentState()
    }

    /**
     * Sends the Fan speed command.
     * Accepted values: "Auto", "Low", "Medium", "High"
     */
    fun sendFan(speed: String) {
        val parsedSpeed = when (speed.lowercase()) {
            "auto" -> FanSpeed.AUTO
            "low" -> FanSpeed.LOW
            "medium" -> FanSpeed.MEDIUM
            "high" -> FanSpeed.HIGH
            "quiet" -> FanSpeed.QUIET
            else -> FanSpeed.AUTO
        }
        currentState = currentState.copy(fan = parsedSpeed)
        transmitCurrentState()
    }

    /**
     * Sends the Swing toggle command.
     */
    fun sendSwing(enable: Boolean) {
        currentState = currentState.copy(swing = enable)
        transmitCurrentState()
    }

    /**
     * Sends the Turbo boost command.
     */
    fun sendTurbo() {
        currentState = currentState.copy(fan = FanSpeed.HIGH, temperature = 18)
        transmitCurrentState()
    }

    /**
     * Sends the Sleep mode command.
     */
    fun sendSleep() {
        currentState = currentState.copy(fan = FanSpeed.QUIET)
        transmitCurrentState()
    }
    
    /**
     * Sends the Timer command.
     */
    fun sendTimer() {
        transmitCurrentState()
    }
}
