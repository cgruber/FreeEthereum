package org.ethereum.net.rlpx;

import org.ethereum.net.rlpx.discover.table.KademliaOptions;
import org.ethereum.net.rlpx.discover.table.NodeBucket;
import org.ethereum.net.rlpx.discover.table.NodeEntry;
import org.ethereum.net.rlpx.discover.table.NodeTable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class KademliaTest {

    private static byte[] getNodeId() {
        Random gen = new Random();
        byte[] id = new byte[64];
        gen.nextBytes(id);
        return id;
    }

    private static Node getNode(byte[] id, int i) {
        id[0] += (byte) i;
        Node n = getNode();
        n.setId(id);
        return n;
    }

    private static Node getNode() {
        return new Node(getNodeId(), "127.0.0.1", 30303);
    }

    private static NodeTable getTestNodeTable(int nodesQuantity) {
        NodeTable testTable = new NodeTable(getNode());

        for (int i = 0; i < nodesQuantity; i++) {
            testTable.addNode(getNode());
        }

        return testTable;
    }

    private static void showBuckets(NodeTable t) {
        for (NodeBucket b : t.getBuckets()) {
            if (b.getNodesCount() > 0) {
                System.out.println(String.format("Bucket %d nodes %d depth %d", b.getDepth(), b.getNodesCount(), b.getDepth()));
            }
        }
    }

    private static boolean containsNode(NodeTable t, Node n) {
        for (NodeEntry e : t.getAllNodes()) {
            if (e.getNode().toString().equals(n.toString())) {
                return true;
            }
        }
        return false;
    }

    @Ignore
    @Test
    public void test1() {
        //init table with one home node
        NodeTable t = getTestNodeTable(0);
        Node homeNode = t.getNode();

        //table should contain the home node only
        assertEquals(t.getAllNodes().size(), 1);

        Node bucketNode = t.getAllNodes().get(0).getNode();

        assertEquals(homeNode, bucketNode);

    }

    @Test
    public void test2() {
        NodeTable t = getTestNodeTable(0);
        Node n = getNode();
        t.addNode(n);

        assertTrue(containsNode(t, n));

        t.dropNode(n);

        assertFalse(containsNode(t, n));
    }

    @Test
    public void test3() {
        NodeTable t = getTestNodeTable(1000);
        showBuckets(t);

        List<Node> closest1 = t.getClosestNodes(t.getNode().getId());
        List<Node> closest2 = t.getClosestNodes(getNodeId());

        assertNotEquals(closest1, closest2);
    }

    @Test
    public void test4() {
        NodeTable t = getTestNodeTable(0);
        Node homeNode = t.getNode();

        //t.getBucketsCount() returns non empty buckets
        assertEquals(t.getBucketsCount(), 1);

        //creates very close nodes
        for (int i = 1; i < KademliaOptions.BUCKET_SIZE; i++) {
            Node n= getNode(homeNode.getId(), i);
            t.addNode(n);
        }

        assertEquals(t.getBucketsCount(), 1);
        assertEquals(t.getBuckets()[0].getNodesCount(), KademliaOptions.BUCKET_SIZE);
    }
}
