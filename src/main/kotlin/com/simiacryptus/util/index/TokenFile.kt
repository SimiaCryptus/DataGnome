package com.simiacryptus.util.index

import com.simiacryptus.util.files.*
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

abstract class TokenFile(val file: File) {

  val fileLength = ByteIndex(file.length())

  private val channel by lazy { FileChannel.open(file.toPath(), StandardOpenOption.READ) }
  protected val mappedByteBuffer by lazy { channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength.bytes) }

  abstract val tokenIndices: Iterable<ByteIndex>
  abstract val tokenCount: TokenCount


  fun read(i: ByteIndex, buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset) {
    when {
      i < 0 -> read((i % fileLength) + fileLength, buffer, offset, length)
      fileLength <= i -> read(i % fileLength, buffer, offset, length)
      fileLength < (i.bytes + length) -> {
        val splitAt = (fileLength - i).bytes.toInt()
        read(i, buffer, offset, splitAt)
        val remainder = length - splitAt
        read(ByteIndex(0), buffer, offset + splitAt, remainder)
      }

      else -> mappedByteBuffer.get(i.bytes.toInt(), buffer, offset, length)
    }
  }

  open fun readString(position: TokenCount, n: CharPosition, skip: CharPosition = CharPosition(0)) =
    tokenIterator(position).invoke().asSequence()
      .runningFold("", { a, b -> a + b }).dropWhile { it.length < (skip + n).charIndex }.first()
      .drop(skip.charIndex.toInt()).take(n.charIndex.toInt())

  open fun charIterator(position: CharPosition): () -> CharIterator {
    return {
      object : CharIterator() {
        val iterator = tokenIterator(charToTokenIndex(position)).invoke()
        var current: String? = null
        var pos = 0
        override fun hasNext() = true
        override fun nextChar(): Char = when {
          current == null -> {
            current = iterator.next()
            pos = 0
            nextChar()
          }

          pos >= current!!.length -> {
            current = iterator.next()
            pos = 0
            nextChar()
          }

          else -> current!![pos++]
        }
      }
    }
  }

  open fun tokenIterator(position: TokenCount): () -> Iterator<String> {
    val charIterator = charIterator(tokenToCharIndex(position))
    return {
      object : Iterator<String> {
        val iterator = charIterator.invoke()
        override fun hasNext() = iterator.hasNext()
        override fun next(): String = iterator.next().toString()
      }
    }
  }

  open fun charToTokenIndex(position: CharPosition): TokenCount = throw NotImplementedError(this::class.java.name)
  open fun tokenToCharIndex(position: TokenCount): CharPosition = throw NotImplementedError(this::class.java.name)

  fun close() {
    channel.close()
  }

  fun writeCompressed(codec: List<String>): Pair<File, File> {
    val maxPrefixLength = codec.maxOfOrNull { it.length } ?: throw IllegalStateException()
    val compressedSequence = File(file.parentFile, "${file.name}.compressed")
    val indexMap = codec.mapIndexed() { index, str ->
      str to index
    }.toMap()
    val dictionary = writeDictionary(codec)
    val prefixLookup = PrefixLookup(codec)
    val arrayFile = IntArrayAppendFile(compressedSequence)
    var position = TokenCount(0L)
    while (position < tokenCount) {
      val string = readString(position, CharPosition(maxPrefixLength.toLong()))
      val prefix = prefixLookup.find(string)?.firstOrNull()
      prefix ?: throw IllegalStateException("No prefix found for $string")
      val value = indexMap[prefix]
      val size = prefix.length
      arrayFile.append(value!!)
      position += size
    }
    arrayFile.close()
    return compressedSequence to dictionary
  }

  fun expand(codecMap: List<String>, compressed: File?, file: File) {
    val codec = codecMap.mapIndexed { index, str -> index to str }.toMap()
    val arrayFile = IntArrayMappedFile(compressed!!)
    val writer = file.writer()
    var elementIndex = ElementIndex(0L)
    while (elementIndex < arrayFile.length) {
      val index = arrayFile.get(elementIndex)
      val string = codec[index]!!
      //val size = string.encodeToByteArray().size
      writer.write(string)
      elementIndex += 1
    }
    writer.close()
  }

  private class PrefixLookup(codec: Collection<String>) {
    val children by lazy {
      codec.filter { it.length > 1 }.groupBy { it.first() }.mapValues { PrefixLookup(it.value.map { it.drop(1) }) }
    }
    val matches by lazy { codec.filter { it.length == 1 }.groupBy { it.first() }.keys }
    fun find(prefix: String): List<String>? {
      val first = prefix.firstOrNull()
      return when {
        first == null -> null
        prefix.length == 1 -> exactMatches(first)
        else -> children[first]?.find(prefix.drop(1))?.map { first + it } ?: exactMatches(first)
      }
    }

    private fun exactMatches(first: Char) = if (matches.contains(first)) listOf(first.toString()) else null
  }

  private fun writeDictionary(codec: List<String>): File {
    val dictionaryFile = File(file.parentFile, "${file.name}.dictionary")
    val sequenceFile = SequenceFile(dictionaryFile)
    codec.mapIndexed { index, str -> require(sequenceFile.append(str.encodeToByteArray()) == ElementIndex(index.toLong())) }
    sequenceFile.close()
    return dictionaryFile
  }
}