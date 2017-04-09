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

package org.ethereum.manager;


import org.ethereum.config.SystemProperties;
import org.ethereum.core.*;
import org.ethereum.db.DbFlushManager;
import org.ethereum.util.*;
import org.ethereum.validator.BlockHeaderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

@Component
public class BlockLoader {
    private static final Logger logger = LoggerFactory.getLogger("blockqueue");
    private final DateFormat df = new SimpleDateFormat("HH:mm:ss.SSSS");
    @Autowired
    private
    SystemProperties config;
    @Autowired
    private
    DbFlushManager dbFlushManager;
    private ExecutorPipeline<Block, Block> exec1;
    private ExecutorPipeline<Block, ?> exec2;
    @Autowired
    private BlockHeaderValidator headerValidator;
    @Autowired
    private BlockchainImpl blockchain;

    private void blockWork(final Block block) {
        if (block.getNumber() >= blockchain.getBlockStore().getBestBlock().getNumber() || blockchain.getBlockStore().getBlockByHash(block.getHash()) == null) {

            if (block.getNumber() > 0 && !isValid(block.getHeader())) {
                throw new RuntimeException();
            }

            final long s = System.currentTimeMillis();
            final ImportResult result = blockchain.tryToConnect(block);

            if (block.getNumber() % 10 == 0) {
                System.out.println(df.format(new Date()) + " Imported block " + block.getShortDescr() + ": " + result + " (prework: "
                        + exec1.getQueue().size() + ", work: " + exec2.getQueue().size() + ", blocks: " + exec1.getOrderMap().size() + ") in " +
                        (System.currentTimeMillis() - s) + " ms");
            }

        } else {

            if (block.getNumber() % 10000 == 0)
                System.out.println("Skipping block #" + block.getNumber());
        }
    }

    public void loadBlocks() {
        exec1 = new ExecutorPipeline(8, 1000, true, new Functional.Function<Block, Block>() {
            @Override
            public Block apply(final Block b) {
                if (b.getNumber() >= blockchain.getBlockStore().getBestBlock().getNumber()) {
                    for (final Transaction tx : b.getTransactionsList()) {
                        tx.getSender();
                    }
                }
                return b;
            }
        }, new Functional.Consumer<Throwable>() {
            @Override
            public void accept(final Throwable throwable) {
                logger.error("Unhandled exception: ", throwable);
            }
        });

        exec2 = exec1.add(1, 1000, new Functional.Consumer<Block>() {
            @Override
            public void accept(final Block block) {
                try {
                    blockWork(block);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final String fileSrc = config.blocksLoader();
        try {
            final String blocksFormat = config.getConfig().hasPath("blocks.format") ? config.getConfig().getString("blocks.format") : null;
            System.out.println("Loading blocks: " + fileSrc + ", format: " + blocksFormat);

            if ("rlp".equalsIgnoreCase(blocksFormat)) {     // rlp encoded bytes
                final Path path = Paths.get(fileSrc);
                // NOT OPTIMAL, but fine for tests
                final byte[] data = Files.readAllBytes(path);
                final RLPList list = RLP.decode2(data);
                for (final RLPElement item : list) {
                    final Block block = new Block(item.getRLPData());
                    exec1.push(block);
                }
            } else {                                        // hex string
                final FileInputStream inputStream = new FileInputStream(fileSrc);
                Scanner scanner = new Scanner(inputStream, "UTF-8");

                while (scanner.hasNextLine()) {

                    final byte[] blockRLPBytes = Hex.decode(scanner.nextLine());
                    final Block block = new Block(blockRLPBytes);

                    exec1.push(block);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        try {
            exec1.join();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        dbFlushManager.flushSync();

        System.out.println(" * Done * ");
        System.exit(0);
    }

    private boolean isValid(final BlockHeader header) {
        return headerValidator.validateAndLog(header, logger);
    }
}
