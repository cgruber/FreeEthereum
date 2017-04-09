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

package org.ethereum.samples;

import org.ethereum.core.Block;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.ProgramResult;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateContractSample extends TestNetSample {

    private final Map<ByteArrayWrapper, TransactionReceipt> txWaiters =
            Collections.synchronizedMap(new HashMap<ByteArrayWrapper, TransactionReceipt>());
    @Autowired
    private
    SolidityCompiler compiler;

    public static void main(final String[] args) throws Exception {
        sLogger.info("Starting EthereumJ!");

        class Config extends TestNetConfig {
            @Override
            @Bean
            public TestNetSample sampleBean() {
                return new CreateContractSample();
            }
        }

        // Based on Config class the BasicSample would be created by Spring
        // and its springInit() method would be called as an entry point
        EthereumFactory.createEthereum(Config.class);
    }

    @Override
    public void onSyncDone() throws Exception {
        ethereum.addListener(new EthereumListenerAdapter() {
            // when block arrives look for our included transactions
            @Override
            public void onBlock(final Block block, final List<TransactionReceipt> receipts) {
                CreateContractSample.this.onBlock(block, receipts);
            }
        });

        logger.info("Compiling contract...");
        final String contract1 = "contract Sample {" +
                "  int i;" +
                "  function inc(int n) {" +
                "    i = i + n;" +
                "  }" +
                "  function get() returns (int) {" +
                "    return i;" +
                "  }" +
                "}";
        final SolidityCompiler.Result result = compiler.compileSrc(contract1.getBytes(), true, true,
                SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN);
        if (result.isFailed()) {
            throw new RuntimeException("Contract compilation failed:\n" + result.errors);
        }
        final CompilationResult res = CompilationResult.parse(result.output);
        if (res.contracts.isEmpty()) {
            throw new RuntimeException("Compilation failed, no contracts returned:\n" + result.errors);
        }
        final CompilationResult.ContractMetadata metadata = res.contracts.values().iterator().next();
        if (metadata.bin == null || metadata.bin.isEmpty()) {
            throw new RuntimeException("Compilation failed, no binary returned:\n" + result.errors);
        }

        logger.info("Sending contract to net and waiting for inclusion");
        final TransactionReceipt receipt = sendTxAndWait(new byte[0], Hex.decode(metadata.bin));

        if (!receipt.isSuccessful()) {
            logger.error("Some troubles creating a contract: " + receipt.getError());
            return;
        }

        final byte[] contractAddress = receipt.getTransaction().getContractAddress();
        logger.info("Contract created: " + Hex.toHexString(contractAddress));

        logger.info("Calling the contract function 'inc'");
        final CallTransaction.Contract contract = new CallTransaction.Contract(metadata.abi);
        final CallTransaction.Function inc = contract.getByName("inc");
        final byte[] functionCallBytes = inc.encode(777);
        final TransactionReceipt receipt1 = sendTxAndWait(contractAddress, functionCallBytes);
        if (!receipt1.isSuccessful()) {
            logger.error("Some troubles invoking the contract: " + receipt.getError());
            return;
        }
        logger.info("Contract modified!");

        final ProgramResult r = ethereum.callConstantFunction(Hex.toHexString(contractAddress),
                contract.getByName("get"));
        final Object[] ret = contract.getByName("get").decodeResult(r.getHReturn());
        logger.info("Current contract data member value: " + ret[0]);
    }

    private TransactionReceipt sendTxAndWait(final byte[] receiveAddress, final byte[] data) throws InterruptedException {
        final BigInteger nonce = ethereum.getRepository().getNonce(senderAddress);
        final Transaction tx = new Transaction(
                ByteUtil.bigIntegerToBytes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(ethereum.getGasPrice()),
                ByteUtil.longToBytesNoLeadZeroes(3_000_000),
                receiveAddress,
                ByteUtil.longToBytesNoLeadZeroes(0),
                data,
                ethereum.getChainIdForNextBlock());
        tx.sign(ECKey.fromPrivate(senderPrivateKey));
        logger.info("<=== Sending transaction: " + tx);
        ethereum.submitTransaction(tx);

        return waitForTx(tx.getHash());
    }

    private void onBlock(final Block block, final List<TransactionReceipt> receipts) {
        for (final TransactionReceipt receipt : receipts) {
            final ByteArrayWrapper txHashW = new ByteArrayWrapper(receipt.getTransaction().getHash());
            if (txWaiters.containsKey(txHashW)) {
                txWaiters.put(txHashW, receipt);
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    private TransactionReceipt waitForTx(final byte[] txHash) throws InterruptedException {
        final ByteArrayWrapper txHashW = new ByteArrayWrapper(txHash);
        txWaiters.put(txHashW, null);
        final long startBlock = ethereum.getBlockchain().getBestBlock().getNumber();
        while(true) {
            final TransactionReceipt receipt = txWaiters.get(txHashW);
            if (receipt != null) {
                return receipt;
            } else {
                final long curBlock = ethereum.getBlockchain().getBestBlock().getNumber();
                if (curBlock > startBlock + 16) {
                    throw new RuntimeException("The transaction was not included during last 16 blocks: " + txHashW.toString().substring(0,8));
                } else {
                    logger.info("Waiting for block with transaction 0x" + txHashW.toString().substring(0,8) +
                            " included (" + (curBlock - startBlock) + " blocks received so far) ...");
                }
            }
            synchronized (this) {
                wait(20000);
            }
        }
    }
}
