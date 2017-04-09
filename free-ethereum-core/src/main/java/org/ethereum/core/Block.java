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

import org.ethereum.crypto.HashUtil;
import org.ethereum.trie.Trie;
import org.ethereum.trie.TrieImpl;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.ethereum.crypto.HashUtil.sha3;

/**
 * The block in Ethereum is the collection of relevant pieces of information
 * (known as the blockheader), H, together with information corresponding to
 * the comprised transactions, R, and a set of other blockheaders U that are known
 * to have a parent equal to the present block’s parent’s parent
 * (such blocks are known as uncles).
 *
 * @author Roman Mandeleil
 * @author Nick Savers
 * @since 20.05.2014
 */
public class Block {

    private static final Logger logger = LoggerFactory.getLogger("blockchain");
    private final StringBuffer toStringBuff = new StringBuffer();
    private BlockHeader header;
    /* Transactions */
    private List<Transaction> transactionsList = new CopyOnWriteArrayList<>();

    /* Private */
    /* Uncles */
    private List<BlockHeader> uncleList = new CopyOnWriteArrayList<>();
    private byte[] rlpEncoded;

    /* Constructors */
    private boolean parsed = false;

    private Block() {
    }

    public Block(final byte[] rawData) {
        logger.debug("new from [" + Hex.toHexString(rawData) + "]");
        this.rlpEncoded = rawData;
    }

    public Block(final BlockHeader header, final List<Transaction> transactionsList, final List<BlockHeader> uncleList) {

        this(header.getParentHash(),
                header.getUnclesHash(),
                header.getCoinbase(),
                header.getLogsBloom(),
                header.getDifficulty(),
                header.getNumber(),
                header.getGasLimit(),
                header.getGasUsed(),
                header.getTimestamp(),
                header.getExtraData(),
                header.getMixHash(),
                header.getNonce(),
                header.getReceiptsRoot(),
                header.getTxTrieRoot(),
                header.getStateRoot(),
                transactionsList,
                uncleList);
    }


    public Block(final byte[] parentHash, final byte[] unclesHash, final byte[] coinbase, final byte[] logsBloom,
                 final byte[] difficulty, final long number, final byte[] gasLimit,
                 final long gasUsed, final long timestamp, final byte[] extraData,
                 final byte[] mixHash, final byte[] nonce, final byte[] receiptsRoot,
                 final byte[] transactionsRoot, final byte[] stateRoot,
                 final List<Transaction> transactionsList, final List<BlockHeader> uncleList) {

        this(parentHash, unclesHash, coinbase, logsBloom, difficulty, number, gasLimit,
                gasUsed, timestamp, extraData, mixHash, nonce, transactionsList, uncleList);

        this.header.setTransactionsRoot(BlockchainImpl.calcTxTrie(transactionsList));
        if (!Hex.toHexString(transactionsRoot).
                equals(Hex.toHexString(this.header.getTxTrieRoot())))
            logger.debug("Transaction root miss-calculate, block: {}", getNumber());

        this.header.setStateRoot(stateRoot);
        this.header.setReceiptsRoot(receiptsRoot);
    }

    public Block(final byte[] parentHash, final byte[] unclesHash, final byte[] coinbase, final byte[] logsBloom,
                 final byte[] difficulty, final long number, final byte[] gasLimit,
                 final long gasUsed, final long timestamp,
                 final byte[] extraData, final byte[] mixHash, final byte[] nonce,
                 final List<Transaction> transactionsList, final List<BlockHeader> uncleList) {
        this.header = new BlockHeader(parentHash, unclesHash, coinbase, logsBloom,
                difficulty, number, gasLimit, gasUsed,
                timestamp, extraData, mixHash, nonce);

        this.transactionsList = transactionsList;
        if (this.transactionsList == null) {
            this.transactionsList = new CopyOnWriteArrayList<>();
        }

        this.uncleList = uncleList;
        if (this.uncleList == null) {
            this.uncleList = new CopyOnWriteArrayList<>();
        }

        this.parsed = true;
    }

    private synchronized void parseRLP() {
        if (parsed) return;

        final RLPList params = RLP.decode2(rlpEncoded);
        final RLPList block = (RLPList) params.get(0);

        // Parse Header
        final RLPList header = (RLPList) block.get(0);
        this.header = new BlockHeader(header);

        // Parse Transactions
        final RLPList txTransactions = (RLPList) block.get(1);
        this.parseTxs(this.header.getTxTrieRoot(), txTransactions, false);

        // Parse Uncles
        final RLPList uncleBlocks = (RLPList) block.get(2);
        for (final RLPElement rawUncle : uncleBlocks) {

            final RLPList uncleHeader = (RLPList) rawUncle;
            final BlockHeader blockData = new BlockHeader(uncleHeader);
            this.uncleList.add(blockData);
        }
        this.parsed = true;
    }

