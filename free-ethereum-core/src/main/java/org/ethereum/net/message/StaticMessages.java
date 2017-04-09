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

package org.ethereum.net.message;

import org.ethereum.config.SystemProperties;
import org.ethereum.net.client.Capability;
import org.ethereum.net.client.ConfigCapabilities;
import org.ethereum.net.p2p.*;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains static values of messages on the network. These message
 * will always be the same and therefore don't need to be created each time.
 *
 * @author Roman Mandeleil
 * @since 13.04.14
 */
@Component
public class StaticMessages {

    public final static PingMessage PING_MESSAGE = new PingMessage();
    public final static PongMessage PONG_MESSAGE = new PongMessage();
    public final static GetPeersMessage GET_PEERS_MESSAGE = new GetPeersMessage();
    public final static DisconnectMessage DISCONNECT_MESSAGE = new DisconnectMessage(ReasonCode.REQUESTED);
    public static final byte[] SYNC_TOKEN = Hex.decode("22400891");
    @Autowired
    private
    SystemProperties config;
    @Autowired
    private
    ConfigCapabilities configCapabilities;

    public HelloMessage createHelloMessage(final String peerId) {
        return createHelloMessage(peerId, config.listenPort());
    }

    private HelloMessage createHelloMessage(final String peerId, final int listenPort) {

        final String helloAnnouncement = buildHelloAnnouncement();
        final byte p2pVersion = (byte) config.defaultP2PVersion();
        final List<Capability> capabilities = configCapabilities.getConfigCapabilities();

        return new HelloMessage(p2pVersion, helloAnnouncement,
                capabilities, listenPort, peerId);
    }

    private String buildHelloAnnouncement() {
        final String version = config.projectVersion();
        String numberVersion = version;
        final Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)*");
        final Matcher matcher = pattern.matcher(numberVersion);
        if (matcher.find()) {
            numberVersion = numberVersion.substring(matcher.start(), matcher.end());
        }
        String system = System.getProperty("os.name");
        if (system.contains(" "))
            system = system.substring(0, system.indexOf(" "));
        if (System.getProperty("java.vm.vendor").contains("Android"))
            system = "Android";
        final String phrase = config.helloPhrase();

        return String.format("Ethereum(J)/v%s/%s/%s/Java/%s", numberVersion, system,
                config.projectVersionModifier().equalsIgnoreCase("release") ? "Release" : "Dev", phrase);
    }
}
