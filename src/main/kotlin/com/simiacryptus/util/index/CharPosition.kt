package com.simiacryptus.util.index

@JvmInline value class CharPosition(val charIndex: Long) {
  val asInt get() = charIndex.toInt()
  operator fun compareTo(other: CharPosition) = charIndex.compareTo(other.charIndex)
  operator fun compareTo(other: Long) = charIndex.compareTo(other)
  operator fun compareTo(other: Int) = charIndex.compareTo(other)
  operator fun plus(other: CharPosition) = CharPosition(charIndex + other.charIndex)
  operator fun minus(other: CharPosition) = CharPosition(charIndex - other.charIndex)
  operator fun plus(other: Long) = CharPosition(charIndex + other)
  operator fun plus(other: Int) = CharPosition(charIndex + other)
  operator fun minus(other: Long) = CharPosition(charIndex - other)
  operator fun minus(other: Int) = CharPosition(charIndex - other)
  operator fun rem(other: CharPosition) = CharPosition(charIndex % other.charIndex)
  operator fun rem(other: Long) = CharPosition(charIndex % other)
  operator fun rem(other: Int) = CharPosition(charIndex % other)
}