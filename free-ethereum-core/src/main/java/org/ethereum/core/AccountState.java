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
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import static org.ethereum.util.FastByteComparisons.equal;

public class AccountState {

    /* A value equal to the number of transactions sent
     * from this address, or, in the case of contract accounts,
     * the number of contract-creations made by this account */
    private final BigInteger nonce;
    /* A scalar value equal to the number of Wei owned by this address */
    private final BigInteger balance;
    /* A 256-bit hash of the root node of a trie structure
     * that encodes the storage contents of the contract,
     * itself a simple mapping between byte arrays of size 32.
     * The hash is formally denoted σ[a] s .
     *
     * Since I typically wish to refer not to the trie’s root hash
     * but to the underlying set of key/value pairs stored within,
     * I define a convenient equivalence TRIE (σ[a] s ) ≡ σ[a] s .
     * It shall be understood that σ[a] s is not a ‘physical’ member
     * of the account and does not contribute to its later serialisation */
    private final byte[] stateRoot;
    /* The hash of the EVM code of this contract—this is the code
     * that gets executed should this address receive a message call;
     * it is immutable and thus, unlike all other fields, cannot be changed
     * after construction. All such code fragments are contained in
     * the state database under their corresponding hashes for later
     * retrieval */
    private final byte[] codeHash;
    private byte[] rlpEncoded;

    public AccountState(final SystemProperties config) {
        this(config.getBlockchainConfig().getCommonConstants().getInitialNonce(), BigInteger.ZERO);
    }

    public AccountState(final BigInteger nonce, final BigInteger balance) {
        this(nonce, balance, HashUtil.INSTANCE.getEMPTY_TRIE_HASH(), HashUtil.INSTANCE.getEMPTY_DATA_HASH());
    }

    private AccountState(final BigInteger nonce, final BigInteger balance, final byte[] stateRoot, final byte[] codeHash) {
        this.nonce = nonce;
        this.balance = balance;
        this.stateRoot = stateRoot == HashUtil.INSTANCE.getEMPTY_TRIE_HASH() || equal(stateRoot, HashUtil.INSTANCE.getEMPTY_TRIE_HASH()) ? HashUtil.INSTANCE.getEMPTY_TRIE_HASH() : stateRoot;
        this.codeHash = codeHash == HashUtil.INSTANCE.getEMPTY_DATA_HASH() || equal(codeHash, HashUtil.INSTANCE.getEMPTY_DATA_HASH()) ? HashUtil.INSTANCE.getEMPTY_DATA_HASH() : codeHash;
    }

    public AccountState(final byte[] rlpData) {
        this.rlpEncoded = rlpData;

        final RLPList items = (RLPList) RLP.decode2(rlpEncoded).get(0);
        this.nonce = items.get(0).getRLPData() == null ? BigInteger.ZERO
                : new BigInteger(1, items.get(0).getRLPData());
        this.balance = items.get(1).getRLPData() == null ? BigInteger.ZERO
                : new BigInteger(1, items.get(1).getRLPData());
        this.stateRoot = items.get(2).getRLPData();
        this.codeHash = items.get(3).getRLPData();
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public AccountState withNonce(final BigInteger nonce) {
        return new AccountState(nonce, balance, stateRoot, codeHash);
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public AccountState withStateRoot(final byte[] stateRoot) {
        return new AccountState(nonce, balance, stateRoot, codeHash);
    }

    public AccountState withIncrementedNonce() {
        return new AccountState(nonce.add(BigInteger.ONE), balance, stateRoot, codeHash);
    }

    public byte[] getCodeHash() {
        return codeHash;
    }

    public AccountState withCodeHash(final byte[] codeHash) {
        return new AccountState(nonce, balance, stateRoot, codeHash);
    }

    public BigInteger getBalance() {
        return balance;
    }

    public AccountState withBalanceIncrement(final BigInteger value) {
        return new AccountState(nonce, balance.add(value), stateRoot, codeHash);
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            final byte[] nonce = RLP.encodeBigInteger(this.nonce);
            final byte[] balance = RLP.encodeBigInteger(this.balance);
            final byte[] stateRoot = RLP.encodeElement(this.stateRoot);
            final byte[] codeHash = RLP.encodeElement(this.codeHash);
            this.rlpEncoded = RLP.encodeList(nonce, balance, stateRoot, codeHash);
        }
        return rlpEncoded;
    }

    public boolean isEmpty() {
        return FastByteComparisons.equal(codeHash, HashUtil.INSTANCE.getEMPTY_DATA_HASH()) &&
                BigInteger.ZERO.equals(balance) &&
                BigInteger.ZERO.equals(nonce);
    }


    public String toString() {
        final String ret = "  Nonce: " + this.getNonce().toString() + "\n" +
                "  Balance: " + getBalance() + "\n" +
                "  State Root: " + Hex.toHexString(this.getStateRoot()) + "\n" +
                "  Code Hash: " + Hex.toHexString(this.getCodeHash());
        return ret;
    }
}
