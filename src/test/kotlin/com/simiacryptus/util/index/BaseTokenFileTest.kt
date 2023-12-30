package com.simiacryptus.util.index

import com.simiacryptus.util.files.ByteIndex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

abstract class BaseTokenFileTest {

    abstract fun createTokenFile(file: File): TokenFile

    @Test
    fun testRead() {
        val file = createTempFile()
        val tokenFile = createTokenFile(file)
        // Write some test data to the file
        file.writeText("Test data for reading")

        // Read data from the token file
        val buffer = ByteArray(file.length().toInt())
        tokenFile.read(ByteIndex(0), buffer)

        // Assert that the data read is correct
        assertEquals("Test data for reading", String(buffer))
    }

    @Test
    fun testWriteCompressed() {
        val file = createTempFile()
        val tokenFile = createTokenFile(file)
        // Define a simple codec for the test
        val codec = listOf("a", "b", "c")

        // Write some test data to the file
        file.writeText("abc")

        // Compress the data in the token file
        val (compressedFile, dictionaryFile) = tokenFile.writeCompressed(codec)

        // Assert that the compressed file and dictionary file are created
        assertTrue(compressedFile.exists())
        assertTrue(dictionaryFile.exists())

        // Assert that the compressed file is not empty
        assertNotEquals(0, compressedFile.length())
    }

    @Test
    fun testExpand() {
        val file = createTempFile()
        val compressedFile = createTempFile()
        val tokenFile = createTokenFile(file)
        // Define a simple codec for the test
        val codec = listOf("a", "b", "c")

        // Write some compressed test data to the compressed file
        // This would normally be done by the writeCompressed method, but for the test, we can simulate it
        compressedFile.writeText("0") // Assuming "0" represents "a" in the codec

        // Expand the compressed data in the token file
        tokenFile.expand(codec, compressedFile, file)

        // Read the expanded data from the file
        val expandedData = file.readText()

        // Assert that the expanded data is correct
        assertEquals("a", expandedData)
    }

    // Add more test methods as needed

    private fun createTempFile(): File {
        return File.createTempFile("test", ".tmp").apply {
            deleteOnExit()
        }
    }
}

class WordTokenFileTest : BaseTokenFileTest() {
    override fun createTokenFile(file: File) = WordTokenFile(file)
}

class SimpleTokenFileTest : BaseTokenFileTest() {
    override fun createTokenFile(file: File) = SimpleTokenFile(file)
}

class CharsetTokenFileTest : BaseTokenFileTest() {
    override fun createTokenFile(file: File) = CharsetTokenFile(file)
}

//class CompressedTokenFileTest : BaseTokenFileTest() {
//    override fun createTokenFile(file: File) = CompressedTokenFile(file)
//}


