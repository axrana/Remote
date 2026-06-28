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
    // The pattern starts with 3320, 1570 (Header).
    // Followed by bit representations:
    // Bit 0: 430 mark, 380 space -> 0
    // Bit 1: 430 mark, 1180 space -> 1
    // (Or vice versa, let's look at both)
    
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
    
    println("DECODED BITS: " + bits.joinToString(""))
    println("DECODED BYTES (HEX LSB-FIRST): " + bytes.map { "0x" + it.toString(16).uppercase() }.joinToString(", "))
    
    // Let's find a formula for the checksum (bytes[7] = 0x87)
    val checkVal = bytes[7]
    for (start in 0..6) {
      for (end in start..6) {
        val sum = bytes.subList(start, end + 1).sum()
        val sumByte = sum and 0xFF
        
        // Let's check various common checksum formula patterns
        for (constVal in 0..255) {
          if (((constVal - sumByte) and 0xFF) == checkVal) {
            println("Found Subtraction Formula: checksum = (0x${constVal.toString(16).uppercase()} - sum(bytes[$start..$end])) & 0xFF")
          }
          if (((sumByte - constVal) and 0xFF) == checkVal) {
            println("Found Alternative Subtraction Formula: checksum = (sum(bytes[$start..$end]) - 0x${constVal.toString(16).uppercase()}) & 0xFF")
          }
          if (((sumByte + constVal) and 0xFF) == checkVal) {
            println("Found Addition Formula: checksum = (sum(bytes[$start..$end]) + 0x${constVal.toString(16).uppercase()}) & 0xFF")
          }
          if ((sumByte xor constVal) == checkVal) {
            println("Found XOR Formula: checksum = sum(bytes[$start..$end]) xor 0x${constVal.toString(16).uppercase()}")
          }
        }
      }
    }

    // Let's also check if the bits are MSB-first
    val bytesMsb = mutableListOf<Int>()
    for (b in 0 until bits.size / 8) {
      var byteVal = 0
      for (bitIdx in 0..7) {
        val bit = bits[b * 8 + bitIdx]
        byteVal = byteVal or (bit shl (7 - bitIdx))
      }
      bytesMsb.add(byteVal)
    }
    println("DECODED BYTES (HEX MSB-FIRST): " + bytesMsb.map { "0x" + it.toString(16).uppercase() }.joinToString(", "))
  }
}

