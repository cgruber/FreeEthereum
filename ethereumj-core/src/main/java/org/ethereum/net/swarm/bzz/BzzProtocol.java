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

package org.ethereum.net.swarm.bzz;

import org.ethereum.net.client.Capability;
import org.ethereum.net.swarm.Key;
import org.ethereum.net.swarm.NetStore;
import org.ethereum.net.swarm.Util;
import org.ethereum.util.Functional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.min;

/**
 * The class is the lowest level right above the network layer.
 * Responsible for BZZ handshaking, brokering inbound messages
 * and delivering outbound messages.
 *
 * Created by Anton Nashatyrev on 18.06.2015.
 */
public class BzzProtocol implements Functional.Consumer<BzzMessage> {
    public final static long ProtocolLength     = 8;
    public final static long ProtocolMaxMsgSize = 10 * 1024 * 1024;
    public final static int Strategy           = 0;
    private final static int Version = 0;
    private final static int NetworkId = 0;
    private final static Logger LOG = LoggerFactory.getLogger("net.bzz");
    private final static AtomicLong idGenerator = new AtomicLong(0);
    private final NetStore netStore;
    private Functional.Consumer<BzzMessage> messageSender;
    private PeerAddress node;

    private boolean handshaken = false;
    private boolean handshakeOut = false;
    private List<BzzMessage> pendingHandshakeOutMessages = new ArrayList<>();
    private List<BzzMessage> pendingHandshakeInMessages = new ArrayList<>();

    public BzzProtocol(final NetStore netStore) {
        this.netStore = netStore;
    }

    private static String addressToShortString(final PeerAddress addr) {
        if (addr == null) return "<null>";
        String s = Hex.toHexString(addr.getId());
        s = s.substring(0, min(8, s.length()));
        return s + "@" + Util.ipBytesToString(addr.getIp()) + ":" + addr.getPort();
    }

    /**
     * Installs the message sender.
     * Normally this is BzzHandler which just sends the message to the peer over the wire
     * In the testing environment this could be a special handler which delivers the message
     * without network stack
     */
    public void setMessageSender(final Functional.Consumer<BzzMessage> messageSender) {
        this.messageSender = messageSender;
    }

    public void start() {
        handshakeOut();
    }

    /**
     * Gets the address of the Peer connected to this instance.
     */
    public PeerAddress getNode() {
        return node;
    }

    /**
     * Sends the Status message to the peer
     */
    private void handshakeOut() {
        if (!handshakeOut) {
            handshakeOut = true;
            final BzzStatusMessage outStatus = new BzzStatusMessage(Version, "honey",
                    netStore.getSelfAddress(), NetworkId,
                    Collections.singletonList(new Capability(Capability.BZZ, (byte) 0)));
            LOG.info("Outbound handshake: " + outStatus);
            sendMessageImpl(outStatus);
        }
    }

    /**
     * Handles inbound Status Message
     */
    private void handshakeIn(final BzzStatusMessage msg) {
        if (!handshaken) {
            LOG.info("Inbound handshake: " + msg);
            netStore.statHandshakes.add(1);
            // TODO check status parameters
            node = msg.getAddr();
            netStore.getHive().addPeer(this);

            handshaken = true;
            handshakeOut();

            start();

            if (!pendingHandshakeOutMessages.isEmpty()) {
                LOG.info("Send pending handshake messages: " + pendingHandshakeOutMessages.size());
                for (final BzzMessage pmsg : pendingHandshakeOutMessages) {
                    sendMessageImpl(pmsg);
                }
            }
            pendingHandshakeOutMessages = null;

            // ping the peer for self neighbours
            sendMessageImpl(new BzzRetrieveReqMessage(Key.zeroKey()));

            if (!pendingHandshakeInMessages.isEmpty()) {
                LOG.info("Processing pending handshake inbound messages: " + pendingHandshakeInMessages.size());
                for (final BzzMessage pmsg : pendingHandshakeInMessages) {
                    handleMsg(pmsg);
                }
            }
            pendingHandshakeInMessages = null;

        } else {
            LOG.warn("Double inbound status message (ignore): " + msg);
        }
    }

    public synchronized void sendMessage(final BzzMessage msg) {
        if (handshaken) {
            sendMessageImpl(msg);
        } else {
            pendingHandshakeOutMessages.add(msg);
        }
    }

    private void sendMessageImpl(final BzzMessage msg) {
        netStore.statOutMsg.add(1);
        msg.setId(idGenerator.incrementAndGet());
        LOG.debug("<===   (to " + addressToShortString(getNode()) + ") " + msg);
        messageSender.accept(msg);
    }

    @Override
    public void accept(final BzzMessage bzzMessage) {
        handleMsg(bzzMessage);
    }

    private void handleMsg(final BzzMessage msg) {
        synchronized (netStore) {
            netStore.statInMsg.add(1);
            msg.setPeer(this);
            if (LOG.isDebugEnabled()) {
                LOG.debug(" ===>(from " + addressToShortString(getNode()) + ") " + msg);
            }
            if (msg.getCommand() == BzzMessageCodes.STATUS) {
                handshakeIn((BzzStatusMessage) msg);
            } else {
                if (!handshaken) {
                    pendingHandshakeInMessages.add(msg);
                } else {
                    switch (msg.getCommand()) {
                        case STORE_REQUEST:
                            netStore.addStoreRequest((BzzStoreReqMessage) msg);
                            break;
                        case RETRIEVE_REQUEST:
                            netStore.addRetrieveRequest((BzzRetrieveReqMessage) msg);
                            break;
                        case PEERS:
                            netStore.getHive().addPeerRecords((BzzPeersMessage) msg);
                            break;
                        default:
                            LOG.error("Invalid BZZ command: " + msg.getCommand() + ": " + msg);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BzzProtocol[" + netStore.getSelfAddress() + " => " + node + "]";
    }
}
