package com.simiacryptus.util.index

import com.simiacryptus.util.files.ByteIndex
import java.io.File
import java.nio.ByteBuffer

class CharsetTokenFile(
  file: File,
  charsetName: String = "UTF-8",
  val maxCharSize: Int = 8
) : TokenFile(file) {
  private val charset = java.nio.charset.Charset.forName(charsetName)
  override var tokenCount: TokenCount = TokenCount(-1)
  override val tokenIndices by lazy {
    indexArray.asIterable()
  }

  private val indexArray by lazy {
    (0 until fileLength.bytes).runningFold(ByteIndex(0L)) { position, index ->
      val buffer = ByteArray(maxCharSize)
      read(position, buffer)
      val first = charset.decode(ByteBuffer.wrap(buffer)).first()
      val size = first.toString().encodeToByteArray().size
      (position + size)
    }.takeWhile { it < fileLength }.toTypedArray()
  }

  init {
    tokenCount = TokenCount(indexArray.size.toLong())
  }

  override fun readString(position: TokenCount, n: CharPosition, skip: CharPosition): String {
    val buffer = ByteArray(((n + skip).charIndex * maxCharSize).coerceAtMost(fileLength.bytes).toInt())
    read(indexArray[position.tokenIndex.toInt()], buffer)
    return charset.decode(ByteBuffer.wrap(buffer)).drop(skip.charIndex.toInt()).take(n.charIndex.toInt()).toString()
  }
  //charToTokenIndex
  override fun charToTokenIndex(position: CharPosition) = TokenCount(position.charIndex)
  override fun tokenToCharIndex(position: TokenCount)  =  CharPosition(position.tokenIndex)

  override fun charIterator(position: CharPosition): () -> CharIterator {
    val initialBuffer = readString(charToTokenIndex(position), CharPosition(16.coerceAtMost(tokenCount.tokenIndex.toInt() - 1).toLong()))
    return {
      object : CharIterator() {
        var buffer = initialBuffer
        var nextPos = position + initialBuffer.length
        var pos = 0
        override fun hasNext() = true
        override fun nextChar(): Char {
          val char = buffer.get(pos++)
          if (pos >= buffer.length) {
            buffer = readString(charToTokenIndex(nextPos), CharPosition(16))
            nextPos = nextPos + buffer.length
            pos = 0
          }
          return char
        }
      }
    }
  }

}

