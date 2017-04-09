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

package org.ethereum.jsontestsuite.suite;

import org.ethereum.core.Block;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.db.BlockStore;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.program.Program;
import org.ethereum.vm.program.invoke.ProgramInvoke;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeImpl;

import java.math.BigInteger;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public class TestProgramInvokeFactory implements ProgramInvokeFactory {

    private final Env env;

    public TestProgramInvokeFactory(final Env env) {
        this.env = env;
    }


    @Override
    public ProgramInvoke createProgramInvoke(final Transaction tx, final Block block, final Repository repository, final BlockStore blockStore) {
        return generalInvoke(tx, repository, blockStore);
    }

    @Override
    public ProgramInvoke createProgramInvoke(final Program program, final DataWord toAddress, final DataWord callerAddress,
                                             final DataWord inValue, final DataWord inGas,
                                             final BigInteger balanceInt, final byte[] dataIn,
                                             final Repository repository, final BlockStore blockStore, final boolean byTestingSuite) {
        return null;
    }


    private ProgramInvoke generalInvoke(final Transaction tx, final Repository repository, final BlockStore blockStore) {

        /***         ADDRESS op       ***/
        // YP: Get address of currently executing account.
        final byte[] address = tx.isContractCreation() ? tx.getContractAddress() : tx.getReceiveAddress();

        /***         ORIGIN op       ***/
        // YP: This is the sender of original transaction; it is never a contract.
        final byte[] origin = tx.getSender();

        /***         CALLER op       ***/
        // YP: This is the address of the account that is directly responsible for this execution.
        final byte[] caller = tx.getSender();

        /***         BALANCE op       ***/
        final byte[] balance = repository.getBalance(address).toByteArray();

        /***         GASPRICE op       ***/
        final byte[] gasPrice = tx.getGasPrice();

        /*** GAS op ***/
        final byte[] gas = tx.getGasLimit();

        /***        CALLVALUE op      ***/
        final byte[] callValue = tx.getValue() == null ? new byte[]{0} : tx.getValue();

        /***     CALLDATALOAD  op   ***/
        /***     CALLDATACOPY  op   ***/
        /***     CALLDATASIZE  op   ***/
        final byte[] data = tx.isContractCreation() ? ByteUtil.EMPTY_BYTE_ARRAY : (tx.getData() == null ? ByteUtil.EMPTY_BYTE_ARRAY : tx.getData());
//        byte[] data =  tx.getData() == null ? ByteUtil.EMPTY_BYTE_ARRAY : tx.getData() ;

        /***    PREVHASH  op  ***/
        final byte[] lastHash = env.getPreviousHash();

        /***   COINBASE  op ***/
        final byte[] coinbase = env.getCurrentCoinbase();

        /*** TIMESTAMP  op  ***/
        final long timestamp = ByteUtil.byteArrayToLong(env.getCurrentTimestamp());

        /*** NUMBER  op  ***/
        final long number = ByteUtil.byteArrayToLong(env.getCurrentNumber());

        /*** DIFFICULTY  op  ***/
        final byte[] difficulty = env.getCurrentDifficulty();

        /*** GASLIMIT op ***/
        final byte[] gaslimit = env.getCurrentGasLimit();

        return new ProgramInvokeImpl(address, origin, caller, balance,
                gasPrice, gas, callValue, data, lastHash, coinbase,
                timestamp, number, difficulty, gaslimit, repository, blockStore);
    }

}
