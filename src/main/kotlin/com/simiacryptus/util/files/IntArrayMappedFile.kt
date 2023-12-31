package com.simiacryptus.util.files

import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class IntArrayMappedFile(val file: File) {


  val length : XElements by lazy {
    val length = file.length()
    require(length > 0) { "Data file empty: $length" }
    require(length < Int.MAX_VALUE) { "Data file too large: $length" }
    XElements(length/4)
  }

  private val channel by lazy { FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE) }
  private val mappedByteBuffer by lazy { channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length()) }

  fun get(pos: XElements) : Int {
    return mappedByteBuffer.getInt((pos.asLong * 4).toInt())
  }

  fun close() {
    channel.close()
  }

  companion object {

  }
}


