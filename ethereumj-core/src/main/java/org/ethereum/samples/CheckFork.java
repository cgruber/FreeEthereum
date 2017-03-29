package org.ethereum.samples;

import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.datasource.Source;
import org.ethereum.db.IndexedBlockStore;

import java.util.List;

/**
 * Created by Anton Nashatyrev on 21.07.2016.
 */
class CheckFork {
    public static void main(String[] args) throws Exception {
        SystemProperties.getDefault().overrideParams("database.dir", "");
        Source<byte[], byte[]> index = CommonConfig.getDefault().cachedDbSource("index");
        Source<byte[], byte[]> blockDS = CommonConfig.getDefault().cachedDbSource("block");

        IndexedBlockStore indexedBlockStore = new IndexedBlockStore();
        indexedBlockStore.init(index, blockDS);

        for (int i = 1_919_990; i < 1_921_000; i++) {
            Block chainBlock = indexedBlockStore.getChainBlockByNumber(i);
            List<Block> blocks = indexedBlockStore.getBlocksByNumber(i);
            StringBuilder s = new StringBuilder(chainBlock.getShortDescr() + " (");
            for (Block block : blocks) {
                if (!block.isEqual(chainBlock)) {
                    s.append(block.getShortDescr()).append(" ");
                }
            }
            System.out.println(s);
        }
    }
}
