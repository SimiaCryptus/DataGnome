package com.simiacryptus.util.index

import com.simiacryptus.util.files.ElementIndex
import com.simiacryptus.util.files.LongArrayMappedFile
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*


class FileIndexer(
  val data: TokenFile,
  val index: LongArrayMappedFile,
) {

  val characters: Set<String> by lazy {
    data.tokenIterator(TokenCount(0)).invoke().asSequence().take(data.tokenCount.tokenIndex.toInt()).toSet()
  }

  /**
   * Sorts the file points in the index file to match the (infinite circular) substring at the given position.
   */
  fun buildIndex(n: CharPosition = CharPosition(2)) {
    val indexLength = index.getLength().let { if (it.element < 0) ElementIndex(data.tokenCount.tokenIndex) else it }
    populateIndex(
      parent = populateByScan(
        n = n,
        skip = CharPosition(0),
        from = ElementIndex(0L),
        to = indexLength,
        indices = (0 until data.tokenCount.tokenIndex).map { TokenCount(it) }),
      n = n,
      skip = n,
      from = ElementIndex(0L)
    )
  }

  fun find(sequence: CharSequence): Array<TokenCount> {
    var start = ElementIndex(0L)
    var end = ElementIndex(index.getLength().element)
    while (start < end) {
      val mid = ElementIndex((start + end).element / 2)
      val midVal = data.readString(TokenCount(index.get(mid)), CharPosition(sequence.length.toLong()))
      when {
        midVal < sequence -> start = mid + 1
        midVal > sequence -> end = mid
        else -> {
          // Find the start of the sequence
          var i = mid
          var buffer: String = ""
          while (i > 0) {
            buffer = data.readString(TokenCount(index.get(i - 1)), CharPosition(sequence.length.toLong()))
            if (buffer != sequence) break
            i -= 1
          }
          // Find the end of the sequence
          var j = mid
          while (j < index.getLength()) {
            buffer = data.readString(TokenCount(index.get(j + 1)), CharPosition(sequence.length.toLong()))
            if (buffer != sequence) break
            j += 1
          }
          return (i.element until (j + 1).element)
            .map { ElementIndex(it) }.map { index.get(it) }.sorted().map { TokenCount(it) }
            .toTypedArray()
        }
      }
    }
    return emptyArray()
  }

  fun findCompressionPrefixes(threshold: Int, count: Int): Array<Pair<String, Int>> {
    val returnMap = TreeMap<String, Int>()
    val map = TreeMap<String, TreeSet<ElementIndex>>()
    for (i in (0 until index.getLength().element).map { ElementIndex(it) }) {
      val lastPtrIdx = if (i <= 0) null else TokenCount(index.get(i - 1))
      val currentIdx = TokenCount(index.get(i))
      val nextPtrIdx = if (i >= index.getLength() - 1) null else TokenCount(index.get(i + 1))
      val lastPtr = lastPtrIdx?.run { data.tokenIterator(this) }
      val nextPtr = nextPtrIdx?.run { data.tokenIterator(this) }
      val currentPtr = data.tokenIterator(currentIdx)
      val commonPrefixA = commonPrefix(lastPtr?.invoke(), currentPtr())
      val commonPrefixB = commonPrefix(currentPtr(), nextPtr?.invoke())
      val longestCommonPrefix = if (commonPrefixA.length > commonPrefixB.length) commonPrefixA else commonPrefixB
      map.keys.filter { !longestCommonPrefix.startsWith(it) }.toTypedArray().forEach { newPrefix ->
        val size = map.remove(newPrefix)!!.size
        val fitness = prefixFitness(newPrefix, size)
        if (fitness > threshold) {
          returnMap[newPrefix] = size
        }
        map.remove(newPrefix)
      }
      (0 until longestCommonPrefix.length).forEach { j ->
        val substring = longestCommonPrefix.substring(0, j)
        map.getOrPut(substring, { TreeSet<ElementIndex>() }).add(i)
      }
    }
    map.keys.toTypedArray().forEach {
      val size = map.remove(it)!!.size
      val fitness = prefixFitness(it, size)
      if (fitness > threshold) {
        returnMap[it] = size
      }
    }
    return collect(returnMap, count).toList().sortedBy { -prefixFitness(it.first, it.second) }.toTypedArray()
  }

  private fun prefixFitness(string: String, count: Int): Int {
    val length = string.encodeToByteArray().size
    return (count * length) - (count * 4) - length
  }

  private fun collect(map: TreeMap<String, Int>, count: Int): Map<String, Int> {
    // Iteratively select the top fitness value, add it to the new map, and remove all overlapping entries
    val returnMap = TreeMap<String, Int>()
    while (map.isNotEmpty() && returnMap.size < count) {
      val best = map.entries.maxByOrNull { prefixFitness(it.key, it.value) }!!
      returnMap[best.key] = best.value
      map.keys.filter { best.key.startsWith(it) || it.startsWith(best.key) }
        .toTypedArray().forEach { newPrefix -> map.remove(newPrefix) }
    }
    return returnMap
  }

  private fun commonPrefix(a: Iterator<String>?, b: Iterator<String>?): String {
    a ?: return ""
    b ?: return ""
    val buffer = StringBuilder()
    var loopCnt = 0
    while (a.hasNext() && b.hasNext()) {
      if (loopCnt++ > 10000) {
        throw IllegalStateException()
      }
      val next = a.next()
      val next2 = b.next()
      if (next != next2) break
      buffer.append(next)
    }
    return buffer.toString()
  }

  private fun countNGrams(
    n: CharPosition,
    skip: CharPosition = CharPosition(0),
    indices: Iterable<TokenCount>
  ): TreeMap<CharSequence, Int> {
    val map = TreeMap<CharSequence, Int>()
    for (position in indices) {
      val key = data.readString(position, n, skip)
      map[key] = map.getOrDefault(key, 0) + 1
    }
    return map
  }

  private fun populateIndex(
    parent: TreeMap<CharSequence, Int>,
    n: CharPosition,
    skip: CharPosition,
    from: ElementIndex
  ) {
    var position = from
    parent.forEach { (nGram, count) ->
      val start = position
      val end = start + count
      position = end
      val indices =
        (start.element until end.element).map { ElementIndex(it) }.map { TokenCount(index.get(it)) }.toTypedArray()
      if (count > 1) {
        if (count >= 10) {
          // Sort and recurse for large blocks
          val nextMap = populateByScan(n = n, skip = skip, from = start, to = end, indices = indices.asIterable())
          if (nextMap.size > 1) {
            populateIndex(parent = nextMap, n = n, skip = skip + n, from = start)
            return@forEach
          }
        }
        // Sort directly for small blocks
        indices.sortWith { a, b ->
          when {
            a == null && b == null -> 0
            a == null -> -1
            b == null -> 1
            else -> data.readString(a, n, skip).compareTo(data.readString(b, n, skip))
          }
        }
        for (i in indices.indices) {
          index.set(start + i, indices[i].tokenIndex)
        }
      }
    }
  }

  private fun populateByScan(
    n: CharPosition,
    skip: CharPosition,
    from: ElementIndex,
    to: ElementIndex,
    indices: Iterable<TokenCount>,
  ): TreeMap<CharSequence, Int> {
    val nGramCounts = countNGrams(n, skip, indices)
//    log.debug("nGramCounts sum: ${nGramCounts.values.sum()}, expected: ${(to - from).element.toInt()}")
//    log.debug("from: $from, to: $to, to-from: ${(to - from).element}")
    require(nGramCounts.values.sum() == (to - from).element.toInt())
    val nGramPositions = accumulatePositions(nGramCounts)
    for (tokenPosition in indices) {
      val key = data.readString(tokenPosition, n, skip)
      val position = nGramPositions[key]!!
      require(position >= 0)
      require(position < (to - from))
      index.set(from + position, tokenPosition.tokenIndex)
      nGramPositions[key] = position + 1
    }
    return nGramCounts
  }


  fun close() {
    index.close()
    data.close()
  }

  companion object {
    val log = LoggerFactory.getLogger(FileIndexer::class.java)

    fun accumulatePositions(nGramCounts: TreeMap<CharSequence, Int>): TreeMap<CharSequence, ElementIndex> {
      val nGramPositions = TreeMap<CharSequence, ElementIndex>()
      var position = ElementIndex(0L)
      for ((nGram, count) in nGramCounts) {
        nGramPositions[nGram] = position
        position += count
      }
      return nGramPositions
    }

    operator fun (() -> CharIterator).compareTo(sequence: CharSequence): Int {
      var i = 0
      this.invoke().apply {
        while (hasNext() && i < sequence.length) {
          val next = next()
          if (next < sequence[i]) return -1
          if (next > sequence[i]) return 1
          i++
        }
        return 0 // CharIterators are infinite, so we can't tell which is longer
      }
    }

    operator fun Sequence<Char>.compareTo(sequence: Sequence<Char>) = iterator().compareTo(sequence.iterator())

    operator fun CharIterator.compareTo(sequence: CharIterator): Int {
      var i = 0
      this.apply {
        while (hasNext() && sequence.hasNext()) {
          val next = next()
          val next2 = sequence.next()
          if (next < next2) return -1
          if (next > next2) return 1
          i++
        }
        if (hasNext()) return 1 // The first iterator has more elements
        if (sequence.hasNext()) return -1  // The second iterator has more elements
        return 0
      }
    }

    operator fun <T : Comparable<T>> Iterator<T>.compareTo(sequence: Iterator<T>): Int {
      var i = 0
      this.apply {
        while (hasNext() && sequence.hasNext()) {
          val next = next()
          val next2 = sequence.next()
          if (next < next2) return -1
          if (next > next2) return 1
          i++
        }
        if (hasNext()) return 1 // The first iterator has more elements
        if (sequence.hasNext()) return -1  // The second iterator has more elements
        return 0
      }
    }

  }


}

fun FileIndexer(dataFile: File, indexFile: File = File(dataFile.parentFile, "${dataFile.name}.index")): FileIndexer {
  return FileIndexer(CharsetTokenFile(dataFile), indexFile)
}

fun FileIndexer(
  data: TokenFile,
  indexFile: File = File(data.file.parentFile, "${data.file.name}.index")
) = FileIndexer(data, LongArrayMappedFile(indexFile, ElementIndex(data.tokenCount.tokenIndex)))

private operator fun CharSequence.compareTo(sequence: CharSequence): Int {
  var i = 0
  while (i < length && i < sequence.length) {
    val next = get(i)
    val next2 = sequence[i]
    if (next < next2) return -1
    if (next > next2) return 1
    i++
  }
  if (length > sequence.length) return 1 // The first iterator has more elements
  if (sequence.length > length) return -1  // The second iterator has more elements
  return 0
}
