package com.simiacryptus.util.files

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

internal class IntArrayMappedFileTest {

    @Test
    fun testLength() {
        val tempFile = createTempDataFile(intArrayOf(1, 2, 3, 4))
        val mappedFile = IntArrayMappedFile(tempFile)
        assertEquals(4, mappedFile.length.asLong)
    }

    @Test
    fun testGet() {
        val tempFile = createTempDataFile(intArrayOf(10, 20, 30, 40))
        val mappedFile = IntArrayMappedFile(tempFile)
        assertEquals(10, mappedFile.get(XElements(0)))
        assertEquals(20, mappedFile.get(XElements(1)))
        assertEquals(30, mappedFile.get(XElements(2)))
        assertEquals(40, mappedFile.get(XElements(3)))
    }

    @Test
    fun testClose() {
        val tempFile = createTempDataFile(intArrayOf(1, 2, 3, 4))
        val mappedFile = IntArrayMappedFile(tempFile)
        assertDoesNotThrow { mappedFile.close() }
    }

    private fun createTempDataFile(data: IntArray): File {
        val tempFile = Files.createTempFile(null, null).toFile()
        tempFile.deleteOnExit()
        RandomAccessFile(tempFile, "rw").use { raf ->
            data.forEach { value ->
                raf.writeInt(value)
            }
        }
        return tempFile
    }
}