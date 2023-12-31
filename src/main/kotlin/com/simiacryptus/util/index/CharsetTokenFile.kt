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
  override val tokenIndices by lazy { indexArray.asIterable() }

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
    require(n.charIndex > 0)
    val from = tokenToCharIndex(position) + skip
    return readString(indexArray[from.asInt % indexArray.size], indexArray[(from + n).asInt % indexArray.size])
  }

  private fun readString(
    fromByte: ByteIndex,
    toByte: ByteIndex
  ): String = when {
    toByte > fileLength -> readString(fromByte, toByte - fileLength)
    toByte < 0L -> readString(fromByte, toByte + fileLength)
    toByte <= 0L  && fromByte == fileLength -> ""
    toByte <= fromByte -> when {
      toByte <= 0L -> readString(fromByte, fileLength)
      fromByte == fileLength -> readString(ByteIndex(0), toByte)
      else -> readString(fromByte, fileLength) + readString(ByteIndex(0), toByte)
    }
    else -> {
      val buffer = ByteArray((toByte - fromByte).bytes.toInt())
      read(fromByte, buffer)
      val toString = charset.decode(ByteBuffer.wrap(buffer)).toString()
      require(toString.isNotEmpty())
      toString
    }
  }

  override fun charToTokenIndex(position: CharPosition) = TokenCount(position.charIndex)
  override fun tokenToCharIndex(position: TokenCount)  =  CharPosition(position.tokenIndex)

  override fun charIterator(position: CharPosition): () -> CharIterator {
    return {
      object : CharIterator() {
        val initPos = charToTokenIndex(position)
        val readAheadBuffer = CharPosition(16.coerceAtMost(indexArray.size - 1).toLong())
        val initialBuffer = readString(initPos, readAheadBuffer)
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

