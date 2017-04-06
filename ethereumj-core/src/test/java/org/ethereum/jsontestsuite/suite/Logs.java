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

package org.ethereum.jsontestsuite.suite;

import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Logs {
    private final List<LogInfo> logs = new ArrayList<>();

    public Logs(final JSONArray jLogs) {

        for (final Object jLog1 : jLogs) {

            final JSONObject jLog = (JSONObject) jLog1;
            final byte[] address = Hex.decode((String) jLog.get("address"));
            final byte[] data = Hex.decode(((String) jLog.get("data")).substring(2));

            final List<DataWord> topics = new ArrayList<>();

            final JSONArray jTopics = (JSONArray) jLog.get("topics");
            for (final Object t : jTopics.toArray()) {
                final byte[] topic = Hex.decode(((String) t));
                topics.add(new DataWord(topic));
            }

            final LogInfo li = new LogInfo(address, topics, data);
            logs.add(li);
        }
    }


    public Iterator<LogInfo> getIterator() {
        return logs.iterator();
    }


    public List<String> compareToReal(final List<LogInfo> logs) {

        final List<String> results = new ArrayList<>();

        int i = 0;
        for (final LogInfo postLog : this.logs) {

            final LogInfo realLog = logs.get(i);

            final String postAddress = Hex.toHexString(postLog.getAddress());
            final String realAddress = Hex.toHexString(realLog.getAddress());

            if (!postAddress.equals(realAddress)) {

                final String formattedString = String.format("Log: %s: has unexpected address, expected address: %s found address: %s",
                        i, postAddress, realAddress);
                results.add(formattedString);
            }

            final String postData = Hex.toHexString(postLog.getData());
            final String realData = Hex.toHexString(realLog.getData());

            if (!postData.equals(realData)) {

                final String formattedString = String.format("Log: %s: has unexpected data, expected data: %s found data: %s",
                        i, postData, realData);
                results.add(formattedString);
            }

            final String postBloom = Hex.toHexString(postLog.getBloom().getData());
            final String realBloom = Hex.toHexString(realLog.getBloom().getData());

            if (!postData.equals(realData)) {

                final String formattedString = String.format("Log: %s: has unexpected bloom, expected bloom: %s found bloom: %s",
                        i, postBloom, realBloom);
                results.add(formattedString);
            }

            final List<DataWord> postTopics = postLog.getTopics();
            final List<DataWord> realTopics = realLog.getTopics();

            int j = 0;
            for (final DataWord postTopic : postTopics) {

                final DataWord realTopic = realTopics.get(j);

                if (!postTopic.equals(realTopic)) {

                    final String formattedString = String.format("Log: %s: has unexpected topic: %s, expected topic: %s found topic: %s",
                            i, j, postTopic, realTopic);
                    results.add(formattedString);
                }
                ++j;
            }

            ++i;
        }

        return results;
    }

}
