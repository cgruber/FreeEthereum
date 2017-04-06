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

import org.ethereum.util.Functional;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.ethereum.util.ByteUtil.toHexString;

public class BlockSummary {

    private final Block block;
    private final Map<byte[], BigInteger> rewards;
    private final List<TransactionReceipt> receipts;
    private final List<TransactionExecutionSummary> summaries;
    private BigInteger totalDifficulty = BigInteger.ZERO;

    public BlockSummary(final byte[] rlp) {
        final RLPList summary = (RLPList) RLP.decode2(rlp).get(0);

        this.block = new Block(summary.get(0).getRLPData());
        this.rewards = decodeRewards((RLPList) summary.get(1));
        this.summaries = decodeSummaries((RLPList) summary.get(2));
        this.receipts = new ArrayList<>();

        final Map<String, TransactionReceipt> receiptByTxHash = decodeReceipts((RLPList) summary.get(3));
        for (final Transaction tx : this.block.getTransactionsList()) {
            final TransactionReceipt receipt = receiptByTxHash.get(toHexString(tx.getHash()));
            receipt.setTransaction(tx);

            this.receipts.add(receipt);
        }
    }

    public BlockSummary(final Block block, final Map<byte[], BigInteger> rewards, final List<TransactionReceipt> receipts, final List<TransactionExecutionSummary> summaries) {
        this.block = block;
        this.rewards = rewards;
        this.receipts = receipts;
        this.summaries = summaries;
    }

    private static <T> byte[] encodeList(final List<T> entries, final Functional.Function<T, byte[]> encoder) {
        final byte[][] result = new byte[entries.size()][];
        for (int i = 0; i < entries.size(); i++) {
            result[i] = encoder.apply(entries.get(i));
        }

        return RLP.encodeList(result);
    }

    private static <T> List<T> decodeList(final RLPList list, final Functional.Function<byte[], T> decoder) {
        final List<T> result = new ArrayList<>();
        for (final RLPElement item : list) {
            result.add(decoder.apply(item.getRLPData()));
        }
        return result;
    }

    private static <K, V> byte[] encodeMap(final Map<K, V> map, final Functional.Function<K, byte[]> keyEncoder, final Functional.Function<V, byte[]> valueEncoder) {
        final byte[][] result = new byte[map.size()][];
        int i = 0;
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            final byte[] key = keyEncoder.apply(entry.getKey());
            final byte[] value = valueEncoder.apply(entry.getValue());
            result[i++] = RLP.encodeList(key, value);
        }
        return RLP.encodeList(result);
    }

    private static <K, V> Map<K, V> decodeMap(final RLPList list, final Functional.Function<byte[], K> keyDecoder, final Functional.Function<byte[], V> valueDecoder) {
        final Map<K, V> result = new HashMap<>();
        for (final RLPElement entry : list) {
            final K key = keyDecoder.apply(((RLPList) entry).get(0).getRLPData());
            final V value = valueDecoder.apply(((RLPList) entry).get(1).getRLPData());
            result.put(key, value);
        }
        return result;
    }

    private static byte[] encodeSummaries(final List<TransactionExecutionSummary> summaries) {
        return encodeList(summaries, new Functional.Function<TransactionExecutionSummary, byte[]>() {
            @Override
            public byte[] apply(final TransactionExecutionSummary summary) {
                return summary.getEncoded();
            }
        });
    }

    private static List<TransactionExecutionSummary> decodeSummaries(final RLPList summaries) {
        return decodeList(summaries, new Functional.Function<byte[], TransactionExecutionSummary>() {
            @Override
            public TransactionExecutionSummary apply(final byte[] encoded) {
                return new TransactionExecutionSummary(encoded);
            }
        });
    }

    private static byte[] encodeReceipts(final List<TransactionReceipt> receipts) {
        final Map<String, TransactionReceipt> receiptByTxHash = new HashMap<>();
        for (final TransactionReceipt receipt : receipts) {
            receiptByTxHash.put(toHexString(receipt.getTransaction().getHash()), receipt);
        }

        return encodeMap(receiptByTxHash, new Functional.Function<String, byte[]>() {
            @Override
            public byte[] apply(final String txHash) {
                return RLP.encodeString(txHash);
            }
        }, new Functional.Function<TransactionReceipt, byte[]>() {
            @Override
            public byte[] apply(final TransactionReceipt receipt) {
                return receipt.getEncoded();
            }
        });
    }

    private static Map<String, TransactionReceipt> decodeReceipts(final RLPList receipts) {
        return decodeMap(receipts, new Functional.Function<byte[], String>() {
            @Override
            public String apply(final byte[] bytes) {
                return new String(bytes);
            }
        }, new Functional.Function<byte[], TransactionReceipt>() {
            @Override
            public TransactionReceipt apply(final byte[] encoded) {
                return new TransactionReceipt(encoded);
            }
        });
    }

    private static byte[] encodeRewards(final Map<byte[], BigInteger> rewards) {
        return encodeMap(rewards, new Functional.Function<byte[], byte[]>() {
            @Override
            public byte[] apply(final byte[] bytes) {
                return RLP.encodeElement(bytes);
            }
        }, new Functional.Function<BigInteger, byte[]>() {
            @Override
            public byte[] apply(final BigInteger reward) {
                return RLP.encodeBigInteger(reward);
            }
        });
    }

    private static Map<byte[], BigInteger> decodeRewards(final RLPList rewards) {
        return decodeMap(rewards, new Functional.Function<byte[], byte[]>() {
            @Override
            public byte[] apply(final byte[] bytes) {
                return bytes;
            }
        }, new Functional.Function<byte[], BigInteger>() {
            @Override
            public BigInteger apply(final byte[] bytes) {
                return isEmpty(bytes) ? BigInteger.ZERO : new BigInteger(1, bytes);
            }
        });
    }

    public Block getBlock() {
        return block;
    }

    public List<TransactionReceipt> getReceipts() {
        return receipts;
    }

    public List<TransactionExecutionSummary> getSummaries() {
        return summaries;
    }

    /**
     * All the mining rewards paid out for this block, including the main block rewards, uncle rewards, and transaction fees.
     */
    public Map<byte[], BigInteger> getRewards() {
        return rewards;
    }

    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    public void setTotalDifficulty(final BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    public byte[] getEncoded() {
        return RLP.encodeList(
                block.getEncoded(),
                encodeRewards(rewards),
                encodeSummaries(summaries),
                encodeReceipts(receipts)
        );
    }
}
