package com.simiacryptus.util.index

import com.simiacryptus.util.files.ByteIndex
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset

class WordTokenFile(
  file: File,
  charsetName: String = "UTF-8",
  val maxCharSize: Int = 8
) : TokenFile(file) {
  private val charset = Charset.forName(charsetName)
  override var tokenCount: TokenCount = TokenCount(-1)
  override val tokenIndices by lazy { indexArray.asIterable() }

  private val indexArray: Array<ByteIndex> by lazy {
    val charSeq: List<Pair<ByteIndex, String>> = (0 until fileLength.bytes)
      .runningFold(ByteIndex(0) to (read(ByteIndex(0)))) { position, index ->
        val size = position.second.encodeToByteArray().size
        val nextPos = position.first + size
        nextPos to read(nextPos)
      }.takeWhile { it.first < fileLength }
    val list = (listOf(ByteIndex(-1) to null) + charSeq).zipWithNext { a, b ->
      when {
        b.second == null -> ByteIndex(0L)
        a.second == null -> b.first
        a.second!!.isBlank() && b.second!!.isNotBlank() -> b.first
        a.second!!.isNotBlank() && b.second!!.isBlank() -> b.first
        else -> null
      }
    }.filterNotNull()
    list.toTypedArray<ByteIndex>()
  }

  private fun read(byteIndex: ByteIndex): String {
    val buffer = ByteArray(maxCharSize)
    read(byteIndex, buffer)
    return charset.decode(ByteBuffer.wrap(buffer)).first().toString()
  }

  init {
    tokenCount = TokenCount(indexArray.size.toLong())
  }

  override fun charToTokenIndex(position: CharPosition) = TokenCount(StringIterator().asSequence()
    .runningFold(CharPosition(0)) { a, b -> a + b.length }.takeWhile { it < position }.count().toLong()
)
  override fun readString(position: TokenCount, n: CharPosition, skip: CharPosition): String {
    val prev: ByteIndex = indexArray[position.asInt]
    return StringIterator(prev).asSequence().runningFold("") { a, b -> a + b }
      .dropWhile { it.length < (skip + n).charIndex }
      .first()
      .drop(skip.asInt)
      .take(n.asInt)
  }

  override fun tokenToCharIndex(position: TokenCount) = StringIterator().asSequence()
    .runningFold(CharPosition(0)) { a, b -> a + b.length }.drop(position.asInt - 1).first()

  override fun tokenIterator(position: TokenCount): () -> Iterator<String> = {
    StringIterator(indexArray[position.asInt])
  }

  inner class StringIterator(
    private val from: ByteIndex = ByteIndex(0L)
  ) : Iterator<String> {
    private var nextPos =
      tokenIndices.indexOf(from).apply { if (this < 0) throw IllegalArgumentException("Position $from not found") }

    override fun hasNext() = true
    override fun next(): String {
      val from = indexArray[(nextPos++ % indexArray.size)]
      val to = indexArray[(nextPos % indexArray.size)]
      val buffer = when {
        to < from -> ByteArray(((fileLength + to) - from).bytesAsInt)
        to == from -> return ""
        else -> ByteArray((to - from).bytesAsInt)
      }
      read(from, buffer)
      val string = charset.decode(ByteBuffer.wrap(buffer)).toString()
      return string
    }
  }

}

