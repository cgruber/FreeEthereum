package org.ethereum.net.swarm.bzz;

import org.ethereum.net.message.Message;

/**
 * Base class for all BZZ messages
 */
public abstract class BzzMessage extends Message {

    long id = -1;
    // non-null for incoming messages
    private BzzProtocol peer;

    BzzMessage() {
    }

    BzzMessage(byte[] encoded) {
        super(encoded);
        decode();
    }

    public BzzMessageCodes getCommand() {
        return BzzMessageCodes.fromByte(code);
    }

    protected abstract void decode();

    /**
     * Returns the {@link BzzProtocol} associated with incoming message
     */
    public BzzProtocol getPeer() {
        return peer;
    }

    void setPeer(BzzProtocol peer) {
        this.peer = peer;
    }

    /**
     * Message ID. Should be unique across all outgoing messages
     */
    public long getId() {
        return id;
    }

    void setId(long id) {
        this.id = id;
    }
}
