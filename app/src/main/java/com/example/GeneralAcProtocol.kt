package com.example

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

enum class AcMode {
    AUTO, COOL, DRY, FAN, HEAT
}

enum class FanSpeed {
    AUTO, QUIET, LOW, MEDIUM, HIGH
}

data class AcState(
    val power: Boolean,
    val temperature: Int,
    val mode: AcMode,
    val fan: FanSpeed,
    val swing: Boolean
)

/**
 * Stateful protocol generator for Fujitsu General Air Conditioner IR remote (AR-RCE3E / AR-RAH1E).
 * Ported faithfully from standard open-source reverse-engineered specifications.
 */
class GeneralAcProtocol(private val context: Context? = null) {

    private val tag = "GeneralAcProtocol"

    companion object {
        const val CARRIER_FREQUENCY_HZ = 38000

        // Timing constants in microseconds (clearly marked for physical blaster validation)
        const val HDR_MARK = 3320
        const val HDR_SPACE = 1570
        const val BIT_MARK = 430
        const val ONE_SPACE = 1180
        const val ZERO_SPACE = 380
        const val STOP_MARK = 430
        const val MIN_GAP = 8000 // Trailing frame space

        // Protocol Header Constants (MSB representation)
        const val CUSTOM_CODE_BYTE_0 = 0xA4
        const val CUSTOM_CODE_BYTE_1 = 0x4D
        const val CUSTOM_CODE_BYTE_2 = 0x25 // Stateful long packet custom byte
        const val CUSTOM_CODE_BYTE_3 = 0x01 // Command offset
        const val CUSTOM_CODE_BYTE_4 = 0x4D // Device specific identifier
        
        const val CMD_LONG_PACKET = 0xFE
        const val CMD_STRUCTURE = 0x09 // Sub-model/command structure
        
        // Sum-subtraction checksum offset constant - clearly marked for validation
        const val CHECKSUM_OFFSET_LONG = 0x4D 
        const val CHECKSUM_OFFSET_SHORT = 0x4D

        /**
         * Creates a default instance without Context (for packet building/testing).
         */
        fun createDefaultInstance(): GeneralAcProtocol {
            return GeneralAcProtocol(null)
        }
    }

    /**
     * Builds the complete stateful IR packet from state.
     * Returns a 16-byte array for long stateful packets, or 8-byte array if powering off (if model-specific).
     */
    fun buildPacket(state: AcState): ByteArray {
        // If power is false (turning OFF), AR-RCE3E/AR-RAH1E remotes can send a short 8-byte power command
        if (!state.power) {
            return buildShortPowerPacket()
        }

        val packet = ByteArray(16)
        
        // Bytes 0..4: Header (Custom Code / Manufacturer ID)
        packet[0] = CUSTOM_CODE_BYTE_0.toByte()
        packet[1] = CUSTOM_CODE_BYTE_1.toByte()
        packet[2] = CUSTOM_CODE_BYTE_2.toByte()
        packet[3] = CUSTOM_CODE_BYTE_3.toByte()
        packet[4] = CUSTOM_CODE_BYTE_4.toByte()
        
        // Byte 5: Long packet command type
        packet[5] = CMD_LONG_PACKET.toByte()
        
        // Byte 6: Command structure code
        packet[6] = CMD_STRUCTURE.toByte()
        
        // Byte 7: Power & Mode
        // Mode bits: AUTO=0, COOL=1, DRY=2, FAN=3, HEAT=4
        val modeVal = when (state.mode) {
            AcMode.AUTO -> 0x00
            AcMode.COOL -> 0x01
            AcMode.DRY -> 0x02
            AcMode.FAN -> 0x03
            AcMode.HEAT -> 0x04
        }
        // Power bit is bit 3 (0x08) when ON
        val powerVal = if (state.power) 0x08 else 0x00
        packet[7] = (modeVal or powerVal).toByte()
        
        // Byte 8: Temperature (Temp - 16 offset in lower 4 bits)
        val tempClamped = state.temperature.coerceIn(16, 30)
        packet[8] = (tempClamped - 16).toByte()
        
        // Byte 9: Fan Speed & Swing
        // Fan speed bits: AUTO=0, HIGH=1, MEDIUM=2, LOW=3, QUIET=4
        val fanVal = when (state.fan) {
            FanSpeed.AUTO -> 0x00
            FanSpeed.HIGH -> 0x01
            FanSpeed.MEDIUM -> 0x02
            FanSpeed.LOW -> 0x03
            FanSpeed.QUIET -> 0x04
        }
        // Swing bit: vertical swing is 0x10 (bit 4) when enabled
        val swingVal = if (state.swing) 0x10 else 0x00
        packet[9] = (fanVal or swingVal).toByte()
        
        // Bytes 10..14: Padding and static offsets
        packet[10] = 0x00.toByte()
        packet[11] = 0x00.toByte()
        packet[12] = 0x00.toByte()
        packet[13] = 0x00.toByte()
        packet[14] = 0x00.toByte()
        
        // Byte 15: Checksum
        packet[15] = calculateChecksum(packet)
        
        return packet
    }

