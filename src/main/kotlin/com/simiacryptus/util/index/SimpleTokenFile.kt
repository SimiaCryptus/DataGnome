package com.simiacryptus.util.index

import com.simiacryptus.util.files.ByteIndex
import java.io.File

class SimpleTokenFile(file: File) : TokenFile(file) {

  override val tokenIndices by lazy { (0 until tokenCount.tokenIndex).map { ByteIndex(it) } }
  override val tokenCount: TokenCount = run {
    val bytePosition = fileLength
    require(bytePosition > 0) { "Data file empty: $bytePosition" }
    require(bytePosition < Int.MAX_VALUE) { "Data file too large: $bytePosition" }
    TokenCount(bytePosition.bytes)
  }

  override fun charToTokenIndex(position: CharPosition) = TokenCount(position.charIndex)

  override fun tokenToCharIndex(position: TokenCount) = CharPosition(position.tokenIndex)

  override fun charIterator(position: CharPosition): () -> CharIterator = {
    object : CharIterator() {
      val buffer = ByteArray(1)
      var current = position
      override fun hasNext() = true
      override fun nextChar(): Char {
        read(ByteIndex(current.charIndex), buffer)
        current = (current + 1) % fileLength.bytes
        return buffer[0].toInt().toChar()
      }
    }
  }

}

