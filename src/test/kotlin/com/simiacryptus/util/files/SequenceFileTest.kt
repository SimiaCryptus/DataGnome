package com.simiacryptus.util.files

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

internal class SequenceFileTest {

    private lateinit var sequenceFile: SequenceFile
    private lateinit var testFile: File

    @BeforeEach
    fun setUp() {
        testFile = Files.createTempFile(null, null).toFile()
        sequenceFile = SequenceFile(testFile)
    }

    @AfterEach
    fun tearDown() {
        sequenceFile.close()
        testFile.delete()
    }

    @Test
    fun `append and get should retrieve the same data`() {
        val data = "Hello, World!".toByteArray()
        val index = sequenceFile.append(data)
        assertArrayEquals(data, sequenceFile.get(index))
    }

    @Test
    fun `read should retrieve all appended data`() {
        val data1 = "Hello".toByteArray()
        val data2 = "World".toByteArray()
        sequenceFile.append(data1)
        sequenceFile.append(data2)
        val readData = sequenceFile.read()
        assertArrayEquals(data1, readData[0])
        assertArrayEquals(data2, readData[1])
    }

    @Test
    fun `get with invalid index should return null`() {
        assertNull(sequenceFile.get(XElements(999)))
    }

    @Test
    fun `append should increase the position`() {
        val initialPos = sequenceFile.append("First".toByteArray())
        val nextPos = sequenceFile.append("Second".toByteArray())
        assertTrue(nextPos > initialPos)
    }

    @Test
    fun `read on empty file should return empty array`() {
        assertArrayEquals(emptyArray<ByteArray>(), sequenceFile.read())
    }

    @Test
    fun `close should not throw exception`() {
        assertDoesNotThrow { sequenceFile.close() }
    }
}