    public BlockHeader getHeader() {
        parseRLP();
        return this.header;
    }

    public byte[] getHash() {
        parseRLP();
        return this.header.getHash();
    }

    public byte[] getParentHash() {
        parseRLP();
        return this.header.getParentHash();
    }

    public byte[] getUnclesHash() {
        parseRLP();
        return this.header.getUnclesHash();
    }

    public byte[] getCoinbase() {
        parseRLP();
        return this.header.getCoinbase();
    }

    public byte[] getStateRoot() {
        parseRLP();
        return this.header.getStateRoot();
    }

    public void setStateRoot(final byte[] stateRoot) {
        parseRLP();
        this.header.setStateRoot(stateRoot);
    }

    public byte[] getTxTrieRoot() {
        parseRLP();
        return this.header.getTxTrieRoot();
    }

    public byte[] getReceiptsRoot() {
        parseRLP();
        return this.header.getReceiptsRoot();
    }

    public byte[] getLogBloom() {
        parseRLP();
        return this.header.getLogsBloom();
    }

    public byte[] getDifficulty() {
        parseRLP();
        return this.header.getDifficulty();
    }

    public BigInteger getDifficultyBI() {
        parseRLP();
        return this.header.getDifficultyBI();
    }

    public BigInteger getCumulativeDifficulty() {
        parseRLP();
        BigInteger calcDifficulty = new BigInteger(1, this.header.getDifficulty());
        for (final BlockHeader uncle : uncleList) {
            calcDifficulty = calcDifficulty.add(new BigInteger(1, uncle.getDifficulty()));
        }
        return calcDifficulty;
    }

    public long getTimestamp() {
        parseRLP();
        return this.header.getTimestamp();
    }

    public long getNumber() {
        parseRLP();
        return this.header.getNumber();
    }

    public byte[] getGasLimit() {
        parseRLP();
        return this.header.getGasLimit();
    }

    public long getGasUsed() {
        parseRLP();
        return this.header.getGasUsed();
    }

    public byte[] getExtraData() {
        parseRLP();
        return this.header.getExtraData();
    }

    public void setExtraData(final byte[] data) {
        this.header.setExtraData(data);
        rlpEncoded = null;
    }

    public byte[] getMixHash() {
        parseRLP();
        return this.header.getMixHash();
    }

    public void setMixHash(final byte[] hash) {
        this.header.setMixHash(hash);
        rlpEncoded = null;
    }

    public byte[] getNonce() {
        parseRLP();
        return this.header.getNonce();
    }

    public void setNonce(final byte[] nonce) {
        this.header.setNonce(nonce);
        rlpEncoded = null;
    }

    public List<Transaction> getTransactionsList() {
        parseRLP();
        return transactionsList;
    }

    public List<BlockHeader> getUncleList() {
        parseRLP();
        return uncleList;
    }
    // [parent_hash, uncles_hash, coinbase, state_root, tx_trie_root,
    // difficulty, number, minGasPrice, gasLimit, gasUsed, timestamp,
    // extradata, nonce]

    @Override
    public String toString() {
        parseRLP();

        toStringBuff.setLength(0);
        toStringBuff.append(Hex.toHexString(this.getEncoded())).append("\n");
        toStringBuff.append("BlockData [ ");
        toStringBuff.append("hash=").append(ByteUtil.toHexString(this.getHash())).append("\n");
        toStringBuff.append(header.toString());

        if (!getUncleList().isEmpty()) {
            toStringBuff.append("Uncles [\n");
            for (final BlockHeader uncle : getUncleList()) {
                toStringBuff.append(uncle.toString());
                toStringBuff.append("\n");
            }
            toStringBuff.append("]\n");
        } else {
            toStringBuff.append("Uncles []\n");
        }
        if (!getTransactionsList().isEmpty()) {
            toStringBuff.append("Txs [\n");
            for (final Transaction tx : getTransactionsList()) {
                toStringBuff.append(tx);
                toStringBuff.append("\n");
            }
            toStringBuff.append("]\n");
        } else {
            toStringBuff.append("Txs []\n");
        }
        toStringBuff.append("]");

        return toStringBuff.toString();
    }

    public String toFlatString() {
        parseRLP();

        toStringBuff.setLength(0);
        toStringBuff.append("BlockData [");
        toStringBuff.append("hash=").append(ByteUtil.toHexString(this.getHash()));
        toStringBuff.append(header.toFlatString());

        for (final Transaction tx : getTransactionsList()) {
            toStringBuff.append("\n");
            toStringBuff.append(tx.toString());
        }

        toStringBuff.append("]");
        return toStringBuff.toString();
    }

