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

package org.ethereum.solidity

import org.ethereum.core.CallTransaction
import org.ethereum.solidity.compiler.CompilationResult
import org.ethereum.solidity.compiler.SolidityCompiler
import org.ethereum.solidity.compiler.SolidityCompiler.Options.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringContains.containsString
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Created by Anton Nashatyrev on 03.03.2016.
 */
class CompilerTest {

    @Test
    @Throws(IOException::class)
    fun solc_getVersion_shouldWork() {
        val version = SolidityCompiler.runGetVersionOutput()

        // ##### May produce 2 lines:
        //solc, the solidity compiler commandline interface
        //Version: 0.4.7+commit.822622cf.mod.Darwin.appleclang
        println(version)

        assertThat(version, containsString("Version:"))
    }

    @Test
    @Throws(IOException::class)
    fun simpleTest() {
        val contract = "pragma solidity ^0.4.7;\n" +
                "\n" +
                "contract a {\n" +
                "\n" +
                "        mapping(address => string) private mailbox;\n" +
                "\n" +
                "        event Mailed(address from, string message);\n" +
                "        event Read(address from, string message);\n" +
                "\n" +
                "}"

        val res = SolidityCompiler.compile(
                contract.toByteArray(), true, ABI, BIN, INTERFACE, METADATA)
        println("Out: '" + res.output + "'")
        println("Err: '" + res.errors + "'")
        val result = CompilationResult.parse(res.output)
        if (result.contracts["a"] != null)
            println(result.contracts["a"]?.bin)
        else
            Assert.fail()
    }

    @Test
    @Throws(IOException::class)
    fun defaultFuncTest() {
        val contractSrc = "pragma solidity ^0.4.7;\n" +
                "contract a {" +
                "        function() {throw;}" +
                "}"

        val res = SolidityCompiler.compile(
                contractSrc.toByteArray(), true, ABI, BIN, INTERFACE, METADATA)
        println("Out: '" + res.output + "'")
        println("Err: '" + res.errors + "'")
        val result = CompilationResult.parse(res.output)

        val a = result.contracts["a"]
        val contract = CallTransaction.Contract(a?.abi)
        System.out.printf(contract.functions[0].toString())
    }

    @Test
    @Throws(IOException::class)
    fun compileFilesTest() {

        val source = File("src/test/resources/solidity/file1.sol")

        val res = SolidityCompiler.compile(
                source, true, ABI, BIN, INTERFACE, METADATA)
        println("Out: '" + res.output + "'")
        println("Err: '" + res.errors + "'")
        val result = CompilationResult.parse(res.output)

        val a = result.contracts["test1"]
        val contract = CallTransaction.Contract(a?.abi)
        System.out.printf(contract.functions[0].toString())
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            CompilerTest().simpleTest()
        }
    }
}
