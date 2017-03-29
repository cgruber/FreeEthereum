package org.ethereum.net.dht;

import java.util.ArrayList;
import java.util.List;

public class Bucket {

    private static final int MAX_KADEMLIA_K = 5;
    private final String name;
    // if bit = 1 go left
    private Bucket left;
    // if bit = 0 go right
    private Bucket right;
    private List<Peer> peers = new ArrayList<>();


    private Bucket(String name) {
        this.name = name;
    }


    private void add(Peer peer) {

        if (peer == null) throw new Error("Not a leaf");

        if ( peers == null){

            if (peer.nextBit(name) == 1)
                left.add(peer);
            else
                right.add(peer);

            return;
        }

        peers.add(peer);

        if (peers.size() > MAX_KADEMLIA_K)
            splitBucket();
    }

    private void splitBucket() {
        left = new Bucket(name + "1");
        right = new Bucket(name + "0");

        for (Peer id : peers) {
            if (id.nextBit(name) == 1)
                left.add(id);
            else
                right.add(id);
        }

        this.peers = null;
    }


    public Bucket left() {
        return left;
    }

    public Bucket right() {
        return right;
    }


    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(name).append("\n");

        if (peers == null) return sb.toString();

        for (Peer id : peers)
            sb.append(id.toBinaryString()).append("\n");

        return sb.toString();
    }


    public void traverseTree(DoOnTree doOnTree) {

        if (left  != null) left.traverseTree(doOnTree);
        if (right != null) right.traverseTree(doOnTree);

        doOnTree.call(this);
    }


    /********************/
     // tree operations //
    public String getName() {
        return name;
    }

    public List<Peer> getPeers() {
        return peers;
    }

    /********************/

    public interface DoOnTree {

        void call(Bucket bucket);
    }

    public static class SaveLeaf implements DoOnTree {

        List<Bucket> leafs = new ArrayList<>();

        @Override
        public void call(Bucket bucket) {
            if (bucket.peers != null) leafs.add(bucket);
        }

        public List<Bucket> getLeafs() {
            return leafs;
        }

        public void setLeafs(List<Bucket> leafs) {
            this.leafs = leafs;
        }
    }
}
