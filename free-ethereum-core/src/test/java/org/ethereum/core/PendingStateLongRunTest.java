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

import org.ethereum.config.CommonConfig;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.db.RepositoryRoot;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.BIUtil;
import org.ethereum.validator.DependentBlockHeaderRuleAdapter;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mikhail Kalinin
 * @since 24.09.2015
 */
@Ignore
public class PendingStateLongRunTest {

    private Blockchain blockchain;

    private PendingState pendingState;

    private List<String> strData;

    @Before
    public void setup() throws URISyntaxException, IOException, InterruptedException {

        blockchain = createBlockchain((Genesis) Genesis.getInstance());
        pendingState = ((BlockchainImpl) blockchain).getPendingState();

        final URL blocks = ClassLoader.getSystemResource("state/47250.dmp");
        final File file = new File(blocks.toURI());
        strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        for (int i = 0; i < 46000; i++) {
            final Block b = new Block(Hex.decode(strData.get(i)));
            blockchain.tryToConnect(b);
        }
    }

    @Test // test with real data from the frontier net
    public void test_1() {

        final Block b46169 = new Block(Hex.decode(strData.get(46169)));
        final Block b46170 = new Block(Hex.decode(strData.get(46170)));

        final Transaction tx46169 = b46169.getTransactionsList().get(0);
        final Transaction tx46170 = b46170.getTransactionsList().get(0);

        Repository pending = pendingState.getRepository();

        final BigInteger balanceBefore46169 = pending.getAccountState(tx46169.getReceiveAddress()).getBalance();
        final BigInteger balanceBefore46170 = pending.getAccountState(tx46170.getReceiveAddress()).getBalance();

        pendingState.addPendingTransaction(tx46169);
        pendingState.addPendingTransaction(tx46170);

        for (int i = 46000; i < 46169; i++) {
            final Block b = new Block(Hex.decode(strData.get(i)));
            blockchain.tryToConnect(b);
        }

        pending = pendingState.getRepository();

        final BigInteger balanceAfter46169 = balanceBefore46169.add(BIUtil.INSTANCE.toBI(tx46169.getValue()));

        assertEquals(pendingState.getPendingTransactions().size(), 2);
        assertEquals(balanceAfter46169, pending.getAccountState(tx46169.getReceiveAddress()).getBalance());

        blockchain.tryToConnect(b46169);
        pending = pendingState.getRepository();

        assertEquals(balanceAfter46169, pending.getAccountState(tx46169.getReceiveAddress()).getBalance());
        assertEquals(pendingState.getPendingTransactions().size(), 1);

        final BigInteger balanceAfter46170 = balanceBefore46170.add(BIUtil.INSTANCE.toBI(tx46170.getValue()));

        assertEquals(balanceAfter46170, pending.getAccountState(tx46170.getReceiveAddress()).getBalance());

        blockchain.tryToConnect(b46170);
        pending = pendingState.getRepository();

        assertEquals(balanceAfter46170, pending.getAccountState(tx46170.getReceiveAddress()).getBalance());
        assertEquals(pendingState.getPendingTransactions().size(), 0);
    }

    private Blockchain createBlockchain(final Genesis genesis) {
        final IndexedBlockStore blockStore = new IndexedBlockStore();
        blockStore.init(new HashMapDB<>(), new HashMapDB<>());

        final Repository repository = new RepositoryRoot(new HashMapDB());

        final ProgramInvokeFactoryImpl programInvokeFactory = new ProgramInvokeFactoryImpl();

        final BlockchainImpl blockchain = new BlockchainImpl(blockStore, repository)
                .withParentBlockHeaderValidator(new CommonConfig().parentHeaderValidator());
        blockchain.setParentHeaderValidator(new DependentBlockHeaderRuleAdapter());
        blockchain.setProgramInvokeFactory(programInvokeFactory);

        blockchain.byTest = true;

        final PendingStateImpl pendingState = new PendingStateImpl(new EthereumListenerAdapter(), blockchain);

        pendingState.setBlockchain(blockchain);
        blockchain.setPendingState(pendingState);

        final Repository track = repository.startTracking();
        Genesis.populateRepository(track, genesis);

        track.commit();

        blockStore.saveBlock(Genesis.getInstance(), Genesis.getInstance().getCumulativeDifficulty(), true);

        blockchain.setBestBlock(Genesis.getInstance());
        blockchain.setTotalDifficulty(Genesis.getInstance().getCumulativeDifficulty());

        return blockchain;
    }
}
