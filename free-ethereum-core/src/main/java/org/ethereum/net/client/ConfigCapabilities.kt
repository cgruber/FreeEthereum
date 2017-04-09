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

package org.ethereum.net.client

import org.ethereum.config.SystemProperties
import org.ethereum.net.client.Capability.Companion.BZZ
import org.ethereum.net.client.Capability.Companion.ETH
import org.ethereum.net.client.Capability.Companion.SHH
import org.ethereum.net.eth.EthVersion
import org.ethereum.net.shh.ShhHandler
import org.ethereum.net.swarm.bzz.BzzHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class ConfigCapabilities @Autowired
constructor(private val config: SystemProperties) {

    private val AllCaps = TreeSet<Capability>()

    init {
        if (config.syncVersion() != null) {
            val eth = EthVersion.fromCode(config.syncVersion()!!)
            if (eth != null) AllCaps.add(Capability(ETH, eth.code))
        } else {
            for (v in EthVersion.supported())
                AllCaps.add(Capability(ETH, v.code))
        }

        AllCaps.add(Capability(SHH, ShhHandler.VERSION))
        AllCaps.add(Capability(BZZ, BzzHandler.VERSION))
    }

    /**
     * Gets the capabilities listed in 'peer.capabilities' config property
     * sorted by their names.
     */
    val configCapabilities: List<Capability>
        get() {
            val ret = ArrayList<Capability>()
            val caps = config.peerCapabilities()
            for (capability in AllCaps) {
                if (caps.contains(capability.name)) {
                    ret.add(capability)
                }
            }
            return ret
        }

}
