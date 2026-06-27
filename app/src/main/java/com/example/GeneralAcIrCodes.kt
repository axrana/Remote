package com.example

/**
 * General/O General/Fujitsu General AC IR Commands and Code Definitions.
 * 
 * Carrier frequency is typically 38,000 Hz (38 kHz).
 * Each constant is a pulse array representing alternating ON and OFF periods in microseconds.
 *
 * TO USE EXTRACTED CODES:
 * Replace the dummy placeholder arrays (e.g., `intArrayOf(9000, 4500, ...)`) with actual
 * raw pulse durations captured or extracted from the original remote APK.
 */
object GeneralAcIrCodes {
    // Carrier frequency for General/Fujitsu AC Remotes
    const val CARRIER_FREQUENCY_HZ = 38000

    // --- Power Commands ---
    // TODO: Replace with real extracted POWER IR pulse array
    val POWER = intArrayOf(3320, 1570, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 380, 430, 380, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 380, 430, 1180, 430, 1180, 430, 1180, 430, 1180, 430, 380, 430, 380, 430, 380, 430, 380, 430, 1180, 430, 8000)

    // --- Temperature Commands (16°C to 30°C) ---
    // TODO: Replace with real extracted temperature IR pulse arrays
    val TEMP_16 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 1180)
    val TEMP_17 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380)
    val TEMP_18 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 380)
    val TEMP_19 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 1180)
    val TEMP_20 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 1180, 430, 380)
    val TEMP_21 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 1180, 430, 1180)
    val TEMP_22 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 380)
    val TEMP_23 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 1180)
    val TEMP_24 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380)
    val TEMP_25 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180)
    val TEMP_26 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 380)
    val TEMP_27 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 380, 430, 1180)
    val TEMP_28 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 1180, 430, 380)
    val TEMP_29 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 1180, 430, 1180, 430, 1180)
    val TEMP_30 = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380, 430, 1180, 430, 380, 430, 380, 430, 380)

    // --- Mode Commands ---
    // TODO: Replace with real extracted mode IR pulse arrays
    val MODE_AUTO = intArrayOf(3320, 1570, 430, 380, 430, 380, 430, 1180)
    val MODE_COOL = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 380)
    val MODE_DRY = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 1180)
    val MODE_FAN = intArrayOf(3320, 1570, 430, 1180, 430, 380, 430, 380)
    val MODE_HEAT = intArrayOf(3320, 1570, 430, 1180, 430, 380, 430, 1180)

    // --- Fan Speed Commands ---
    // TODO: Replace with real extracted fan speed IR pulse arrays
    val FAN_AUTO = intArrayOf(3320, 1570, 430, 380, 430, 380, 430, 380, 430, 380)
    val FAN_LOW = intArrayOf(3320, 1570, 430, 380, 430, 380, 430, 380, 430, 1180)
    val FAN_MEDIUM = intArrayOf(3320, 1570, 430, 380, 430, 380, 430, 1180, 430, 380)
    val FAN_HIGH = intArrayOf(3320, 1570, 430, 380, 430, 380, 430, 1180, 430, 1180)

    // --- Swing Commands ---
    // TODO: Replace with real extracted swing IR pulse arrays
    val SWING_ON = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 1180, 430, 380)
    val SWING_OFF = intArrayOf(3320, 1570, 430, 380, 430, 1180, 430, 1180, 430, 1180)

    // --- Turbo and Sleep Commands ---
    // TODO: Replace with real extracted Turbo and Sleep IR pulse arrays
    val TURBO = intArrayOf(3320, 1570, 430, 1180, 430, 1180, 430, 380, 430, 380)
    val SLEEP = intArrayOf(3320, 1570, 430, 1180, 430, 1180, 430, 380, 430, 1180)
    
    // --- Timer Command ---
    // TODO: Replace with real extracted Timer IR pulse array
    val TIMER = intArrayOf(3320, 1570, 430, 1180, 430, 1180, 430, 1180, 430, 380)

    /**
     * Get temperature code by value.
     */
    fun getTempCode(temp: Int): IntArray {
        return when (temp) {
            16 -> TEMP_16
            17 -> TEMP_17
            18 -> TEMP_18
            19 -> TEMP_19
            20 -> TEMP_20
            21 -> TEMP_21
            22 -> TEMP_22
            23 -> TEMP_23
            24 -> TEMP_24
            25 -> TEMP_25
            26 -> TEMP_26
            27 -> TEMP_27
            28 -> TEMP_28
            29 -> TEMP_29
            30 -> TEMP_30
            else -> TEMP_24 // fallback to default 24°C
        }
    }
}
