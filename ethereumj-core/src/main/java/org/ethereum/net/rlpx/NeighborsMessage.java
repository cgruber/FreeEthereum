package org.ethereum.net.rlpx;

import org.ethereum.crypto.ECKey;
import org.ethereum.util.*;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

public class NeighborsMessage extends Message {

    private List<Node> nodes;
    private long expires;

    public static NeighborsMessage create(List<Node> nodes, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[][] nodeRLPs = null;

        if (nodes != null) {
            nodeRLPs = new byte[nodes.size()][];
            int i = 0;
            for (Node node : nodes) {
                nodeRLPs[i] = node.getRLP();
                ++i;
            }
        }

        byte[] rlpListNodes = RLP.encodeList(nodeRLPs);
        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{4};
        byte[] data = RLP.encodeList(rlpListNodes, rlpExp);

        NeighborsMessage neighborsMessage = new NeighborsMessage();
        neighborsMessage.encode(type, data, privKey);
        neighborsMessage.nodes = nodes;
        neighborsMessage.expires = expiration;

        return neighborsMessage;
    }

    @Override
    public void parse(byte[] data) {
        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        RLPList nodesRLP = (RLPList) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        nodes = new ArrayList<>();

        for (RLPElement aNodesRLP : nodesRLP) {
            RLPList nodeRLP = (RLPList) aNodesRLP;
            Node node = new Node(nodeRLP.getRLPData());
            nodes.add(node);
        }
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public long getExpires() {
        return expires;
    }


    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[NeighborsMessage] \n nodes [%d]: %s \n expires in %d seconds \n %s\n",
                this.getNodes().size(), this.getNodes(), (expires - currTime), super.toString());

        return out;
    }


}
