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
    val swing: Boolean,
    val timerHours: Int = 0
)

/**
 * Stateful protocol generator for Fujitsu/O General Air Conditioner IR remote,
 * supporting multiple models: AR-RAH2E, AR-REB1E, AR-RY4, AR-REW4E, AR-DB1, and AR-JW2.
 * Faithful port of open-source reverse-engineered specs.
 */
class GeneralAcProtocol(private val context: Context? = null) {

    private val tag = "GeneralAcProtocol"

    companion object {
        const val CARRIER_FREQUENCY_HZ = 38000

        // Timing constants in microseconds (clearly marked for physical blaster validation)
        const val HDR_MARK = 3324
        const val HDR_SPACE = 1574
        const val BIT_MARK = 448
        const val ONE_SPACE = 1182
        const val ZERO_SPACE = 390
        const val STOP_MARK = 448
        const val MIN_GAP = 8100 // Trailing frame space

        /**
         * Creates a default instance without Context (for packet building/testing).
         */
        fun createDefaultInstance(): GeneralAcProtocol {
            return GeneralAcProtocol(null)
        }
    }

    /**
     * Builds the complete stateful IR packet from state and model.
     */
    fun buildPacket(state: AcState, modelStr: String = "ARRAH2E"): ByteArray {
        val model = modelStr.uppercase().trim()
        val isLongModel = model == "ARRAH2E" || model == "ARREB1E" || model == "ARRY4" || model == "ARREW4E"
        val stateLength = if (isLongModel) 16 else 15
        val stateLengthShort = if (isLongModel) 7 else 6

        // Power-off is transmitted as a short packet
        if (!state.power) {
            val packet = ByteArray(stateLengthShort)
            packet[0] = 0x14.toByte()
            packet[1] = 0x63.toByte()
            packet[2] = 0x00.toByte() // Device ID = 0
            packet[3] = 0x10.toByte()
            packet[4] = 0x10.toByte()
            packet[5] = 0x02.toByte() // Power Off Command
            
            if (isLongModel) {
                // Last byte is the inverse of the penultimate byte
                packet[6] = packet[5].toInt().inv().toByte()
            }
            return packet
        }

        // Long packet (Power ON or state change)
        val packet = ByteArray(stateLength)
        packet[0] = 0x14.toByte()
        packet[1] = 0x63.toByte()
        packet[2] = 0x00.toByte() // Device ID = 0
        packet[3] = 0x10.toByte()
        packet[4] = 0x10.toByte()
        packet[5] = if (isLongModel) 0xFE.toByte() else 0xFC.toByte()
        packet[6] = (stateLength - 7).toByte() // RestLength: 9 for 16-byte, 8 for 15-byte
        packet[7] = if (model == "ARREW4E") 0x31.toByte() else 0x30.toByte()

        // Byte 8: Power, Fahrenheit, and Temp
        val powerBit = if (state.power) 1 else 0
        val fahrenheitBit = 0 // Celsius in our app
        val tempVal = if (model == "ARREW4E") {
            ((state.temperature - 8) * 2).coerceIn(0, 63)
        } else {
            ((state.temperature - 16) * 4).coerceIn(0, 63)
        }
        packet[8] = (powerBit or (fahrenheitBit shl 1) or (tempVal shl 2)).toByte()

        // Byte 9: Mode, Clean, TimerType
        val modeVal = when (state.mode) {
            AcMode.AUTO -> 0
            AcMode.COOL -> 1
            AcMode.DRY -> 2
            AcMode.FAN -> 3
            AcMode.HEAT -> 4
        }
        val cleanBit = 0
        val timerTypeVal = if (state.power && state.timerHours > 0) 2 else 0 // 2 is Off Timer
        packet[9] = (modeVal or (cleanBit shl 3) or (timerTypeVal shl 4)).toByte()

        // Byte 10: Fan and Swing
        val fanVal = when (state.fan) {
            FanSpeed.AUTO -> 0
            FanSpeed.HIGH -> 1
            FanSpeed.MEDIUM -> 2
            FanSpeed.LOW -> 3
            FanSpeed.QUIET -> 4
        }
        val swingVal = if (state.swing) {
            if (model == "ARRAH2E" || model == "ARJW2") 3 else 1 // 3 is Both, 1 is Vert
        } else {
            0
        }
        packet[10] = (fanVal or (swingVal shl 4)).toByte()

        // Bytes 11..13: Timer values
        val offTimer = if (state.power && state.timerHours > 0) state.timerHours * 60 else 0
        val offTimerEnable = offTimer > 0
        packet[11] = (offTimer and 0xFF).toByte()
        packet[12] = (((offTimer ushr 8) and 0x07) or (if (offTimerEnable) 8 else 0)).toByte() // Bit 3 is OffTimerEnable (1 shl 3 = 8)
        packet[13] = 0.toByte() // On Timer disabled

        // Byte 14 / Checksum
        if (isLongModel) {
            // Byte 14 for 16-byte models: set unknown bit (bit 5) to 1 (1 shl 5 = 32 = 0x20)
            packet[14] = 0x20.toByte()
            // Byte 15 is Checksum
            val checksum = sumBytes(packet, 7, 8)
            packet[15] = (0x00 - checksum).toByte()
        } else {
            // Byte 14 for 15-byte models is Checksum
            val checksum = sumBytes(packet, 0, 14)
            packet[14] = (0x9B - checksum).toByte()
        }

        return packet
    }

    private fun sumBytes(bytes: ByteArray, start: Int, length: Int): Int {
        var sum = 0
        for (i in start until (start + length)) {
            sum += bytes[i].toInt() and 0xFF
        }
        return sum and 0xFF
    }

    /**
     * Encodes bytes into raw pulses.
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
    fun send(state: AcState, modelStr: String = "ARRAH2E") {
        val packet = buildPacket(state, modelStr)
        val pulses = encodeBytes(packet)
        val pattern = toConsumerIrPattern(pulses)
        
        val irManager = context?.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        if (irManager?.hasIrEmitter() == true) {
            try {
                Log.d(tag, "Transmitting via ConsumerIrManager: State=${state}, Model=${modelStr}")
                irManager.transmit(CARRIER_FREQUENCY_HZ, pattern)
            } catch (e: Exception) {
                Log.e(tag, "Failed to transmit IR signal", e)
            }
        } else {
            Log.d(tag, "[SIMULATION] Transmitted IR signal: State=${state}, Model=${modelStr}, PacketSize=${packet.size} bytes")
        }
    }
}
