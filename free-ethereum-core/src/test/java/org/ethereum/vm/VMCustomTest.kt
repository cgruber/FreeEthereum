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

import org.ethereum.vm.program.Program
import org.ethereum.vm.program.Program.OutOfGasException
import org.ethereum.vm.program.Program.StackTooSmallException
import org.ethereum.vm.program.invoke.ProgramInvokeMockImpl
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runners.MethodSorters
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

/**
 * @author Roman Mandeleil
 * *
 * @since 01.06.2014
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class VMCustomTest {

    private var invoke: ProgramInvokeMockImpl? = null
    private var program: Program? = null

    @Before
    fun setup() {
        val ownerAddress = Hex.decode("77045E71A7A2C50903D88E564CD72FAB11E82051")
        val msgData = Hex.decode("00000000000000000000000000000000000000000000000000000000000000A1" + "00000000000000000000000000000000000000000000000000000000000000B1")

        invoke = ProgramInvokeMockImpl(msgData)
        invoke!!.setOwnerAddress(ownerAddress)

        invoke!!.repository.createAccount(ownerAddress)
        invoke!!.repository.addBalance(ownerAddress, BigInteger.valueOf(1000L))
    }

    @After
    fun tearDown() {
        invoke!!.repository.close()
    }

    @Test // CALLDATASIZE OP
    fun testCALLDATASIZE_1() {

        val vm = VM()
        program = Program(Hex.decode("36"), invoke)
        val s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000040"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }


    @Test // CALLDATALOAD OP
    fun testCALLDATALOAD_1() {

        val vm = VM()
        program = Program(Hex.decode("600035"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000000000A1"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // CALLDATALOAD OP
    fun testCALLDATALOAD_2() {

        val vm = VM()
        program = Program(Hex.decode("600235"), invoke)
        val s_expected_1 = "0000000000000000000000000000000000000000000000000000000000A10000"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }


    @Test // CALLDATALOAD OP
    fun testCALLDATALOAD_3() {

        val vm = VM()
        program = Program(Hex.decode("602035"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000000000B1"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }


    @Test // CALLDATALOAD OP
    fun testCALLDATALOAD_4() {

        val vm = VM()
        program = Program(Hex.decode("602335"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000B1000000"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // CALLDATALOAD OP
    fun testCALLDATALOAD_5() {

        val vm = VM()
        program = Program(Hex.decode("603F35"), invoke)
        val s_expected_1 = "B100000000000000000000000000000000000000000000000000000000000000"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test(expected = RuntimeException::class) // CALLDATALOAD OP mal
    fun testCALLDATALOAD_6() {

        val vm = VM()
        program = Program(Hex.decode("35"), invoke)
        try {
            vm.step(program)
        } finally {
            assertTrue(program!!.isStopped)
        }
    }

    @Test // CALLDATACOPY OP
    fun testCALLDATACOPY_1() {

        val vm = VM()
        program = Program(Hex.decode("60206000600037"), invoke)
        val m_expected = "00000000000000000000000000000000000000000000000000000000000000A1"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        assertEquals(m_expected, Hex.toHexString(program!!.memory).toUpperCase())
    }

    @Test // CALLDATACOPY OP
    fun testCALLDATACOPY_2() {

        val vm = VM()
        program = Program(Hex.decode("60406000600037"), invoke)
        val m_expected = "00000000000000000000000000000000000000000000000000000000000000A1" + "00000000000000000000000000000000000000000000000000000000000000B1"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        assertEquals(m_expected, Hex.toHexString(program!!.memory).toUpperCase())
    }


    @Test // CALLDATACOPY OP
    fun testCALLDATACOPY_3() {

        val vm = VM()
        program = Program(Hex.decode("60406004600037"), invoke)
        val m_expected = "000000000000000000000000000000000000000000000000000000A100000000" + "000000000000000000000000000000000000000000000000000000B100000000"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        assertEquals(m_expected, Hex.toHexString(program!!.memory).toUpperCase())
    }


    @Test // CALLDATACOPY OP
    fun testCALLDATACOPY_4() {

        val vm = VM()
        program = Program(Hex.decode("60406000600437"), invoke)
        val m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000A100000000000000000000000000000000000000000000000000000000" +
                "000000B100000000000000000000000000000000000000000000000000000000"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        assertEquals(m_expected, Hex.toHexString(program!!.memory).toUpperCase())
    }

    @Test // CALLDATACOPY OP
    fun testCALLDATACOPY_5() {

        val vm = VM()
        program = Program(Hex.decode("60406000600437"), invoke)
        val m_expected = "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000A100000000000000000000000000000000000000000000000000000000" +
                "000000B100000000000000000000000000000000000000000000000000000000"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        assertEquals(m_expected, Hex.toHexString(program!!.memory).toUpperCase())
    }


    @Test(expected = StackTooSmallException::class) // CALLDATACOPY OP mal
    fun testCALLDATACOPY_6() {

        val vm = VM()
        program = Program(Hex.decode("6040600037"), invoke)

        try {
            vm.step(program)
            vm.step(program)
            vm.step(program)
        } finally {
            assertTrue(program!!.isStopped)
        }
    }

    @Test(expected = OutOfGasException::class) // CALLDATACOPY OP mal
    fun testCALLDATACOPY_7() {

        val vm = VM()
        program = Program(Hex.decode("6020600073CC0929EB16730E7C14FEFC63006AC2D794C5795637"), invoke)

        try {
            vm.step(program)
            vm.step(program)
            vm.step(program)
            vm.step(program)
        } finally {
            assertTrue(program!!.isStopped)
        }
    }

    @Test // ADDRESS OP
    fun testADDRESS_1() {

        val vm = VM()
        program = Program(Hex.decode("30"), invoke)
        val s_expected_1 = "00000000000000000000000077045E71A7A2C50903D88E564CD72FAB11E82051"

        vm.step(program)

        val item1 = program!!.stackPop()
        program!!.storage.close()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // BALANCE OP
    fun testBALANCE_1() {

        val vm = VM()
        program = Program(Hex.decode("3031"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000000003E8"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // ORIGIN OP
    fun testORIGIN_1() {

        val vm = VM()
        program = Program(Hex.decode("32"), invoke)
        val s_expected_1 = "00000000000000000000000013978AEE95F38490E9769C39B2773ED763D9CD5F"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // CALLER OP
    fun testCALLER_1() {

        val vm = VM()
        program = Program(Hex.decode("33"), invoke)
        val s_expected_1 = "000000000000000000000000885F93EED577F2FC341EBB9A5C9B2CE4465D96C4"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // CALLVALUE OP
    fun testCALLVALUE_1() {

        val vm = VM()
        program = Program(Hex.decode("34"), invoke)
        val s_expected_1 = "0000000000000000000000000000000000000000000000000DE0B6B3A7640000"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // SHA3 OP
    fun testSHA3_1() {

        val vm = VM()
        program = Program(Hex.decode("60016000536001600020"), invoke)
        val s_expected_1 = "5FE7F977E71DBA2EA1A68E21057BEEBB9BE2AC30C6410AA38D4F3FBE41DCFFD2"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // SHA3 OP
    fun testSHA3_2() {

        val vm = VM()
        program = Program(Hex.decode("6102016000526002601E20"), invoke)
        val s_expected_1 = "114A3FE82A0219FCC31ABD15617966A125F12B0FD3409105FC83B487A9D82DE4"

        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test(expected = StackTooSmallException::class) // SHA3 OP mal
    fun testSHA3_3() {

        val vm = VM()
        program = Program(Hex.decode("610201600052600220"), invoke)
        try {
            vm.step(program)
            vm.step(program)
            vm.step(program)
            vm.step(program)
            vm.step(program)
        } finally {
            assertTrue(program!!.isStopped)
        }
    }

    @Test // BLOCKHASH OP
    fun testBLOCKHASH_1() {

        val vm = VM()
        program = Program(Hex.decode("600140"), invoke)
        val s_expected_1 = "C89EFDAA54C0F20C7ADF612882DF0950F5A951637E0307CDCB4C672F298B8BC6"

        vm.step(program)
        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // COINBASE OP
    fun testCOINBASE_1() {

        val vm = VM()
        program = Program(Hex.decode("41"), invoke)
        val s_expected_1 = "000000000000000000000000E559DE5527492BCB42EC68D07DF0742A98EC3F1E"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // TIMESTAMP OP
    fun testTIMESTAMP_1() {

        val vm = VM()
        program = Program(Hex.decode("42"), invoke)
        val s_expected_1 = "000000000000000000000000000000000000000000000000000000005387FE24"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // NUMBER OP
    fun testNUMBER_1() {

        val vm = VM()
        program = Program(Hex.decode("43"), invoke)
        val s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000021"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // DIFFICULTY OP
    fun testDIFFICULTY_1() {

        val vm = VM()
        program = Program(Hex.decode("44"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000003ED290"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // GASPRICE OP
    fun testGASPRICE_1() {

        val vm = VM()
        program = Program(Hex.decode("3A"), invoke)
        val s_expected_1 = "000000000000000000000000000000000000000000000000000009184E72A000"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Ignore //TODO #POC9
    @Test // GAS OP
    fun testGAS_1() {

        val vm = VM()
        program = Program(Hex.decode("5A"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000000F423F"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test // GASLIMIT OP
    fun testGASLIMIT_1() {

        val vm = VM()
        program = Program(Hex.decode("45"), invoke)
        val s_expected_1 = "00000000000000000000000000000000000000000000000000000000000F4240"

        vm.step(program)

        val item1 = program!!.stackPop()
        assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
    }

    @Test(expected = Program.IllegalOperationException::class) // INVALID OP
    fun testINVALID_1() {

        val vm = VM()
        program = Program(Hex.decode("60012F6002"), invoke)
        val s_expected_1 = "0000000000000000000000000000000000000000000000000000000000000001"

        try {
            vm.step(program)
            vm.step(program)
        } finally {
            assertTrue(program!!.isStopped)
            val item1 = program!!.stackPop()
            assertEquals(s_expected_1, Hex.toHexString(item1.data).toUpperCase())
        }
    }

    /* TEST CASE LIST END */

}

// TODO: add gas expeted and calculated to all test cases
// TODO: considering: G_TXDATA + G_TRANSACTION

/**
 * TODO:

 * 22) CREATE:
 * 23) CALL:


 */

/**

 * contract creation (gas usage)
 * -----------------------------
 * G_TRANSACTION =                                (500)
 * 60016000546006601160003960066000f261778e600054 (115)
 * PUSH1    6001 (1)
 * PUSH1    6000 (1)
 * MSTORE   54   (1 + 1)
 * PUSH1    6006 (1)
 * PUSH1    6011 (1)
 * PUSH1    6000 (1)
 * CODECOPY 39   (1)
 * PUSH1    6006 (1)
 * PUSH1    6000 (1)
 * RETURN   f2   (1)
 * 61778e600054

 */
