package com.simiacryptus.util.files

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

class IntArrayAppendFile(val file: File) {

  val length : Long by lazy {
    val length = file.length()
    require(length > 0) { "Data file empty: $length" }
    require(length < Int.MAX_VALUE) { "Data file too large: $length" }
    length/4
  }

  private val bufferedOutputStream by lazy { file.outputStream().buffered() }
  fun append(value: Int) {
    val toBytes = value.toBytes()
    bufferedOutputStream.write(toBytes)
  }


  fun close() {
    bufferedOutputStream.close()
  }

  companion object {
    fun Int.toBytes(): ByteArray {
      val byteArray = ByteArray(4)
      ByteBuffer.wrap(byteArray).putInt(this)
      return byteArray
    }
    fun ByteArray.toInt(): Int {
      return ByteBuffer.wrap(this).int
    }

  }
}

