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

package org.ethereum.net.eth.handler;

import com.google.common.util.concurrent.ListenableFuture;
import org.ethereum.core.*;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.sync.SyncStatistics;

import java.math.BigInteger;
import java.util.List;

import static org.ethereum.net.eth.EthVersion.Companion;

/**
 * It's quite annoying to always check {@code if (eth != null)} before accessing it. <br>
 *
 * This adapter helps to avoid such checks. It provides meaningful answers to Eth client
 * assuming that Eth hasn't been initialized yet. <br>
 *
 * Check {@link org.ethereum.net.server.Channel} for example.
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public class EthAdapter implements Eth {

    private final SyncStatistics syncStats = new SyncStatistics();

    @Override
    public boolean hasStatusPassed() {
        return false;
    }

    @Override
    public boolean hasStatusSucceeded() {
        return false;
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public String getSyncStats() {
        return "";
    }

    @Override
    public boolean isHashRetrievingDone() {
        return false;
    }

    @Override
    public boolean isHashRetrieving() {
        return false;
    }

    @Override
    public boolean isIdle() {
        return true;
    }

    @Override
    public SyncStatistics getStats() {
        return syncStats;
    }

    @Override
    public void disableTransactions() {
    }

    @Override
    public void enableTransactions() {
    }

    @Override
    public void sendTransaction(List<Transaction> tx) {
    }

    @Override
    public ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(long blockNumber, int maxBlocksAsk, boolean reverse) {
        return null;
    }

    @Override
    public ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(byte[] blockHash, int maxBlocksAsk, int skip, boolean reverse) {
        return null;
    }

    @Override
    public ListenableFuture<List<Block>> sendGetBlockBodies(List<BlockHeaderWrapper> headers) {
        return null;
    }

    @Override
    public void sendNewBlock(Block newBlock) {
    }

    @Override
    public void sendNewBlockHashes(Block block) {

    }

    @Override
    public EthVersion getVersion() {
        return Companion.fromCode(Companion.getUPPER());
    }

    @Override
    public void onSyncDone(boolean done) {
    }

    @Override
    public void sendStatus() {
    }

    @Override
    public void dropConnection() {
    }

    @Override
    public void fetchBodies(List<BlockHeaderWrapper> headers) {
    }

    @Override
    public BlockIdentifier getBestKnownBlock() {
        return null;
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return BigInteger.ZERO;
    }

}
