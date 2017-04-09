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

package org.ethereum.core

import org.ethereum.crypto.HashUtil.sha3
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex

/**
 * @author Anton Nashatyrev
 */
class ABITest {

    @Test
    fun testTransactionCreate() {
        // demo only
        val function = CallTransaction.Function.fromJsonInterface(funcJson1)
        val ctx = CallTransaction.createCallTransaction(1, 1000000000, 1000000000,
                "86e0497e32a8e1d79fe38ab87dc80140df5470d9", 0, function, "1234567890abcdef1234567890abcdef12345678")
        ctx.sign(sha3("974f963ee4571e86e5f9bc3b493e453db9c15e5bd19829a4ef9a790de0da0015".toByteArray()))
    }

    @Test
    fun testSimple1() {

        logger.info("\n{}", funcJson1)

        val function = CallTransaction.Function.fromJsonInterface(funcJson1)

        Assert.assertEquals("5c19a95c0000000000000000000000001234567890abcdef1234567890abcdef12345678",
                Hex.toHexString(function.encode("1234567890abcdef1234567890abcdef12345678")))
        Assert.assertEquals("5c19a95c0000000000000000000000001234567890abcdef1234567890abcdef12345678",
                Hex.toHexString(function.encode("0x1234567890abcdef1234567890abcdef12345678")))
        try {
            Hex.toHexString(function.encode("0xa1234567890abcdef1234567890abcdef12345678"))
            Assert.assertTrue(false)
        } catch (e: Exception) {
        }

        try {
            Hex.toHexString(function.encode("blabla"))
            Assert.assertTrue(false)
        } catch (e: Exception) {
        }

    }

    @Test
    fun testSimple2() {

        logger.info("\n{}", funcJson2)

        val function = CallTransaction.Function.fromJsonInterface(funcJson2)
        val ctx = CallTransaction.createCallTransaction(1, 1000000000, 1000000000,
                "86e0497e32a8e1d79fe38ab87dc80140df5470d9", 0, function)
        ctx.sign(sha3("974f963ee4571e86e5f9bc3b493e453db9c15e5bd19829a4ef9a790de0da0015".toByteArray()))

        Assert.assertEquals("91888f2e", Hex.toHexString(ctx.data))
    }

    @Test
    fun test3() {

        logger.info("\n{}", funcJson3)

        val function = CallTransaction.Function.fromJsonInterface(funcJson3)

        Assert.assertEquals("a4f72f5a" +
                "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffb2e" +
                "00000000000000000000000000000000000000000000000000000000000004d2" +
                "000000000000000000000000000000000000000000000000000000000000007b61" +
                "000000000000000000000000000000000000000000000000000000000000007468" +
                "6520737472696e6700000000000000000000000000000000000000000000",
                Hex.toHexString(function.encode(-1234, 1234, 123, "a", "the string")))
    }

    @Test
    fun test4() {

        logger.info("\n{}", funcJson4)

        val function = CallTransaction.Function.fromJsonInterface(funcJson4)
        Assert.assertEquals("d383b9f6" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000002" +
                "0000000000000000000000000000000000000000000000000000000000000003",
                Hex.toHexString(function.encode(intArrayOf(1, 2, 3))))

        Assert.assertEquals(
                "d383b9f60000000000000000000000000000000000000000000000000000000000000001" +
                        "0000000000000000000000000000000000000000000000000000000000000002" +
                        "0000000000000000000000000000000000000000000000000000000000000003" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000002" +
                        "0000000000000000000000000000000000000000000000000000000000000004" +
                        "0000000000000000000000000000000000000000000000000000000000000005",
                Hex.toHexString(function.encode(intArrayOf(1, 2, 3), intArrayOf(4, 5))))

    }

