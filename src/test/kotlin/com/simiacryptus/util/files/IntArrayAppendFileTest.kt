package com.simiacryptus.util.files

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class IntArrayAppendFileTest {

    @Test
    fun testAppendAndLength() {
        val tempFile = Files.createTempFile(null, null).toFile()
        tempFile.deleteOnExit()

        val intArrayAppendFile = IntArrayAppendFile(tempFile)
        assertEquals(XElements(0), intArrayAppendFile.length, "File length should be 0 before any append operations")

        intArrayAppendFile.append(42)
        assertEquals(XElements(1), intArrayAppendFile.length, "File length should be 1 after appending one element")

        intArrayAppendFile.append(-1)
        assertEquals(XElements(2), intArrayAppendFile.length, "File length should be 2 after appending two elements")

        intArrayAppendFile.close()
    }

    @Test
    fun testClose() {
        val tempFile = Files.createTempFile(null, null).toFile()
        tempFile.deleteOnExit()

        val intArrayAppendFile = IntArrayAppendFile(tempFile)
        intArrayAppendFile.close()

        assertThrows(IllegalStateException::class.java) {
            intArrayAppendFile.append(42)
        }
    }
}