package com.simiacryptus.util.index

import com.simiacryptus.util.files.ByteIndex
import com.simiacryptus.util.files.ElementIndex
import com.simiacryptus.util.files.IntArrayMappedFile
import com.simiacryptus.util.files.SequenceFile
import java.io.File

class  CompressedTokenFile(
  file: File,
  dictionaryFile: File,
) : TokenFile(file) {
  override val tokenIndices: Iterable<ByteIndex> get() = (0 until tokenCount.tokenIndex).map {
      val tokenPosition = TokenCount(it)
      ByteIndex(tokenPosition.tokenIndex * 4)
    }.asIterable()
  override val tokenCount: TokenCount by lazy { TokenCount(file.length() / 4) }
  val dict = SequenceFile(dictionaryFile)
  val data = IntArrayMappedFile(file)
  val codec by lazy { dict.read().map { String(it) } }

  override fun tokenIterator(position: TokenCount): () -> Iterator<String> = {
    object : Iterator<String> {
      var nextPos = ElementIndex(position.tokenIndex)
      override fun hasNext() = true
      override fun next(): String {
        val get: Int = data.get((nextPos % data.length.element))
        nextPos += 1
        return codec[get]
      }
    }
  }

}