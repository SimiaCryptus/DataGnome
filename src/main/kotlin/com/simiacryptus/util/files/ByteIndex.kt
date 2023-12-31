package com.simiacryptus.util.files

@JvmInline value class ByteIndex(val bytes: Long) : Comparable<ByteIndex> {
  val bytesAsInt get() = bytes.toInt()
  override operator fun compareTo(other: ByteIndex) = bytes.compareTo(other.bytes)
  operator fun compareTo(other: Long) = bytes.compareTo(other)
  operator fun compareTo(other: Int) = bytes.compareTo(other)
  operator fun plus(other: ByteIndex) = ByteIndex(bytes + other.bytes)
  operator fun minus(other: ByteIndex) = ByteIndex(bytes - other.bytes)
  operator fun plus(other: Long) = ByteIndex(bytes + other)
  operator fun minus(other: Long) = ByteIndex(bytes - other)

  operator fun plus(other: Int) = ByteIndex(bytes + other)
  operator fun minus(other: Int) = ByteIndex(bytes - other)
  operator fun rem(other: ByteIndex) = ByteIndex(bytes % other.bytes)
  operator fun rem(other: Long) = ByteIndex(bytes % other)

}
infix fun ByteIndex.until(to: ByteIndex): Iterable<ByteIndex> = this.bytes.until(to.bytes).map { ByteIndex(it) }