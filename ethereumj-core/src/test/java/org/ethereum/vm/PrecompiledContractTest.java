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

package org.ethereum.vm;

import org.ethereum.util.ByteUtil;
import org.ethereum.vm.PrecompiledContracts.PrecompiledContract;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Roman Mandeleil
 */
public class PrecompiledContractTest {


    @Test
    public void identityTest1() {

        final DataWord addr = new DataWord("0000000000000000000000000000000000000000000000000000000000000004");
        final PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
        final byte[] data = Hex.decode("112233445566");
        final byte[] expected = Hex.decode("112233445566");

        final byte[] result = contract.execute(data);

        assertArrayEquals(expected, result);
    }


    @Test
    public void sha256Test1() {

        final DataWord addr = new DataWord("0000000000000000000000000000000000000000000000000000000000000002");
        final PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
        final byte[] data = null;
        final String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        final byte[] result = contract.execute(data);

        assertEquals(expected, Hex.toHexString(result));
    }

    @Test
    public void sha256Test2() {

        final DataWord addr = new DataWord("0000000000000000000000000000000000000000000000000000000000000002");
        final PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
        final byte[] data = ByteUtil.EMPTY_BYTE_ARRAY;
        final String expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        final byte[] result = contract.execute(data);

        assertEquals(expected, Hex.toHexString(result));
    }

    @Test
    public void sha256Test3() {

        final DataWord addr = new DataWord("0000000000000000000000000000000000000000000000000000000000000002");
        final PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
        final byte[] data = Hex.decode("112233");
        final String expected = "49ee2bf93aac3b1fb4117e59095e07abe555c3383b38d608da37680a406096e8";

        final byte[] result = contract.execute(data);

        assertEquals(expected, Hex.toHexString(result));
    }


    @Test
    public void Ripempd160Test1() {

        final DataWord addr = new DataWord("0000000000000000000000000000000000000000000000000000000000000003");
        final PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
        final byte[] data = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001");
        final String expected = "000000000000000000000000ae387fcfeb723c3f5964509af111cf5a67f30661";

        final byte[] result = contract.execute(data);

        assertEquals(expected, Hex.toHexString(result));
    }

    @Test
    public void ecRecoverTest1() {

        final byte[] data = Hex.decode("18c547e4f7b0f325ad1e56f57e26c745b09a3e503d86e00e5255ff7f715d3d1c000000000000000000000000000000000000000000000000000000000000001c73b1693892219d736caba55bdb67216e485557ea6b6af75f37096c9aa6a5a75feeb940b1d03b21e36b0e47e79769f095fe2ab855bd91e3a38756b7d75a9c4549");
        final DataWord addr = new DataWord("0000000000000000000000000000000000000000000000000000000000000001");
        final PrecompiledContract contract = PrecompiledContracts.getContractForAddress(addr);
        final String expected = "000000000000000000000000ae387fcfeb723c3f5964509af111cf5a67f30661";

        final byte[] result = contract.execute(data);

        System.out.println(Hex.toHexString(result));


    }

}
