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

package org.ethereum.net.shh;


import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WhisperImpl extends Whisper {
    private final static Logger logger = LoggerFactory.getLogger("net.shh");
    final BloomFilter hostBloomFilter = BloomFilter.createAll();
    private final Set<MessageWatcher> filters = new HashSet<>();
    private final List<Topic> knownTopics = new ArrayList<>();
    private final Map<WhisperMessage, ?> known = new LRUMap<>(1024); // essentially Set
    private final Map<String, ECKey> identities = new HashMap<>();
    private final List<ShhHandler> activePeers = new ArrayList<>();

    public WhisperImpl() {
    }

    public static String toIdentity(final ECKey key) {
        return Hex.toHexString(key.getNodeId());
    }

    public static ECKey fromIdentityToPub(final String identity) {
        try {
            return identity == null ? null :
                    ECKey.fromPublicOnly(ByteUtil.merge(new byte[]{0x04}, Hex.decode(identity)));
        } catch (final Exception e) {
            throw new RuntimeException("Converting identity '" + identity + "'", e);
        }
    }

    @Override
    public void send(final String from, final String to, final byte[] payload, final Topic[] topicList, final int ttl, final int workToProve) {
        ECKey fromKey = null;
        if (from != null && !from.isEmpty()) {
            fromKey = getIdentity(from);
            if (fromKey == null) {
                throw new Error(String.format("Unknown identity to send from %s", from));
            }
        }

        final WhisperMessage m = new WhisperMessage()
                .setFrom(fromKey)
                .setTo(to)
                .setPayload(payload)
                .setTopics(topicList)
                .setTtl(ttl)
                .setWorkToProve(workToProve);

        logger.info("Sending Whisper message: " + m);

        addMessage(m, null);
    }

    public void processEnvelope(final ShhEnvelopeMessage e, final ShhHandler shhHandler) {
        for (final WhisperMessage message : e.getMessages()) {
            message.decrypt(identities.values(), knownTopics);
            logger.info("New Whisper message: " + message);
            addMessage(message, shhHandler);
        }
    }

    void addPeer(final ShhHandler peer) {
        activePeers.add(peer);
    }

    void removePeer(final ShhHandler peer) {
        activePeers.remove(peer);
    }

    public void watch(final MessageWatcher f) {
        filters.add(f);
        for (final Topic topic : f.getTopics()) {
            hostBloomFilter.addTopic(topic);
            knownTopics.add(topic);
        }
        notifyBloomFilterChanged();
    }

    public void unwatch(final MessageWatcher f) {
        filters.remove(f);
        for (final Topic topic : f.getTopics()) {
            hostBloomFilter.removeTopic(topic);
        }
        notifyBloomFilterChanged();
    }

    private void notifyBloomFilterChanged() {
        for (final ShhHandler peer : activePeers) {
            peer.sendHostBloom();
        }
    }

    // Processing both messages:
    // own outgoing messages (shhHandler == null)
    // and inbound messages from peers
    private void addMessage(final WhisperMessage m, final ShhHandler inboundPeer) {
        if (!known.containsKey(m)) {
            known.put(m, null);
            if (inboundPeer != null) {
                matchMessage(m);
            }

            for (final ShhHandler peer : activePeers) {
                if (peer != inboundPeer) {
                    peer.sendEnvelope(new ShhEnvelopeMessage(m));
                }
            }
        }
    }

    private void matchMessage(final WhisperMessage m) {
        for (final MessageWatcher f : filters) {
            if (f.match(m.getTo(), m.getFrom(), m.getTopics())) {
                f.newMessage(m);
            }
        }
    }

    @Override
    public String addIdentity(final ECKey key) {
        final String identity = toIdentity(key);
        identities.put(identity, key);
        return identity;
    }

    @Override
    public String newIdentity() {
        return addIdentity(new ECKey());
    }

    private ECKey getIdentity(final String identity) {
        if (identities.containsKey(identity)) {
            return identities.get(identity);
        }

        return null;
    }
}
