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
    val charSeq: List<Pair<ByteIndex, String?>> = (0 until fileLength.bytes).map { ByteIndex(it) }
      .runningFold(ByteIndex(-1L) to (null as String?)) { position, index ->
        val buffer = ByteArray(maxCharSize)
        read(position.first, buffer)
        val first = charset.decode(ByteBuffer.wrap(buffer)).first()
        val size = first.toString().encodeToByteArray().size
        (if (position.first < 0) ByteIndex(size.toLong()) else (position.first + size)) to first.toString()
      }.takeWhile { it.first < fileLength }
    val list = charSeq.zipWithNext { a, b ->
      when {
        a.second == null -> ByteIndex(0L)
        b.second == null -> a.first
        a.second!!.isBlank() && b.second!!.isNotBlank() -> a.first
        a.second!!.isNotBlank() && b.second!!.isBlank() -> a.first
        else -> null
      }
    }.filterNotNull()
    (list.zipWithNext { from, to ->
      val buffer = when {
        to < from -> ByteArray(((fileLength + to) - from).bytes.toInt())
        else -> ByteArray((to - from).bytes.toInt())
      }
      read(from, buffer)
      val string = charset.decode(ByteBuffer.wrap(buffer)).toString()
      from
    } + charSeq.last().first).toTypedArray()
  }

  init {
    tokenCount = TokenCount(indexArray.size.toLong())
  }


  override fun readString(position: TokenCount, n: CharPosition, skip: CharPosition): String {
    val prev: ByteIndex = indexArray[position.asInt]
    return StringIterator(prev).asSequence().runningFold("", { a, b -> a + b })
      .dropWhile { it.length < (skip + n).charIndex }.first()
      .drop(skip.asInt).take(n.asInt)
  }

  override fun tokenIterator(position: TokenCount): () -> Iterator<String> = {
    StringIterator(indexArray[position.asInt])
  }

  inner class StringIterator(
    private val position: ByteIndex
  ) : Iterator<String> {
    var nextPos =
      tokenIndices.indexOf(position).apply { if (this < 0) throw IllegalArgumentException("Position $position not found") }

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

