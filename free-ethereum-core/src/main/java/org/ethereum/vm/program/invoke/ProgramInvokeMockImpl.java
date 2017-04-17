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

package org.ethereum.vm.program.invoke;

import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.BlockStore;
import org.ethereum.db.BlockStoreDummy;
import org.ethereum.db.RepositoryRoot;
import org.ethereum.vm.DataWord;
import org.spongycastle.util.encoders.Hex;

/**
 * @author Roman Mandeleil
 * @since 03.06.2014
 */
public class ProgramInvokeMockImpl implements ProgramInvoke {

    private byte[] msgData;
    private Repository repository;
    private byte[] ownerAddress = Hex.decode("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
    // default for most tests. This can be overwritten by the test
    private long gasLimit = 1000000;

    public ProgramInvokeMockImpl(final byte[] msgDataRaw) {
        this();
        this.msgData = msgDataRaw;
    }

    public ProgramInvokeMockImpl() {


        this.repository = new RepositoryRoot(new HashMapDB<>());
        this.repository.createAccount(ownerAddress);

        final byte[] contractAddress = Hex.decode("471fd3ad3e9eeadeec4608b92d16ce6b500704cc");
        this.repository.createAccount(contractAddress);
        this.repository.saveCode(contractAddress,
                Hex.decode("385E60076000396000605f556014600054601e60"
                        + "205463abcddcba6040545b51602001600a525451"
                        + "6040016014525451606001601e52545160800160"
                        + "28525460a052546016604860003960166000f260"
                        + "00603f556103e75660005460005360200235"));
    }

    public ProgramInvokeMockImpl(final boolean defaults) {


    }

    /*           ADDRESS op         */
    public DataWord getOwnerAddress() {
        return new DataWord(ownerAddress);
    }

    public void setOwnerAddress(final byte[] ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    /*           BALANCE op         */
    public DataWord getBalance() {
        final byte[] balance = Hex.decode("0DE0B6B3A7640000");
        return new DataWord(balance);
    }

    /*           ORIGIN op         */
    public DataWord getOriginAddress() {

        final byte[] cowPrivKey = HashUtil.INSTANCE.sha3("horse".getBytes());
        final byte[] addr = ECKey.fromPrivate(cowPrivKey).getAddress();

        return new DataWord(addr);
    }

    /*           CALLER op         */
    public DataWord getCallerAddress() {

        final byte[] cowPrivKey = HashUtil.INSTANCE.sha3("monkey".getBytes());
        final byte[] addr = ECKey.fromPrivate(cowPrivKey).getAddress();

        return new DataWord(addr);
    }

    /*           GASPRICE op       */
    public DataWord getMinGasPrice() {

        final byte[] minGasPrice = Hex.decode("09184e72a000");
        return new DataWord(minGasPrice);
    }

    /*           GAS op       */
    public DataWord getGas() {

        return new DataWord(gasLimit);
    }

    public void setGas(final long gasLimit) {
        this.gasLimit = gasLimit;
    }

    @Override
    public long getGasLong() {
        return gasLimit;
    }

    /*****************/
    /***  msg data ***/

    /*          CALLVALUE op    */
    public DataWord getCallValue() {
        final byte[] balance = Hex.decode("0DE0B6B3A7640000");
        return new DataWord(balance);
    }

    /**
     * *************
     */

    /*     CALLDATALOAD  op   */
    public DataWord getDataValue(final DataWord indexData) {

        final byte[] data = new byte[32];

        final int index = indexData.value().intValue();
        int size = 32;

        if (msgData == null) return new DataWord(data);
        if (index > msgData.length) return new DataWord(data);
        if (index + 32 > msgData.length) size = msgData.length - index;

        System.arraycopy(msgData, index, data, 0, size);

        return new DataWord(data);
    }

    /*  CALLDATASIZE */
    public DataWord getDataSize() {

        if (msgData == null || msgData.length == 0) return new DataWord(new byte[32]);
        final int size = msgData.length;
        return new DataWord(size);
    }

    /*  CALLDATACOPY */
    public byte[] getDataCopy(final DataWord offsetData, final DataWord lengthData) {

        final int offset = offsetData.value().intValue();
        int length = lengthData.value().intValue();

        final byte[] data = new byte[length];

        if (msgData == null) return data;
        if (offset > msgData.length) return data;
        if (offset + length > msgData.length) length = msgData.length - offset;

        System.arraycopy(msgData, offset, data, 0, length);

        return data;
    }

    @Override
    public DataWord getPrevHash() {
        final byte[] prevHash = Hex.decode("961CB117ABA86D1E596854015A1483323F18883C2D745B0BC03E87F146D2BB1C");
        return new DataWord(prevHash);
    }

    @Override
    public DataWord getCoinbase() {
        final byte[] coinBase = Hex.decode("E559DE5527492BCB42EC68D07DF0742A98EC3F1E");
        return new DataWord(coinBase);
    }

    @Override
    public DataWord getTimestamp() {
        final long timestamp = 1401421348;
        return new DataWord(timestamp);
    }

    @Override
    public DataWord getNumber() {
        final long number = 33;
        return new DataWord(number);
    }

    @Override
    public DataWord getDifficulty() {
        final byte[] difficulty = Hex.decode("3ED290");
        return new DataWord(difficulty);
    }

    @Override
    public DataWord getGaslimit() {
        return new DataWord(gasLimit);
    }

    public void setGasLimit(final long gasLimit) {
        this.gasLimit = gasLimit;
    }

    @Override
    public boolean byTransaction() {
        return true;
    }

    @Override
    public boolean byTestingSuite() {
        return false;
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    @Override
    public BlockStore getBlockStore() {
        return new BlockStoreDummy();
    }

    @Override
    public int getCallDeep() {
        return 0;
    }
}
