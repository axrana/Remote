package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun decodePowerBytes() {
    val pattern = GeneralAcIrCodes.POWER
    
    val bits = mutableListOf<Int>()
    var i = 2
    while (i < pattern.size - 2) {
      val mark = pattern[i]
      val space = pattern[i+1]
      val bit = if (space > 800) 1 else 0
      bits.add(bit)
      i += 2
    }
    
    val bytes = mutableListOf<Int>()
    for (b in 0 until bits.size / 8) {
      var byteVal = 0
      for (bitIdx in 0..7) {
        val bit = bits[b * 8 + bitIdx]
        byteVal = byteVal or (bit shl bitIdx)
      }
      bytes.add(byteVal)
    }
    
    // Validate our LSB-first checksum formula: (0x52 - sum(bytes[0..6])) & 0xFF
    val sum = bytes.subList(0, 7).sum()
    val calculatedChecksum = (0x52 - sum) and 0xFF
    val expectedChecksum = bytes[7]
    
    assertEquals(expectedChecksum, calculatedChecksum)
    
    // Also validate MSB-first checksum formula: (0x4D - sum(bytesMsb[0..6])) & 0xFF
    val bytesMsb = mutableListOf<Int>()
    for (b in 0 until bits.size / 8) {
      var byteVal = 0
      for (bitIdx in 0..7) {
        val bit = bits[b * 8 + bitIdx]
        byteVal = byteVal or (bit shl (7 - bitIdx))
      }
      bytesMsb.add(byteVal)
    }
    
    val sumMsb = bytesMsb.subList(0, 7).sum()
    val calculatedChecksumMsb = (0x4D - sumMsb) and 0xFF
    val expectedChecksumMsb = bytesMsb[7]
    
    assertEquals(expectedChecksumMsb, calculatedChecksumMsb)
  }
}

