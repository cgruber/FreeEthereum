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

import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;

/**
 * @author Roman Mandeleil
 * @since 09.01.2015
 */
public class PrecompiledContracts {

    private static final ECRecover ecRecover = new ECRecover();
    private static final Sha256 sha256 = new Sha256();
    private static final Ripempd160 ripempd160 = new Ripempd160();
    private static final Identity identity = new Identity();

    private static final DataWord ecRecoverAddr =   new DataWord("0000000000000000000000000000000000000000000000000000000000000001");
    private static final DataWord sha256Addr =      new DataWord("0000000000000000000000000000000000000000000000000000000000000002");
    private static final DataWord ripempd160Addr =  new DataWord("0000000000000000000000000000000000000000000000000000000000000003");
    private static final DataWord identityAddr =    new DataWord("0000000000000000000000000000000000000000000000000000000000000004");


    public static PrecompiledContract getContractForAddress(final DataWord address) {

        if (address == null) return identity;
        if (address.equals(ecRecoverAddr)) return ecRecover;
        if (address.equals(sha256Addr)) return sha256;
        if (address.equals(ripempd160Addr)) return ripempd160;
        if (address.equals(identityAddr)) return identity;

        return null;
    }


    public static abstract class PrecompiledContract {
        public abstract long getGasForData(byte[] data);

        public abstract byte[] execute(byte[] data);
    }

    public static class Identity extends PrecompiledContract {

        public Identity() {
        }

        @Override
        public long getGasForData(final byte[] data) {

            // gas charge for the execution:
            // minimum 1 and additional 1 for each 32 bytes word (round  up)
            if (data == null) return 15;
            return 15 + (data.length + 31) / 32 * 3;
        }

        @Override
        public byte[] execute(final byte[] data) {
            return data;
        }
    }

    public static class Sha256 extends PrecompiledContract {


        @Override
        public long getGasForData(final byte[] data) {

            // gas charge for the execution:
            // minimum 50 and additional 50 for each 32 bytes word (round  up)
            if (data == null) return 60;
            return 60 + (data.length + 31) / 32 * 12;
        }

        @Override
        public byte[] execute(final byte[] data) {

            if (data == null) return HashUtil.INSTANCE.sha256(ByteUtil.EMPTY_BYTE_ARRAY);
            return HashUtil.INSTANCE.sha256(data);
        }
    }


    public static class Ripempd160 extends PrecompiledContract {


        @Override
        public long getGasForData(final byte[] data) {

            // TODO #POC9 Replace magic numbers with constants
            // gas charge for the execution:
            // minimum 50 and additional 50 for each 32 bytes word (round  up)
            if (data == null) return 600;
            return 600 + (data.length + 31) / 32 * 120;
        }

        @Override
        public byte[] execute(final byte[] data) {

            byte[] result = null;
            if (data == null) result = HashUtil.INSTANCE.ripemd160(ByteUtil.EMPTY_BYTE_ARRAY);
            else result = HashUtil.INSTANCE.ripemd160(data);

            return new DataWord(result).getData();
        }
    }


    public static class ECRecover extends PrecompiledContract {

        private static boolean validateV(final byte[] v) {
            for (int i = 0; i < v.length - 1; i++) {
                if (v[i] != 0) return false;
            }
            return true;
        }

        @Override
        public long getGasForData(final byte[] data) {
            return 3000;
        }

        @Override
        public byte[] execute(final byte[] data) {

            final byte[] h = new byte[32];
            final byte[] v = new byte[32];
            final byte[] r = new byte[32];
            final byte[] s = new byte[32];

            DataWord out = null;

            try {
                System.arraycopy(data, 0, h, 0, 32);
                System.arraycopy(data, 32, v, 0, 32);
                System.arraycopy(data, 64, r, 0, 32);

                final int sLength = data.length < 128 ? data.length - 96 : 32;
                System.arraycopy(data, 96, s, 0, sLength);

                final ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v[31]);
                if (validateV(v) && signature.validateComponents()) {
                    out = new DataWord(ECKey.signatureToAddress(h, signature));
                }
            } catch (final Throwable ignored) {
            }

            if (out == null) {
                return new byte[0];
            } else {
                return out.getData();
            }
        }
    }


}
