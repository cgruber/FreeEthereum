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

package org.ethereum.net.client;

import org.ethereum.config.SystemProperties;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.shh.ShhHandler;
import org.ethereum.net.swarm.bzz.BzzHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.ethereum.net.client.Capability.*;

@Component
public class ConfigCapabilities {

    private final SystemProperties config;

    private final SortedSet<Capability> AllCaps = new TreeSet<>();

    @Autowired
    public ConfigCapabilities(final SystemProperties config) {
        this.config = config;
        if (config.syncVersion() != null) {
            final EthVersion eth = EthVersion.Companion.fromCode(config.syncVersion());
            if (eth != null) AllCaps.add(new Capability(ETH, eth.getCode()));
        } else {
            for (final EthVersion v : EthVersion.Companion.supported())
                AllCaps.add(new Capability(ETH, v.getCode()));
        }

        AllCaps.add(new Capability(SHH, ShhHandler.VERSION));
        AllCaps.add(new Capability(BZZ, BzzHandler.VERSION));
    }

    /**
     * Gets the capabilities listed in 'peer.capabilities' config property
     * sorted by their names.
     */
    public List<Capability> getConfigCapabilities() {
        final List<Capability> ret = new ArrayList<>();
        final List<String> caps = config.peerCapabilities();
        for (final Capability capability : AllCaps) {
            if (caps.contains(capability.getName())) {
                ret.add(capability);
            }
        }
        return ret;
    }

}