    private byte[] parseTxs(final RLPList txTransactions, final boolean validate) {

        final Trie<byte[]> txsState = new TrieImpl();
        for (int i = 0; i < txTransactions.size(); i++) {
            final RLPElement transactionRaw = txTransactions.get(i);
            final Transaction tx = new Transaction(transactionRaw.getRLPData());
            if (validate) tx.verify();
            this.transactionsList.add(tx);
            txsState.put(RLP.encodeInt(i), transactionRaw.getRLPData());
        }
        return txsState.getRootHash();
    }


    private boolean parseTxs(final byte[] expectedRoot, final RLPList txTransactions, final boolean validate) {

        final byte[] rootHash = parseTxs(txTransactions, validate);
        final String calculatedRoot = Hex.toHexString(rootHash);
        if (!calculatedRoot.equals(Hex.toHexString(expectedRoot))) {
            logger.debug("Transactions trie root validation failed for block #{}", this.header.getNumber());
            return false;
        }

        return true;
    }

    /**
     * check if param block is son of this block
     *
     * @param block - possible a son of this
     * @return - true if this block is parent of param block
     */
    public boolean isParentOf(final Block block) {
        return Arrays.areEqual(this.getHash(), block.getParentHash());
    }

    public boolean isGenesis() {
        return this.header.isGenesis();
    }

    public boolean isEqual(final Block block) {
        return Arrays.areEqual(this.getHash(), block.getHash());
    }

    private byte[] getTransactionsEncoded() {

        final byte[][] transactionsEncoded = new byte[transactionsList.size()][];
        int i = 0;
        for (final Transaction tx : transactionsList) {
            transactionsEncoded[i] = tx.getEncoded();
            ++i;
        }
        return RLP.encodeList(transactionsEncoded);
    }

    private byte[] getUnclesEncoded() {

        final byte[][] unclesEncoded = new byte[uncleList.size()][];
        int i = 0;
        for (final BlockHeader uncle : uncleList) {
            unclesEncoded[i] = uncle.getEncoded();
            ++i;
        }
        return RLP.encodeList(unclesEncoded);
    }

    public void addUncle(final BlockHeader uncle) {
        uncleList.add(uncle);
        this.getHeader().setUnclesHash(sha3(getUnclesEncoded()));
        rlpEncoded = null;
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            final byte[] header = this.header.getEncoded();

            final List<byte[]> block = getBodyElements();
            block.add(0, header);
            final byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }

    public byte[] getEncodedWithoutNonce() {
        parseRLP();
        return this.header.getEncodedWithoutNonce();
    }

    public byte[] getEncodedBody() {
        final List<byte[]> body = getBodyElements();
        final byte[][] elements = body.toArray(new byte[body.size()][]);
        return RLP.encodeList(elements);
    }

    private List<byte[]> getBodyElements() {
        parseRLP();

        final byte[] transactions = getTransactionsEncoded();
        final byte[] uncles = getUnclesEncoded();

        final List<byte[]> body = new ArrayList<>();
        body.add(transactions);
        body.add(uncles);

        return body;
    }

    public String getShortHash() {
        parseRLP();
        return Hex.toHexString(getHash()).substring(0, 6);
    }

    public String getShortDescr() {
        return "#" + getNumber() + " (" + Hex.toHexString(getHash()).substring(0,6) + " <~ "
                + Hex.toHexString(getParentHash()).substring(0,6) + ") Txs:" + getTransactionsList().size() +
                ", Unc: " + getUncleList().size();
    }

    public static class Builder {

        private BlockHeader header;
        private byte[] body;

        public Builder withHeader(final BlockHeader header) {
            this.header = header;
            return this;
        }

        public Builder withBody(final byte[] body) {
            this.body = body;
            return this;
        }

        public Block create() {
            if (header == null || body == null) {
                return null;
            }

            final Block block = new Block();
            block.header = header;
            block.parsed = true;

            final RLPList items = (RLPList) RLP.decode2(body).get(0);

            final RLPList transactions = (RLPList) items.get(0);
            final RLPList uncles = (RLPList) items.get(1);

            if (!block.parseTxs(header.getTxTrieRoot(), transactions, false)) {
                return null;
            }

            final byte[] unclesHash = HashUtil.sha3(uncles.getRLPData());
            if (!java.util.Arrays.equals(header.getUnclesHash(), unclesHash)) {
                return null;
            }

            for (final RLPElement rawUncle : uncles) {

                final RLPList uncleHeader = (RLPList) rawUncle;
                final BlockHeader blockData = new BlockHeader(uncleHeader);
                block.uncleList.add(blockData);
            }

            return block;
        }
    }
}