    @Test
    fun test5() {

        logger.info("\n{}", funcJson5)

        val function = CallTransaction.Function.fromJsonInterface(funcJson5)

        Assert.assertEquals(
                "3ed2792b000000000000000000000000000000000000000000000000000000000000006f" +
                        "0000000000000000000000000000000000000000000000000000000000000060" +
                        "00000000000000000000000000000000000000000000000000000000000000de" +
                        "0000000000000000000000000000000000000000000000000000000000000003" +
                        "abcdef0000000000000000000000000000000000000000000000000000000000",
                Hex.toHexString(function.encode(111, byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte()), 222)))

    }

    @Test
    fun decodeDynamicTest1() {
        var funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'bytes'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "   'outputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'bytes'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n"
        funcJson = funcJson.replace("'".toRegex(), "\"")

        val function = CallTransaction.Function.fromJsonInterface(funcJson)
        val bytes = byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte())
        val encoded = function.encodeArguments(111, bytes, 222)
        val objects = function.decodeResult(encoded)
        //        System.out.println(Arrays.toString(objects));
        Assert.assertEquals((objects[0] as Number).toInt().toLong(), 111)
        Assert.assertArrayEquals(objects[1] as ByteArray, bytes)
        Assert.assertEquals((objects[2] as Number).toInt().toLong(), 222)
    }

    @Test
    fun decodeDynamicTest2() {
        var funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "   'outputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'type':'function' \n" +
                "}\n"
        funcJson = funcJson.replace("'".toRegex(), "\"")

        val function = CallTransaction.Function.fromJsonInterface(funcJson)
        val strings = arrayOf("aaa", "long string: 123456789012345678901234567890", "ccc")
        val encoded = function.encodeArguments(111, strings, 222)
        val objects = function.decodeResult(encoded)
        //        System.out.println(Arrays.toString(objects));
        Assert.assertEquals((objects[0] as Number).toInt().toLong(), 111)
        Assert.assertArrayEquals(objects[1] as Array<Any>, strings)
        Assert.assertEquals((objects[2] as Number).toInt().toLong(), 222)
    }

    @Test
    fun decodeWithUnknownPropertiesTest() {
        var funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "   'outputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'type':'function', \n" +
                "    'test':'test' \n" +
                "}\n"
        funcJson = funcJson.replace("'".toRegex(), "\"")

        val function = CallTransaction.Function.fromJsonInterface(funcJson)
        val strings = arrayOf("aaa", "long string: 123456789012345678901234567890", "ccc")
        val encoded = function.encodeArguments(111, strings, 222)
        val objects = function.decodeResult(encoded)
        Assert.assertEquals((objects[0] as Number).toInt().toLong(), 111)
        Assert.assertArrayEquals(objects[1] as Array<Any>, strings)
        Assert.assertEquals((objects[2] as Number).toInt().toLong(), 222)
    }

    @Test
    fun decodeWithPayablePropertyTest() {
        var funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "   'outputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'type':'function', \n" +
                "    'payable':true \n" +
                "}\n"
        funcJson = funcJson.replace("'".toRegex(), "\"")

        val function = CallTransaction.Function.fromJsonInterface(funcJson)
        Assert.assertTrue(function.payable)
        val strings = arrayOf("aaa", "long string: 123456789012345678901234567890", "ccc")
        val encoded = function.encodeArguments(111, strings, 222)
        val objects = function.decodeResult(encoded)
        Assert.assertEquals((objects[0] as Number).toInt().toLong(), 111)
        Assert.assertArrayEquals(objects[1] as Array<Any>, strings)
        Assert.assertEquals((objects[2] as Number).toInt().toLong(), 222)
    }

    @Test
    fun decodeWithFunctionTypeFallbackTest() {
        var funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "   'outputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'type':'fallback' \n" +
                "}\n"
        funcJson = funcJson.replace("'".toRegex(), "\"")

        val function = CallTransaction.Function.fromJsonInterface(funcJson)
        Assert.assertEquals(CallTransaction.FunctionType.fallback, function.type)
        val strings = arrayOf("aaa", "long string: 123456789012345678901234567890", "ccc")
        val encoded = function.encodeArguments(111, strings, 222)
        val objects = function.decodeResult(encoded)
        Assert.assertEquals((objects[0] as Number).toInt().toLong(), 111)
        Assert.assertArrayEquals(objects[1] as Array<Any>, strings)
        Assert.assertEquals((objects[2] as Number).toInt().toLong(), 222)
    }

    @Test
    fun decodeWithUnknownFunctionTypeTest() {
        var funcJson = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "   'outputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'string[]'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'type':'test' \n" +
                "}\n"
        funcJson = funcJson.replace("'".toRegex(), "\"")
        val function = CallTransaction.Function.fromJsonInterface(funcJson)
        Assert.assertEquals(null, function.type)
        val strings = arrayOf("aaa", "long string: 123456789012345678901234567890", "ccc")
        val encoded = function.encodeArguments(111, strings, 222)
        val objects = function.decodeResult(encoded)
        Assert.assertEquals((objects[0] as Number).toInt().toLong(), 111)
        Assert.assertArrayEquals(objects[1] as Array<Any>, strings)
        Assert.assertEquals((objects[2] as Number).toInt().toLong(), 222)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")
        private var funcJson1 = "{ \n" +
                "  'constant': false, \n" +
                "  'inputs': [{'name':'to', 'type':'address'}], \n" +
                "  'name': 'delegate', \n" +
                "  'outputs': [], \n" +
                "  'type': 'function' \n" +
                "} \n"
        private var funcJson2 = "{\n" +
                " 'constant':false, \n" +
                " 'inputs':[], \n" +
                " 'name':'tst', \n" +
                " 'outputs':[], \n" +
                " 'type':'function' \n" +
                "}"
        private var funcJson3 = "{\n" +
                " 'constant':false, \n" +
                " 'inputs':[ \n" +
                "   {'name':'i','type':'int'}, \n" +
                "   {'name':'u','type':'uint'}, \n" +
                "   {'name':'i8','type':'int8'}, \n" +
                "   {'name':'b2','type':'bytes2'}, \n" +
                "   {'name':'b32','type':'bytes32'} \n" +
                "  ], \n" +
                "  'name':'f1', \n" +
                "  'outputs':[], \n" +
                "  'type':'function' \n" +
                "}\n"
        private var funcJson4 = "{\n" +
                " 'constant':false, \n" +
                " 'inputs':[{'name':'i','type':'int[3]'}, {'name':'j','type':'int[]'}], \n" +
                " 'name':'f2', \n" +
                " 'outputs':[], \n" +
                " 'type':'function' \n" +
                "}\n"
        private var funcJson5 = "{\n" +
                "   'constant':false, \n" +
                "   'inputs':[{'name':'i','type':'int'}, \n" +
                "               {'name':'s','type':'bytes'}, \n" +
                "               {'name':'j','type':'int'}], \n" +
                "    'name':'f4', \n" +
                "    'outputs':[], \n" +
                "    'type':'function' \n" +
                "}\n"

        init {
            funcJson1 = funcJson1.replace("'".toRegex(), "\"")
        }

        init {
            funcJson2 = funcJson2.replace("'".toRegex(), "\"")
        }

        init {
            funcJson3 = funcJson3.replace("'".toRegex(), "\"")
        }

        init {
            funcJson4 = funcJson4.replace("'".toRegex(), "\"")
        }

        init {
            funcJson5 = funcJson5.replace("'".toRegex(), "\"")
        }
    }
}
