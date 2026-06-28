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
    val POWER: IntArray
        get() {
            val protocol = GeneralAcProtocol.createDefaultInstance()
            // Set power = false to generate the standard 8-byte Power Off/Toggle packet
            val state = AcState(power = false, temperature = 24, mode = AcMode.COOL, fan = FanSpeed.AUTO, swing = false)
            return protocol.encodeBytes(protocol.buildPacket(state))
        }

    // --- Temperature Commands (16°C to 30°C) ---
    val TEMP_16: IntArray get() = getTempCode(16)
    val TEMP_17: IntArray get() = getTempCode(17)
    val TEMP_18: IntArray get() = getTempCode(18)
    val TEMP_19: IntArray get() = getTempCode(19)
    val TEMP_20: IntArray get() = getTempCode(20)
    val TEMP_21: IntArray get() = getTempCode(21)
    val TEMP_22: IntArray get() = getTempCode(22)
    val TEMP_23: IntArray get() = getTempCode(23)
    val TEMP_24: IntArray get() = getTempCode(24)
    val TEMP_25: IntArray get() = getTempCode(25)
    val TEMP_26: IntArray get() = getTempCode(26)
    val TEMP_27: IntArray get() = getTempCode(27)
    val TEMP_28: IntArray get() = getTempCode(28)
    val TEMP_29: IntArray get() = getTempCode(29)
    val TEMP_30: IntArray get() = getTempCode(30)

    // --- Mode Commands ---
    val MODE_AUTO: IntArray get() = getModePattern(AcMode.AUTO)
    val MODE_COOL: IntArray get() = getModePattern(AcMode.COOL)
    val MODE_DRY: IntArray get() = getModePattern(AcMode.DRY)
    val MODE_FAN: IntArray get() = getModePattern(AcMode.FAN)
    val MODE_HEAT: IntArray get() = getModePattern(AcMode.HEAT)

    // --- Fan Speed Commands ---
    val FAN_AUTO: IntArray get() = getFanPattern(FanSpeed.AUTO)
    val FAN_LOW: IntArray get() = getFanPattern(FanSpeed.LOW)
    val FAN_MEDIUM: IntArray get() = getFanPattern(FanSpeed.MEDIUM)
    val FAN_HIGH: IntArray get() = getFanPattern(FanSpeed.HIGH)

    // --- Swing Commands ---
    val SWING_ON: IntArray get() = getSwingPattern(true)
    val SWING_OFF: IntArray get() = getSwingPattern(false)

    // --- Turbo and Sleep Commands ---
    val TURBO: IntArray
        get() {
            val protocol = GeneralAcProtocol.createDefaultInstance()
            val state = AcState(power = true, temperature = 18, mode = AcMode.COOL, fan = FanSpeed.HIGH, swing = false)
            return protocol.encodeBytes(protocol.buildPacket(state))
        }
    val SLEEP: IntArray
        get() {
            val protocol = GeneralAcProtocol.createDefaultInstance()
            val state = AcState(power = true, temperature = 24, mode = AcMode.COOL, fan = FanSpeed.QUIET, swing = false)
            return protocol.encodeBytes(protocol.buildPacket(state))
        }
    
    // --- Timer Command ---
    val TIMER: IntArray get() = POWER

    /**
     * Get temperature code by value.
     */
    fun getTempCode(temp: Int): IntArray {
        val protocol = GeneralAcProtocol.createDefaultInstance()
        val state = AcState(power = true, temperature = temp, mode = AcMode.COOL, fan = FanSpeed.AUTO, swing = false)
        return protocol.encodeBytes(protocol.buildPacket(state))
    }

    private fun getModePattern(mode: AcMode): IntArray {
        val protocol = GeneralAcProtocol.createDefaultInstance()
        val state = AcState(power = true, temperature = 24, mode = mode, fan = FanSpeed.AUTO, swing = false)
        return protocol.encodeBytes(protocol.buildPacket(state))
    }

    private fun getFanPattern(fan: FanSpeed): IntArray {
        val protocol = GeneralAcProtocol.createDefaultInstance()
        val state = AcState(power = true, temperature = 24, mode = AcMode.COOL, fan = fan, swing = false)
        return protocol.encodeBytes(protocol.buildPacket(state))
    }

    private fun getSwingPattern(swing: Boolean): IntArray {
        val protocol = GeneralAcProtocol.createDefaultInstance()
        val state = AcState(power = true, temperature = 24, mode = AcMode.COOL, fan = FanSpeed.AUTO, swing = swing)
        return protocol.encodeBytes(protocol.buildPacket(state))
    }
}
