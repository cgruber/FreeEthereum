package org.ethereum.net.p2p;

import org.ethereum.net.message.Message;

public abstract class P2pMessage extends Message {

    P2pMessage() {
    }

    P2pMessage(byte[] encoded) {
        super(encoded);
    }

    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.fromByte(code);
    }
}
