package org.ethereum.net.eth.message;

import org.ethereum.net.message.Message;

public abstract class EthMessage extends Message {

    EthMessage() {
    }

    EthMessage(byte[] encoded) {
        super(encoded);
    }

    abstract public EthMessageCodes getCommand();
}
