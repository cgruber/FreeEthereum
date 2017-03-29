package org.ethereum.net.rlpx.discover;

import org.ethereum.net.rlpx.Node;
import org.ethereum.net.rlpx.discover.table.KademliaOptions;
import org.ethereum.net.rlpx.discover.table.NodeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class DiscoverTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("discover");

    private final NodeManager nodeManager;

    private final byte[] nodeId;

    public DiscoverTask(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        nodeId = nodeManager.homeNode.getId();
    }

    @Override
    public void run() {
        discover(nodeId, 0, new ArrayList<>());
    }

    synchronized void discover(byte[] nodeId, int round, List<Node> prevTried) {

        try {
//        if (!channel.isOpen() || round == KademliaOptions.MAX_STEPS) {
//            logger.info("{}", String.format("Nodes discovered %d ", table.getAllNodes().size()));
//            return;
//        }

            if (round == KademliaOptions.MAX_STEPS) {
                logger.debug("Node table contains [{}] peers", nodeManager.getTable().getNodesCount());
                logger.debug("{}", String.format("(KademliaOptions.MAX_STEPS) Terminating discover after %d rounds.", round));
                logger.trace("{}\n{}", String.format("Nodes discovered %d ", nodeManager.getTable().getNodesCount()), dumpNodes());
                return;
            }

            List<Node> closest = nodeManager.getTable().getClosestNodes(nodeId);
            List<Node> tried = new ArrayList<>();

            for (Node n : closest) {
                if (!tried.contains(n) && !prevTried.contains(n)) {
                    try {
                        nodeManager.getNodeHandler(n).sendFindNode(nodeId);
                        tried.add(n);
                        Thread.sleep(50);
                    }catch (InterruptedException e) {
                    } catch (Exception ex) {
                        logger.error("Unexpected Exception " + ex, ex);
                    }
                }
                if (tried.size() == KademliaOptions.ALPHA) {
                    break;
                }
            }

//            channel.flush();

            if (tried.isEmpty()) {
                logger.debug("{}", String.format("(tried.isEmpty()) Terminating discover after %d rounds.", round));
                logger.trace("{}\n{}", String.format("Nodes discovered %d ", nodeManager.getTable().getNodesCount()), dumpNodes());
                return;
            }

            tried.addAll(prevTried);

            discover(nodeId, round + 1, tried);
        } catch (Exception ex) {
            logger.info("{}", ex);
        }
    }

    private String dumpNodes() {
        StringBuilder ret = new StringBuilder();
        for (NodeEntry entry : nodeManager.getTable().getAllNodes()) {
            ret.append("    ").append(entry.getNode()).append("\n");
        }
        return ret.toString();
    }
}
