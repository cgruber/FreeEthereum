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

package org.ethereum.mine;

import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.core.genesis.GenesisLoader;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.PruneManager;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.junit.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Anton Nashatyrev on 10.12.2015.
 */
public class MineBlock {

    @Mock
    PruneManager pruneManager;

    @InjectMocks
    @Resource
    private
    BlockchainImpl blockchain = ImportLightTest.Companion.createBlockchain(GenesisLoader.loadGenesis(
            getClass().getResourceAsStream("/genesis/genesis-light.json")));

    @BeforeClass
    public static void setup() {
        SystemProperties.getDefault().setBlockchainConfig(StandaloneBlockchain.getEasyMiningConfig());
    }

    @AfterClass
    public static void cleanup() {
        SystemProperties.resetToDefault();
    }

    @Before
    public void setUp() throws Exception {
        // Initialize mocks created above
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void mine1() throws Exception {

        blockchain.setMinerCoinbase(Hex.decode("ee0250c19ad59305b2bdb61f34b45b72fe37154f"));
        Block parent = blockchain.getBestBlock();

        List<Transaction> pendingTx = new ArrayList<>();

        ECKey senderKey = ECKey.fromPrivate(Hex.decode("3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c"));
        byte[] receiverAddr = Hex.decode("31e2e1ed11951c7091dfba62cd4b7145e947219c");
        Transaction tx = new Transaction(new byte[] {0}, new byte[] {1}, ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr, new byte[] {77}, new byte[0]);
        tx.sign(senderKey);
        pendingTx.add(tx);

        Block b = blockchain.createNewBlock(parent, pendingTx, Collections.EMPTY_LIST);

        System.out.println("Mining...");
        Ethash.getForBlock(SystemProperties.getDefault(), b.getNumber()).mineLight(b).get();
        System.out.println("Validating...");
        boolean valid = Ethash.getForBlock(SystemProperties.getDefault(), b.getNumber()).validate(b.getHeader());
        Assert.assertTrue(valid);

        System.out.println("Connecting...");
        ImportResult importResult = blockchain.tryToConnect(b);

        Assert.assertTrue(importResult == ImportResult.IMPORTED_BEST);
        System.out.println(Hex.toHexString(blockchain.getRepository().getRoot()));
    }
}
