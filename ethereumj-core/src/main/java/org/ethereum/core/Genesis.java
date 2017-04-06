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

package org.ethereum.core;

import org.ethereum.config.SystemProperties;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.util.ByteUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * The genesis block is the first block in the chain and has fixed values according to
 * the protocol specification. The genesis block is 13 items, and is specified thus:
 * <p>
 * ( zerohash_256 , SHA3 RLP () , zerohash_160 , stateRoot, 0, 2^22 , 0, 0, 1000000, 0, 0, 0, SHA3 (42) , (), () )
 * <p>
 * - Where zerohash_256 refers to the parent hash, a 256-bit hash which is all zeroes;
 * - zerohash_160 refers to the coinbase address, a 160-bit hash which is all zeroes;
 * - 2^22 refers to the difficulty;
 * - 0 refers to the timestamp (the Unix epoch);
 * - the transaction trie root and extradata are both 0, being equivalent to the empty byte array.
 * - The sequences of both uncles and transactions are empty and represented by ().
 * - SHA3 (42) refers to the SHA3 hash of a byte array of length one whose first and only byte is of value 42.
 * - SHA3 RLP () value refers to the hash of the uncle lists in RLP, both empty lists.
 * <p>
 * See Yellow Paper: http://www.gavwood.com/Paper.pdf (Appendix I. Genesis Block)
 */
public class Genesis extends Block {

    public static final byte[] ZERO_HASH_2048 = new byte[256];
    public static final long NUMBER = 0;
    public static byte[] DIFFICULTY = BigInteger.valueOf(2).pow(17).toByteArray();
    private static Block instance;
    private Map<ByteArrayWrapper, PremineAccount> premine = new HashMap<>();

    public Genesis(final byte[] parentHash, final byte[] unclesHash, final byte[] coinbase, final byte[] logsBloom,
                   final byte[] difficulty, final long number, final long gasLimit,
                   final long gasUsed, final long timestamp,
                   final byte[] extraData, final byte[] mixHash, final byte[] nonce) {
        super(parentHash, unclesHash, coinbase, logsBloom, difficulty,
                number, ByteUtil.longToBytesNoLeadZeroes(gasLimit), gasUsed, timestamp, extraData,
                mixHash, nonce, null, null);
    }

    public static Block getInstance() {
        return SystemProperties.getDefault().getGenesis();
    }

    public static Genesis getInstance(final SystemProperties config) {
        return config.getGenesis();
    }

    public static void populateRepository(final Repository repository, final Genesis genesis) {
        for (final ByteArrayWrapper key : genesis.getPremine().keySet()) {
            final Genesis.PremineAccount premineAccount = genesis.getPremine().get(key);
            final AccountState accountState = premineAccount.accountState;

            repository.createAccount(key.getData());
            repository.setNonce(key.getData(), accountState.getNonce());
            repository.addBalance(key.getData(), accountState.getBalance());
            if (premineAccount.code != null) {
                repository.saveCode(key.getData(), premineAccount.code);
            }
        }
    }

    public Map<ByteArrayWrapper, PremineAccount> getPremine() {
        return premine;
    }

    public void setPremine(final Map<ByteArrayWrapper, PremineAccount> premine) {
        this.premine = premine;
    }

    public void addPremine(final ByteArrayWrapper address, final AccountState accountState) {
        premine.put(address, new PremineAccount(accountState));
    }

    /**
     * Used to keep addition fields.
     */
    public static class PremineAccount {

        public byte[] code;

        public AccountState accountState;

        public PremineAccount(final AccountState accountState) {
            this.accountState = accountState;
        }

        public PremineAccount() {
        }

        public byte[] getStateRoot() {
            return accountState.getStateRoot();
        }
    }
}
