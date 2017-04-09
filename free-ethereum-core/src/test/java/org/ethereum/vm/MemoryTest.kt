/*
 * The MIT License (MIT)
 *
 * Copyright 2017 Alexander Orlov <alexander.orlov@loxal.net>. All rights reserved.
 * Copyright (c) [2016] [ <ether.camp> ]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package org.ethereum.vm

import org.ethereum.vm.program.Memory
import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.util.encoders.Hex
import java.lang.Math.ceil
import java.util.*

class MemoryTest {

    @Test
    fun testExtend() {
        checkMemoryExtend(0)
        checkMemoryExtend(1)
        checkMemoryExtend(WORD_SIZE)
        checkMemoryExtend(WORD_SIZE * 2)
        checkMemoryExtend(CHUNK_SIZE - 1)
        checkMemoryExtend(CHUNK_SIZE)
        checkMemoryExtend(CHUNK_SIZE + 1)
        checkMemoryExtend(2000)
    }

    @Test
    fun memorySave_1() {

        val memoryBuffer = Memory()
        val data = byteArrayOf(1, 1, 1, 1)

        memoryBuffer.write(0, data, data.size, false)

        assertTrue(1 == memoryBuffer.chunks.size)

        val chunk = memoryBuffer.chunks[0]
        assertTrue(chunk[0].toInt() == 1)
        assertTrue(chunk[1].toInt() == 1)
        assertTrue(chunk[2].toInt() == 1)
        assertTrue(chunk[3].toInt() == 1)
        assertTrue(chunk[4].toInt() == 0)

        assertTrue(memoryBuffer.size() == 32)
    }

    @Test
    fun memorySave_2() {

        val memoryBuffer = Memory()
        val data = Hex.decode("0101010101010101010101010101010101010101010101010101010101010101")

        memoryBuffer.write(0, data, data.size, false)

        assertTrue(1 == memoryBuffer.chunks.size)

        val chunk = memoryBuffer.chunks[0]
        assertTrue(chunk[0].toInt() == 1)
        assertTrue(chunk[1].toInt() == 1)

        assertTrue(chunk[30].toInt() == 1)
        assertTrue(chunk[31].toInt() == 1)
        assertTrue(chunk[32].toInt() == 0)

        assertTrue(memoryBuffer.size() == 32)
    }

    @Test
    fun memorySave_3() {

        val memoryBuffer = Memory()
        val data = Hex.decode("010101010101010101010101010101010101010101010101010101010101010101")

        memoryBuffer.write(0, data, data.size, false)

        assertTrue(1 == memoryBuffer.chunks.size)

        val chunk = memoryBuffer.chunks[0]
        assertTrue(chunk[0].toInt() == 1)
        assertTrue(chunk[1].toInt() == 1)

        assertTrue(chunk[30].toInt() == 1)
        assertTrue(chunk[31].toInt() == 1)
        assertTrue(chunk[32].toInt() == 1)
        assertTrue(chunk[33].toInt() == 0)

        assertTrue(memoryBuffer.size() == 64)
    }

    @Test
    fun memorySave_4() {

        val memoryBuffer = Memory()
        val data = ByteArray(1024)
        Arrays.fill(data, 1.toByte())

        memoryBuffer.write(0, data, data.size, false)

        assertTrue(1 == memoryBuffer.chunks.size)

        val chunk = memoryBuffer.chunks[0]
        assertTrue(chunk[0].toInt() == 1)
        assertTrue(chunk[1].toInt() == 1)

        assertTrue(chunk[1022].toInt() == 1)
        assertTrue(chunk[1023].toInt() == 1)

        assertTrue(memoryBuffer.size() == 1024)
    }

    @Test
    fun memorySave_5() {

        val memoryBuffer = Memory()

        val data = ByteArray(1025)
        Arrays.fill(data, 1.toByte())

        memoryBuffer.write(0, data, data.size, false)

        assertTrue(2 == memoryBuffer.chunks.size)

        val chunk1 = memoryBuffer.chunks[0]
        assertTrue(chunk1[0].toInt() == 1)
        assertTrue(chunk1[1].toInt() == 1)

        assertTrue(chunk1[1022].toInt() == 1)
        assertTrue(chunk1[1023].toInt() == 1)

        val chunk2 = memoryBuffer.chunks[1]
        assertTrue(chunk2[0].toInt() == 1)
        assertTrue(chunk2[1].toInt() == 0)

        assertTrue(memoryBuffer.size() == 1056)
    }

    @Test
    fun memorySave_6() {

        val memoryBuffer = Memory()

        val data1 = ByteArray(1024)
        Arrays.fill(data1, 1.toByte())

        val data2 = ByteArray(1024)
        Arrays.fill(data2, 2.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        memoryBuffer.write(1024, data2, data2.size, false)

        assertTrue(2 == memoryBuffer.chunks.size)

        val chunk1 = memoryBuffer.chunks[0]
        assertTrue(chunk1[0].toInt() == 1)
        assertTrue(chunk1[1].toInt() == 1)

        assertTrue(chunk1[1022].toInt() == 1)
        assertTrue(chunk1[1023].toInt() == 1)

        val chunk2 = memoryBuffer.chunks[1]
        assertTrue(chunk2[0].toInt() == 2)
        assertTrue(chunk2[1].toInt() == 2)

        assertTrue(chunk2[1022].toInt() == 2)
        assertTrue(chunk2[1023].toInt() == 2)

        assertTrue(memoryBuffer.size() == 2048)
    }

    @Test
    fun memorySave_7() {

        val memoryBuffer = Memory()

        val data1 = ByteArray(1024)
        Arrays.fill(data1, 1.toByte())

        val data2 = ByteArray(1024)
        Arrays.fill(data2, 2.toByte())

        val data3 = ByteArray(1)
        Arrays.fill(data3, 3.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        memoryBuffer.write(1024, data2, data2.size, false)
        memoryBuffer.write(2048, data3, data3.size, false)

        assertTrue(3 == memoryBuffer.chunks.size)

        val chunk1 = memoryBuffer.chunks[0]
        assertTrue(chunk1[0].toInt() == 1)
        assertTrue(chunk1[1].toInt() == 1)

        assertTrue(chunk1[1022].toInt() == 1)
        assertTrue(chunk1[1023].toInt() == 1)

        val chunk2 = memoryBuffer.chunks[1]
        assertTrue(chunk2[0].toInt() == 2)
        assertTrue(chunk2[1].toInt() == 2)

        assertTrue(chunk2[1022].toInt() == 2)
        assertTrue(chunk2[1023].toInt() == 2)

        val chunk3 = memoryBuffer.chunks[2]
        assertTrue(chunk3[0].toInt() == 3)

        assertTrue(memoryBuffer.size() == 2080)
    }

    @Test
    fun memorySave_8() {

        val memoryBuffer = Memory()

        val data1 = ByteArray(128)
        Arrays.fill(data1, 1.toByte())

        memoryBuffer.extendAndWrite(0, 256, data1)

        var ones = 0
        var zeroes = 0
        for (i in 0..memoryBuffer.size() - 1) {
            if (memoryBuffer.readByte(i).toInt() == 1) ++ones
            if (memoryBuffer.readByte(i).toInt() == 0) ++zeroes
        }

        assertTrue(ones == zeroes)
        assertTrue(256 == memoryBuffer.size())
    }


    @Test
    fun memoryLoad_1() {

        val memoryBuffer = Memory()
        val value = memoryBuffer.readWord(100)
        assertTrue(value.intValue() == 0)
        assertTrue(memoryBuffer.chunks.size == 1)
        assertTrue(memoryBuffer.size() == 32 * 5)
    }

    @Test
    fun memoryLoad_2() {

        val memoryBuffer = Memory()
        val value = memoryBuffer.readWord(2015)
        assertTrue(value.intValue() == 0)
        assertTrue(memoryBuffer.chunks.size == 2)
        assertTrue(memoryBuffer.size() == 2048)
    }

    @Test
    fun memoryLoad_3() {

        val memoryBuffer = Memory()
        val value = memoryBuffer.readWord(2016)
        assertTrue(value.intValue() == 0)
        assertTrue(memoryBuffer.chunks.size == 2)
        assertTrue(memoryBuffer.size() == 2048)
    }

    @Test
    fun memoryLoad_4() {

        val memoryBuffer = Memory()
        val value = memoryBuffer.readWord(2017)
        assertTrue(value.intValue() == 0)
        assertTrue(memoryBuffer.chunks.size == 3)
        assertTrue(memoryBuffer.size() == 2080)
    }

    @Test
    fun memoryLoad_5() {

        val memoryBuffer = Memory()

        val data1 = ByteArray(1024)
        Arrays.fill(data1, 1.toByte())

        val data2 = ByteArray(1024)
        Arrays.fill(data2, 2.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        memoryBuffer.write(1024, data2, data2.size, false)

        assertTrue(memoryBuffer.chunks.size == 2)
        assertTrue(memoryBuffer.size() == 2048)

        val val1 = memoryBuffer.readWord(0x3df)
        val val2 = memoryBuffer.readWord(0x3e0)
        val val3 = memoryBuffer.readWord(0x3e1)

        assertArrayEquals(
                Hex.decode("0101010101010101010101010101010101010101010101010101010101010101"),
                val1.data)

        assertArrayEquals(
                Hex.decode("0101010101010101010101010101010101010101010101010101010101010101"),
                val2.data)

        assertArrayEquals(
                Hex.decode("0101010101010101010101010101010101010101010101010101010101010102"),
                val3.data)
        assertTrue(memoryBuffer.size() == 2048)
    }


    @Test
    fun memoryChunk_1() {
        val memoryBuffer = Memory()

        val data1 = ByteArray(32)
        Arrays.fill(data1, 1.toByte())

        val data2 = ByteArray(32)
        Arrays.fill(data2, 2.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        memoryBuffer.write(32, data2, data2.size, false)

        val data = memoryBuffer.read(0, 64)

        assertArrayEquals(
                Hex.decode("0101010101010101010101010101010101010101010101010101010101010101" + "0202020202020202020202020202020202020202020202020202020202020202"),
                data
        )

        assertEquals(64, memoryBuffer.size().toLong())
    }


    @Test
    fun memoryChunk_2() {
        val memoryBuffer = Memory()

        val data1 = ByteArray(32)
        Arrays.fill(data1, 1.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        assertTrue(32 == memoryBuffer.size())

        val data = memoryBuffer.read(0, 64)

        assertArrayEquals(
                Hex.decode("0101010101010101010101010101010101010101010101010101010101010101" + "0000000000000000000000000000000000000000000000000000000000000000"),
                data
        )

        assertEquals(64, memoryBuffer.size().toLong())
    }

    @Test
    fun memoryChunk_3() {

        val memoryBuffer = Memory()

        val data1 = ByteArray(1024)
        Arrays.fill(data1, 1.toByte())

        val data2 = ByteArray(1024)
        Arrays.fill(data2, 2.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        memoryBuffer.write(1024, data2, data2.size, false)

        val data = memoryBuffer.read(0, 2048)

        var ones = 0
        var twos = 0
        for (aData in data) {
            if (aData.toInt() == 1) ++ones
            if (aData.toInt() == 2) ++twos
        }

        assertTrue(ones == twos)
        assertTrue(2048 == memoryBuffer.size())
    }

    @Test
    fun memoryChunk_4() {

        val memoryBuffer = Memory()

        val data1 = ByteArray(1024)
        Arrays.fill(data1, 1.toByte())

        val data2 = ByteArray(1024)
        Arrays.fill(data2, 2.toByte())

        memoryBuffer.write(0, data1, data1.size, false)
        memoryBuffer.write(1024, data2, data2.size, false)

        val data = memoryBuffer.read(0, 2049)

        var ones = 0
        var twos = 0
        var zero = 0
        for (aData in data) {
            if (aData.toInt() == 1) ++ones
            if (aData.toInt() == 2) ++twos
            if (aData.toInt() == 0) ++zero
        }

        assertTrue(zero == 1)
        assertTrue(ones == twos)
        assertTrue(2080 == memoryBuffer.size())
    }


    @Test
    fun memoryWriteLimited_1() {

        val memoryBuffer = Memory()
        memoryBuffer.extend(0, 3072)

        val data1 = ByteArray(6272)
        Arrays.fill(data1, 1.toByte())

        memoryBuffer.write(2720, data1, data1.size, true)

        val lastZero = memoryBuffer.readByte(2719)
        val firstOne = memoryBuffer.readByte(2721)

        assertTrue(memoryBuffer.size() == 3072)
        assertTrue(lastZero.toInt() == 0)
        assertTrue(firstOne.toInt() == 1)

        val data = memoryBuffer.read(2720, 352)

        var ones = 0
        var zero = 0
        for (aData in data) {
            if (aData.toInt() == 1) ++ones
            if (aData.toInt() == 0) ++zero
        }

        assertTrue(ones == data.size)
        assertTrue(zero == 0)
    }

    @Test
    fun memoryWriteLimited_2() {

        val memoryBuffer = Memory()
        memoryBuffer.extend(0, 3072)

        val data1 = ByteArray(6272)
        Arrays.fill(data1, 1.toByte())

        memoryBuffer.write(2720, data1, 300, true)

        val lastZero = memoryBuffer.readByte(2719)
        val firstOne = memoryBuffer.readByte(2721)

        assertTrue(memoryBuffer.size() == 3072)
        assertTrue(lastZero.toInt() == 0)
        assertTrue(firstOne.toInt() == 1)

        val data = memoryBuffer.read(2720, 352)

        var ones = 0
        var zero = 0
        for (aData in data) {
            if (aData.toInt() == 1) ++ones
            if (aData.toInt() == 0) ++zero
        }

        assertTrue(ones == 300)
        assertTrue(zero == 52)
    }

    @Test
    fun memoryWriteLimited_3() {

        val memoryBuffer = Memory()
        memoryBuffer.extend(0, 128)

        val data1 = ByteArray(20)
        Arrays.fill(data1, 1.toByte())

        memoryBuffer.write(10, data1, 40, true)

        val lastZero = memoryBuffer.readByte(9)
        val firstOne = memoryBuffer.readByte(10)

        assertTrue(memoryBuffer.size() == 128)
        assertTrue(lastZero.toInt() == 0)
        assertTrue(firstOne.toInt() == 1)

        val data = memoryBuffer.read(10, 30)

        var ones = 0
        var zero = 0
        for (aData in data) {
            if (aData.toInt() == 1) ++ones
            if (aData.toInt() == 0) ++zero
        }

        assertTrue(ones == 20)
        assertTrue(zero == 10)
    }

    companion object {

        private val WORD_SIZE = 32
        private val CHUNK_SIZE = 1024

        private fun checkMemoryExtend(dataSize: Int) {
            val memory = Memory()
            memory.extend(0, dataSize)
            assertEquals(calcSize(dataSize, CHUNK_SIZE).toLong(), memory.internalSize().toLong())
            assertEquals(calcSize(dataSize, WORD_SIZE).toLong(), memory.size().toLong())
        }

        private fun calcSize(dataSize: Int, chunkSize: Int): Int {
            return ceil(dataSize.toDouble() / chunkSize).toInt() * chunkSize
        }
    }


}