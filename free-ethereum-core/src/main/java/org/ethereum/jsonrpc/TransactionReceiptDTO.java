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

package org.ethereum.jsonrpc;

import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.LogInfo;

import static org.ethereum.jsonrpc.TypeConverter.toJsonHex;

public class TransactionReceiptDTO {

    public final long gasUsed;             //The amount of gas used by this specific transaction alone.
    public long blockNumber;         // block number where this transaction was in.
    public String contractAddress; // The contract address created, if the transaction was a contract creation, otherwise  null .

    public TransactionReceiptDTO(final Block block, final TransactionInfo txInfo) {
        final TransactionReceipt receipt = txInfo.getReceipt();

        String transactionHash = toJsonHex(receipt.getTransaction().getHash());
        int transactionIndex = txInfo.getIndex();
        long cumulativeGasUsed = ByteUtil.byteArrayToLong(receipt.getCumulativeGas());
        gasUsed = ByteUtil.byteArrayToLong(receipt.getGasUsed());
        if (receipt.getTransaction().getContractAddress() != null)
            contractAddress = toJsonHex(receipt.getTransaction().getContractAddress());
        JsonRpc.LogFilterElement[] logs = new JsonRpc.LogFilterElement[receipt.getLogInfoList().size()];
        if (block != null) {
            blockNumber = block.getNumber();
            String blockHash = toJsonHex(txInfo.getBlockHash());
            for (int i = 0; i < logs.length; i++) {
                final LogInfo logInfo = receipt.getLogInfoList().get(i);
                logs[i] = new JsonRpc.LogFilterElement(logInfo, block, txInfo.getIndex(),
                        txInfo.getReceipt().getTransaction(), i);
            }
        }
    }
}
