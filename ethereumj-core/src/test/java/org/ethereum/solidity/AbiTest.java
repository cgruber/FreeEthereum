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

import org.ethereum.solidity.Abi.Entry;
import org.ethereum.solidity.Abi.Entry.Type;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbiTest {

    @Test
    public void simpleTest() throws IOException {
        final String contractAbi = "[{"
                + "\"name\":\"simpleFunction\","
                + "\"constant\":true,"
                + "\"payable\":true,"
                + "\"type\":\"function\","
                + "\"inputs\": [{\"name\":\"_in\", \"type\":\"bytes32\"}],"
                + "\"outputs\":[{\"name\":\"_out\",\"type\":\"bytes32\"}]}]";

        final Abi abi = Abi.fromJson(contractAbi);
        assertEquals(abi.size(), 1);

        final Entry onlyFunc = abi.get(0);
        assertEquals(onlyFunc.type, Type.function);
        assertEquals(onlyFunc.inputs.size(), 1);
        assertEquals(onlyFunc.outputs.size(), 1);
        assertTrue(onlyFunc.payable);
        assertTrue(onlyFunc.constant);
    }

    @Test
    public void simpleLegacyTest() throws IOException {
        final String contractAbi = "[{"
                + "\"name\":\"simpleFunction\","
                + "\"constant\":true,"
                + "\"type\":\"function\","
                + "\"inputs\": [{\"name\":\"_in\", \"type\":\"bytes32\"}],"
                + "\"outputs\":[{\"name\":\"_out\",\"type\":\"bytes32\"}]}]";

        final Abi abi = Abi.fromJson(contractAbi);
        assertEquals(abi.size(), 1);

        final Entry onlyFunc = abi.get(0);
        assertEquals(onlyFunc.type, Type.function);
        assertEquals(onlyFunc.inputs.size(), 1);
        assertEquals(onlyFunc.outputs.size(), 1);
        assertTrue(onlyFunc.constant);
    }
}
