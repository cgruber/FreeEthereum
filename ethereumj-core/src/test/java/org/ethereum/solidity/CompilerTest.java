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

package org.ethereum.solidity;

import org.ethereum.core.CallTransaction;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.ethereum.solidity.compiler.SolidityCompiler.Options.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Created by Anton Nashatyrev on 03.03.2016.
 */
public class CompilerTest {

    public static void main(final String[] args) throws Exception {
        new CompilerTest().simpleTest();
    }

    @Test
    public void solc_getVersion_shouldWork() throws IOException {
        final String version = SolidityCompiler.runGetVersionOutput();

        // ##### May produce 2 lines:
        //solc, the solidity compiler commandline interface
        //Version: 0.4.7+commit.822622cf.mod.Darwin.appleclang
        System.out.println(version);

        assertThat(version, containsString("Version:"));
    }

    @Test
    public void simpleTest() throws IOException {
        final String contract =
            "pragma solidity ^0.4.7;\n" +
                    "\n" +
                    "contract a {\n" +
                    "\n" +
                    "        mapping(address => string) private mailbox;\n" +
                    "\n" +
                    "        event Mailed(address from, string message);\n" +
                    "        event Read(address from, string message);\n" +
                    "\n" +
                    "}";

        final SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");
        final CompilationResult result = CompilationResult.parse(res.output);
        if (result.contracts.get("a") != null)
            System.out.println(result.contracts.get("a").bin);
        else
            Assert.fail();
    }

    @Test
    public void defaultFuncTest() throws IOException {
        final String contractSrc =
            "pragma solidity ^0.4.7;\n" +
                    "contract a {" +
                    "        function() {throw;}" +
                    "}";

        final SolidityCompiler.Result res = SolidityCompiler.compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");
        final CompilationResult result = CompilationResult.parse(res.output);

        final CompilationResult.ContractMetadata a = result.contracts.get("a");
        final CallTransaction.Contract contract = new CallTransaction.Contract(a.abi);
        System.out.printf(contract.functions[0].toString());
    }

    @Test
    public void compileFilesTest() throws IOException {

        final File source = new File("src/test/resources/solidity/file1.sol");

        final SolidityCompiler.Result res = SolidityCompiler.compile(
                source, true, ABI, BIN, INTERFACE, METADATA);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");
        final CompilationResult result = CompilationResult.parse(res.output);

        final CompilationResult.ContractMetadata a = result.contracts.get("test1");
        final CallTransaction.Contract contract = new CallTransaction.Contract(a.abi);
        System.out.printf(contract.functions[0].toString());
    }
}