    /**
     * Helper to build the classic 8-byte Power Off command.
     */
    private fun buildShortPowerPacket(): ByteArray {
        val packet = ByteArray(8)
        packet[0] = 0xA4.toByte()
        packet[1] = 0x4D.toByte()
        packet[2] = 0x84.toByte()
        packet[3] = 0x01.toByte()
        packet[4] = 0x4D.toByte()
        packet[5] = 0x90.toByte()
        packet[6] = 0x19.toByte()
        packet[7] = calculateChecksumShort(packet)
        return packet
    }

    /**
     * Calculates the additive 8-bit checksum for 16-byte long packet.
     */
    fun calculateChecksum(bytes: ByteArray): Byte {
        var sum = 0
        for (i in 0..14) {
            sum += bytes[i].toInt() and 0xFF
        }
        return ((CHECKSUM_OFFSET_LONG - sum) and 0xFF).toByte()
    }

    /**
     * Calculates the additive 8-bit checksum for 8-byte short packet.
     */
    private fun calculateChecksumShort(bytes: ByteArray): Byte {
        var sum = 0
        for (i in 0..6) {
            sum += bytes[i].toInt() and 0xFF
        }
        return ((CHECKSUM_OFFSET_SHORT - sum) and 0xFF).toByte()
    }

    /**
     * Encodes bytes into raw pulses. Delegates to buildRawPulses as required.
     */
    fun encodeBytes(bytes: ByteArray): IntArray {
        return buildRawPulses(bytes)
    }

    /**
     * Builds raw pulse array (mark/space transitions in microseconds) LSB-first.
     */
    fun buildRawPulses(bytes: ByteArray): IntArray {
        val list = mutableListOf<Int>()
        
        // 1. Header (Mark and Space)
        list.add(HDR_MARK)
        list.add(HDR_SPACE)
        
        // 2. Data Bytes (transmitted LSB-first)
        for (b in bytes) {
            val byteVal = b.toInt() and 0xFF
            for (bitIdx in 0..7) {
                val bit = (byteVal ushr bitIdx) and 1
                list.add(BIT_MARK)
                if (bit == 1) {
                    list.add(ONE_SPACE)
                } else {
                    list.add(ZERO_SPACE)
                }
            }
        }
        
        // 3. Stop Bit (Mark and trailing frame space)
        list.add(STOP_MARK)
        list.add(MIN_GAP)
        
        return list.toIntArray()
    }

    /**
     * Formats pulses for ConsumerIrManager. Direct pass-through.
     */
    fun toConsumerIrPattern(pulses: IntArray): IntArray {
        return pulses
    }

    /**
     * Transmits the stateful IR command via ConsumerIrManager.
     */
    fun send(state: AcState) {
        val packet = buildPacket(state)
        val pulses = encodeBytes(packet)
        val pattern = toConsumerIrPattern(pulses)
        
        val irManager = context?.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        if (irManager?.hasIrEmitter() == true) {
            try {
                Log.d(tag, "Transmitting via ConsumerIrManager: State=${state}")
                irManager.transmit(CARRIER_FREQUENCY_HZ, pattern)
            } catch (e: Exception) {
                Log.e(tag, "Failed to transmit IR signal", e)
            }
        } else {
            Log.d(tag, "[SIMULATION] Transmitted IR signal: State=${state}, PacketSize=${packet.size} bytes")
        }
    }
}
