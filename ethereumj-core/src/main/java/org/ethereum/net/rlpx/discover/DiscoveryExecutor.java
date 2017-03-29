package org.ethereum.net.rlpx.discover;

import org.ethereum.net.rlpx.discover.table.KademliaOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class DiscoveryExecutor {

    private final ScheduledExecutorService discoverer = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();

    private final NodeManager nodeManager;

    public DiscoveryExecutor(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void start() {
        discoverer.scheduleWithFixedDelay(
                new DiscoverTask(nodeManager),
                1, KademliaOptions.DISCOVER_CYCLE, TimeUnit.SECONDS);

        refresher.scheduleWithFixedDelay(
                new RefreshTask(nodeManager),
                1, KademliaOptions.BUCKET_REFRESH, TimeUnit.MILLISECONDS);

    }

    public void close() {
        discoverer.shutdownNow();
        refresher.shutdownNow();
    }
}
