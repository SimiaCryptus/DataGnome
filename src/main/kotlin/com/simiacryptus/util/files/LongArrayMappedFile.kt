package com.simiacryptus.util.files

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class LongArrayMappedFile(file: File, count: XElements) {

  private val mappedByteBuffer by lazy {
    channel.map(FileChannel.MapMode.READ_WRITE, 0, 4 * channel.size())
  }

  private var length : XElements = XElements(-1)

  private val channel by lazy {
    length = if (!file.exists()) {
      initialize(file, count)
      count
    } else {
      XElements(file.length() / 4)
    }
    FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ)
  }

  fun getLength(): XElements {
    require(null != mappedByteBuffer)
    return length
  }

  fun get(position: XElements): Long {
    require(position.asLong >= 0) { "Index out of bounds: $position" }
    require(position < length) { "Index out of bounds: $position / $length" }
    val value = mappedByteBuffer.getInt(4 * position.asInt).toLong()
    require(value >= 0) { "Index out of bounds: $value @$position" }
    require(value < length.asLong) { "Index out of bounds: $value / $length @$position" }
    return value
  }

  fun set(position: XElements, value: Long) {
    require(mappedByteBuffer != null)
    require(position.asLong >= 0) { "Index out of bounds: $position" }
    require(position < length) { "Index out of bounds: $position / $length" }
    mappedByteBuffer.putInt(4 * position.asInt, value.toInt())
  }

  fun close() {
    mappedByteBuffer.force()
    channel.close()
  }

  companion object {
    fun initialize(file: File, count: XElements) {
      file.createNewFile()
      file.setWritable(true)
      file.setReadable(true)
      file.setExecutable(false)
      file.outputStream().buffered().use { out ->
        val byteArray = ByteArray(4)
        val wrap = ByteBuffer.wrap(byteArray)
        (0 until count.asLong).forEach { i ->
          wrap.clear()
          wrap.putInt(-1)
          out.write(byteArray)
        }
      }
    }
  }

}