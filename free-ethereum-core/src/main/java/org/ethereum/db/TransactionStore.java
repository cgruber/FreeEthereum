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

package org.ethereum.db;

import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.core.TransactionInfo;
import org.ethereum.datasource.ObjectDataSource;
import org.ethereum.datasource.Serializer;
import org.ethereum.datasource.Source;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Storage (tx hash) => List of (block idx, tx idx, TransactionReceipt)
 *
 * Since a transaction could be included into blocks from different forks and
 * have different receipts the class stores all of them (the same manner fork blocks are stored)
 *
 * NOTE: the TransactionInfo instances returned contains TransactionReceipt which
 * has no initialized Transaction object. If needed use BlockStore to retrieve and setup
 * Transaction instance
 *
 * Created by Anton Nashatyrev on 07.04.2016.
 */
@Component
public class TransactionStore extends ObjectDataSource<List<TransactionInfo>> {
    private static final Logger logger = LoggerFactory.getLogger("db");
    private final static Serializer<List<TransactionInfo>, byte[]> serializer =
            new Serializer<List<TransactionInfo>, byte[]>() {
        @Override
        public byte[] serialize(final List<TransactionInfo> object) {
            final byte[][] txsRlp = new byte[object.size()][];
            for (int i = 0; i < txsRlp.length; i++) {
                txsRlp[i] = object.get(i).getEncoded();
            }
            return RLP.encodeList(txsRlp);
        }

        @Override
        public List<TransactionInfo> deserialize(final byte[] stream) {
            try {
                if (stream == null) return null;
                final RLPList params = RLP.decode2(stream);
                final RLPList infoList = (RLPList) params.get(0);
                final List<TransactionInfo> ret = new ArrayList<>();
                for (final RLPElement anInfoList : infoList) {
                    ret.add(new TransactionInfo(anInfoList.getRLPData()));
                }
                return ret;
            } catch (final Exception e) {
                // fallback to previous DB version
                return Collections.singletonList(new TransactionInfo(stream));
            }
        }
    };
    private final LRUMap<ByteArrayWrapper, Object> lastSavedTxHash = new LRUMap<>(5000);
    private final Object object = new Object();

    public TransactionStore(final Source<byte[], byte[]> src) {
        super(src, serializer, 256);
    }

    /**
     * Adds TransactionInfo to the store.
     * If entries for this transaction already exist the method adds new entry to the list
     * if no entry for the same block exists
     * @return true if TransactionInfo was added, false if already exist
     */
    public boolean put(final TransactionInfo tx) {
        final byte[] txHash = tx.getReceipt().getTransaction().getHash();

        List<TransactionInfo> existingInfos = null;
        synchronized (lastSavedTxHash) {
            if (lastSavedTxHash.put(new ByteArrayWrapper(txHash), object) != null || !lastSavedTxHash.isFull()) {
                existingInfos = get(txHash);
            }
        }
        // else it is highly unlikely that the transaction was included into another block
        // earlier than 5000 transactions before with regard to regular block import process

        if (existingInfos == null) {
            existingInfos = new ArrayList<>();
        } else {
            for (final TransactionInfo info : existingInfos) {
                if (FastByteComparisons.equal(info.getBlockHash(), tx.getBlockHash())) {
                    return false;
                }
            }
        }
        existingInfos.add(tx);
        put(txHash, existingInfos);

        return true;
    }

    public TransactionInfo get(final byte[] txHash, final byte[] blockHash) {
        final List<TransactionInfo> existingInfos = get(txHash);
        for (final TransactionInfo info : existingInfos) {
            if (FastByteComparisons.equal(info.getBlockHash(), blockHash)) {
                return info;
            }
        }
        return null;
    }

    @PreDestroy
    public void close() {
//        try {
//            logger.info("Closing TransactionStore...");
//            super.close();
//        } catch (Exception e) {
//            logger.warn("Problems closing TransactionStore", e);
//        }
    }
}
