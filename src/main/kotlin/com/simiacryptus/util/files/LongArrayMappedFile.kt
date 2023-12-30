package com.simiacryptus.util.files

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class LongArrayMappedFile(file: File, count: ElementIndex) {

  private val mappedByteBuffer by lazy {
    channel.map(FileChannel.MapMode.READ_WRITE, 0, 4 * length.element)
  }

  private var length : ElementIndex = ElementIndex(-1)
  fun getLength() = length

  private val channel by lazy {
    length = if (!file.exists()) {
      initialize(file, count)
      count
    } else {
      ElementIndex(file.length() / 4)
    }
    FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ)
  }

  fun get(position: ElementIndex): Long {
    require(position.element >= 0) { "Index out of bounds: $position" }
    require(position < length) { "Index out of bounds: $position / $length" }
    val idx = 4 * position.element
    val value = mappedByteBuffer.getInt(idx.toInt()).toLong()
    require(value >= 0) { "Index out of bounds: $value @$position" }
    require(value < length.element) { "Index out of bounds: $value / $length @$position" }
    return value
  }

  fun set(position: ElementIndex, value: Long) {
    require(position.element >= 0) { "Index out of bounds: $position" }
    require(position < length) { "Index out of bounds: $position / $length" }
    mappedByteBuffer.putInt(4 * position.element.toInt(), value.toInt())
  }

  fun close() {
    mappedByteBuffer.force()
    channel.close()
  }

  companion object {
    fun initialize(file: File, count: ElementIndex) {
      file.createNewFile()
      file.setWritable(true)
      file.setReadable(true)
      file.setExecutable(false)
      file.outputStream().buffered().use { out ->
        val byteArray = ByteArray(4)
        val wrap = ByteBuffer.wrap(byteArray)
        (0 until count.element).forEach { i ->
          wrap.clear()
          wrap.putInt(-1)
          out.write(byteArray)
        }
      }
    }
  }

}