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

package org.ethereum.samples;

import com.typesafe.config.ConfigFactory;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.springframework.context.annotation.Bean;

/**
 * This class just extends the BasicSample with the config which connect the peer to the Morden network
 * This class can be used as a base for free transactions testing
 * Everyone may use that 'cow' sender (which is effectively address aacc23ff079d96a5502b31fefcda87a6b3fbdcfb)
 * If you need more coins on this account just go to https://morden.ether.camp/
 * and push 'Get Free Ether' button.
 *
 * Created by Anton Nashatyrev on 10.02.2016.
 */
public class RopstenSample extends BasicSample {
    /**
     * Use that sender key to sign transactions
     */
    private final byte[] senderPrivateKey = HashUtil.INSTANCE.sha3("cow".getBytes());
    // sender address is derived from the private key aacc23ff079d96a5502b31fefcda87a6b3fbdcfb
    protected final byte[] senderAddress = ECKey.fromPrivate(senderPrivateKey).getAddress();

    public static void main(final String[] args) throws Exception {
        sLogger.info("Starting EthereumJ!");

        class SampleConfig extends RopstenSampleConfig {
            @Bean
            public RopstenSample sampleBean() {
                return new RopstenSample();
            }
        }

        final Ethereum ethereum = EthereumFactory.INSTANCE.createEthereum(SampleConfig.class);
    }

    abstract static class RopstenSampleConfig {
        private final String config =
                "peer.discovery = {" +
                "    enabled = true \n" +
                "    ip.list = [" +
                "        '94.242.229.4:40404'," +
                "        '94.242.229.203:30303'" +
                "    ]" +
                "} \n" +
                "peer.p2p.eip8 = true \n" +
                "peer.networkId = 3 \n" +
                "sync.enabled = true \n" +
                "genesis = ropsten.json \n" +
                "blockchain.config.name = 'ropsten' \n" +
                "database.dir = database-ropstenSample";

        public abstract RopstenSample sampleBean();

        @Bean
        public SystemProperties systemProperties() {
            final SystemProperties props = new SystemProperties();
            props.overrideParams(ConfigFactory.parseString(config.replaceAll("'", "\"")));
            return props;
        }
    }
}
