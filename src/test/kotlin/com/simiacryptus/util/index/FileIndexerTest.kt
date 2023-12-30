package com.simiacryptus.util.index
import com.simiacryptus.util.files.ByteIndex
import com.simiacryptus.util.files.ElementIndex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File


class FileIndexerTest {

  @Test
  fun testDataRead() {
    val tempDataFile = File.createTempFile("testData", ".txt")
    tempDataFile.writeText("This is a test data file.")
    val fileIndexer = FileIndexer(tempDataFile)
    try {
      val buffer = ByteArray(4)
      fileIndexer.data.read(ByteIndex(0L), buffer)
      val result = String(buffer)
      assertEquals("This", result)
    } finally {
      // Close the index file and delete the temporary file
      fileIndexer.close()
      tempDataFile.delete()
    }
  }


  @Test
  fun testIndexGetAndSet() {
    val tempDataFile = File.createTempFile("testData", ".txt")
    tempDataFile.writeText("This is a test data file.")
    val fileIndexer = FileIndexer(tempDataFile)
    try {
      fileIndexer.index.set(ElementIndex(0L), 1)
      val result = fileIndexer.index.get(ElementIndex(0))
      assertEquals(1, result)
    } finally {
      // Close the index file and delete the temporary file
      fileIndexer.close()
      tempDataFile.delete()
    }
  }

  @Test
  fun testSearch() {
    val tempDataFile = File.createTempFile("testData", ".txt")
    tempDataFile.writeText("This is a test data file.")
    val fileIndexer = FileIndexer(tempDataFile)
    try {
      fileIndexer.buildIndex(CharPosition(1))
      fileIndexer.apply {
        val strings = (0 until index.getLength().element).toList()
          .map { CharPosition(index.get(ElementIndex(it))) }
          .map { data.charIterator(it) }
          .map { it.invoke().asSequence().take(data.tokenCount.asInt).joinToString("") }
        strings.mapIndexed { index, s -> println("$index: $s") }
        assertEquals(strings.toList().sorted().joinToString("\n"), strings.joinToString("\n"))
      }

      assertEquals(listOf(0L), fileIndexer.find("This").toList().sortedBy { it.tokenIndex })
      assertEquals(listOf(2L, 5L), fileIndexer.find("is").toList().sortedBy { it.tokenIndex })
      assertEquals(emptyList<Int>(), fileIndexer.find("foo").toList().sortedBy { it.tokenIndex })
      assertEquals(listOf(8L, 16L, 18L), fileIndexer.find("a").toList().sortedBy { it.tokenIndex })
    } finally {
      // Close the index file and delete the temporary file
      fileIndexer.close()
      tempDataFile.delete()
    }
  }

  @Test
  fun testSearchWords() {
    val tempDataFile = File.createTempFile("testData", ".txt")
    tempDataFile.writeText("This is a test data file.")
    val fileIndexer =
      FileIndexer(WordTokenFile(tempDataFile), File(tempDataFile.parentFile, "${tempDataFile.name}.index"))
    try {
      fileIndexer.buildIndex(CharPosition(1))
      fileIndexer.apply {
        val strings = (0 until index.getLength().element).toList()
          .map { CharPosition(index.get(ElementIndex(it))) }
          .map { data.charIterator(it) }
          .map { it.invoke().asSequence().take(data.tokenCount.asInt).joinToString("") }
        strings.mapIndexed { index, s -> println("$index: $s") }
        assertEquals(strings.toList().sorted().joinToString("\n"), strings.joinToString("\n"))
      }

      assertEquals(listOf(0L), fileIndexer.find("This").toList().sortedBy { it.tokenIndex })
      assertEquals(listOf(2L, 5L), fileIndexer.find("is").toList().sortedBy { it.tokenIndex })
      assertEquals(emptyList<Int>(), fileIndexer.find("foo").toList().sortedBy { it.tokenIndex })
      assertEquals(listOf(8L, 16L, 18L), fileIndexer.find("a").toList().sortedBy { it.tokenIndex })
    } finally {
      // Close the index file and delete the temporary file
      fileIndexer.close()
      tempDataFile.delete()
    }
  }

  @Test
  fun testDataFile() {
    val dataFile = File("C:\\Users\\andre\\Downloads\\pg84.txt")
//    val dataFile = File("C:\\Users\\andre\\Downloads\\pg100.txt")
    val indexFile = File(dataFile.parentFile, "${dataFile.name}.index")
    if (indexFile.exists()) indexFile.delete()
//    val indexer = FileIndexer(CharsetTokenFile(dataFile), indexFile)
    val indexer = FileIndexer(WordTokenFile(dataFile), indexFile)
    withPerf("buildIndex (${dataFile.length()} bytes)") { indexer.buildIndex(CharPosition(2)) }
    for (pos in withPerf("find") { indexer.find("This").toList() }.sortedBy { it.tokenIndex }) {
      println(indexer.data.readString(pos, CharPosition(100)).takeWhile { it != '\n' }.trim())
    }
    val characters = indexer.characters
    val dictionaryMaxSize = Integer.MAX_VALUE.toInt() - characters.size
    val codec = withPerf("findCompressionPrefixes") { indexer.findCompressionPrefixes(200, dictionaryMaxSize) }
    codec.forEach { (k, v) -> println("<$k>: $v") }
    val codecMap = (codec.map { it.first } + characters).sorted()
    val (compressed, dictionaryFile) = withPerf("data.compress") { indexer.data.writeCompressed(codecMap) }
    val expandFile = File(dataFile.parentFile, "${dataFile.name}.expand")
    withPerf("data.expand") { indexer.data.expand(codecMap, compressed, expandFile) }

    val compressedIndexer = FileIndexer(CompressedTokenFile(compressed, dictionaryFile))
    withPerf("buildIndex (${compressedIndexer.data.fileLength} bytes)") { compressedIndexer.buildIndex(CharPosition(3)) }
    for (pos in withPerf("find") { compressedIndexer.find("This").toList() }.sortedBy { it.tokenIndex }) {
//      println(compressedIndexer.data.readString(pos, 100).takeWhile { it != '\n' }.trim())
    }
  }

  private fun <T> withPerf(name: String, fn: () -> T): T {
    val startTime = System.nanoTime()
    try {
      return fn()
    } finally {
      val endTime = System.nanoTime()
      println("$name took ${(endTime - startTime) / 1e9} seconds")
    }
  }
}