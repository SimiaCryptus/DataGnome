package com.simiacryptus.util.files

import java.io.File

class IntArrayAppendFile(val file: File) {

  private var isClosed: Boolean = false
  var length : ElementIndex = run {
    val length = file.length()
    //require(length > 0) { "Data file empty: $length" }
    require(length < Int.MAX_VALUE) { "Data file too large: $length" }
    ElementIndex(length/4)
  }
    private set

  private val bufferedOutputStream by lazy { file.outputStream().buffered() }
  fun append(value: Int) {
    if(isClosed) throw IllegalStateException("File is closed")
    val toBytes = value.toBytes()
    bufferedOutputStream.write(toBytes)
    length = length + 1
  }


  fun close() {
    isClosed = true
    bufferedOutputStream.close()
  }

  companion object {
  }
}

