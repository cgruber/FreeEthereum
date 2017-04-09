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

import org.ethereum.util.ByteUtil
import org.ethereum.vm.program.Program
import org.ethereum.vm.program.invoke.ProgramInvokeMockImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.nio.ByteBuffer

class ProgramMemoryTest {

    private val pi = ProgramInvokeMockImpl()
    internal var memory: ByteBuffer? = null
    private var program: Program? = null

    @Before
    fun createProgram() {
        program = Program(ByteUtil.EMPTY_BYTE_ARRAY, pi)
    }

    @Test
    fun testGetMemSize() {
        val memory = ByteArray(64)
        program!!.initMem(memory)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    @Ignore
    fun testMemorySave() {
        fail("Not yet implemented")
    }

    @Test
    @Ignore
    fun testMemoryLoad() {
        fail("Not yet implemented")
    }

    @Test
    fun testMemoryChunk1() {
        program!!.initMem(ByteArray(64))
        val offset = 128
        val size = 32
        program!!.memoryChunk(offset, size)
        assertEquals(160, program!!.memSize.toLong())
    }

    @Test // size 0 doesn't increase memory
    fun testMemoryChunk2() {
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 0
        program!!.memoryChunk(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory1() {

        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory2() {

        // memory.limit() > offset, == size
        // memory.limit() < offset + size
        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory3() {

        // memory.limit() > offset, > size
        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory4() {

        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory5() {

        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 0
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory6() {

        // memory.limit() == offset, > size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory7() {

        // memory.limit() == offset - size
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(128, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory8() {

        program!!.initMem(ByteArray(64))
        val offset = 0
        val size = 96
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory9() {

        // memory.limit() < offset, > size
        // memory.limit() < offset - size
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 0
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    /** */


    @Test
    fun testAllocateMemory10() {

        // memory = null, offset > size
        val offset = 32
        val size = 0
        program!!.allocateMemory(offset, size)
        assertEquals(0, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory11() {

        // memory = null, offset < size
        val offset = 0
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(32, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory12() {

        // memory.limit() < offset, < size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 96
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory13() {

        // memory.limit() > offset, < size
        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 128
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory14() {

        // memory.limit() < offset, == size
        program!!.initMem(ByteArray(64))
        val offset = 96
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory15() {

        // memory.limit() == offset, < size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 96
        program!!.allocateMemory(offset, size)
        assertEquals(160, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory16() {

        // memory.limit() == offset, == size
        // memory.limit() > offset - size
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 64
        program!!.allocateMemory(offset, size)
        assertEquals(128, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemory17() {

        // memory.limit() > offset + size
        program!!.initMem(ByteArray(96))
        val offset = 32
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded1() {

        // memory unrounded
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded2() {

        // offset unrounded
        program!!.initMem(ByteArray(64))
        val offset = 16
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded3() {

        // size unrounded
        program!!.initMem(ByteArray(64))
        val offset = 64
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(96, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded4() {

        // memory + offset unrounded
        program!!.initMem(ByteArray(64))
        val offset = 16
        val size = 32
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded5() {

        // memory + size unrounded
        program!!.initMem(ByteArray(64))
        val offset = 32
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(64, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded6() {

        // offset + size unrounded
        program!!.initMem(ByteArray(32))
        val offset = 16
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(32, program!!.memSize.toLong())
    }

    @Test
    fun testAllocateMemoryUnrounded7() {

        // memory + offset + size unrounded
        program!!.initMem(ByteArray(32))
        val offset = 16
        val size = 16
        program!!.allocateMemory(offset, size)
        assertEquals(32, program!!.memSize.toLong())
    }

    @Ignore
    @Test
    fun testInitialInsert() {


        // todo: fix the array out of bound here
        val offset = 32
        val size = 0
        program!!.memorySave(32, 0, ByteArray(0))
        assertEquals(32, program!!.memSize.toLong())
    }
}