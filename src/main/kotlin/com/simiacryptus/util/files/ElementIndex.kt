package com.simiacryptus.util.files

@JvmInline value class ElementIndex(val element: Long) {
  val asInt get() = element.toInt()
  operator fun compareTo(other: ElementIndex) = element.compareTo(other.element)
  operator fun compareTo(other: Long) = element.compareTo(other)
  operator fun compareTo(other: Int) = element.compareTo(other)

  operator fun plus(other: ElementIndex) = ElementIndex(element + other.element)
  operator fun minus(other: ElementIndex) = ElementIndex(element - other.element)
  operator fun plus(other: Long) = ElementIndex(element + other)
  operator fun minus(other: Long) = ElementIndex(element - other)
  operator fun plus(other: Int) = ElementIndex(element + other)
  operator fun minus(other: Int) = ElementIndex(element - other)

  operator fun rem(other: ElementIndex) = ElementIndex(element % other.element)
  operator fun rem(other: Long) = ElementIndex(element % other)
  operator fun rem(other: Int) = ElementIndex(element % other)
}