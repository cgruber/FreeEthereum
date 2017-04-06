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


import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RLPTestCase {
    private static Logger logger = LoggerFactory.getLogger("rlp");
    private final List<String> computed = new ArrayList<>();
    private final List<String> expected = new ArrayList<>();
    private Object in;
    private String out;

    public Object getIn() {
        return in;
    }

    public void setIn(final Object in) {
        this.in = in;
    }

    public String getOut() {
        return out;
    }

    public void setOut(final String out) {
        this.out = out;
    }

    public List<String> getComputed() {
        return computed;
    }

    public List<String> getExpected() {
        return expected;
    }

    public void doEncode() {
        final byte[] in = buildRLP(this.in);
        final String expected = this.out.toLowerCase();
        final String computed = Hex.toHexString(in);
        this.computed.add(computed);
        this.expected.add(expected);
    }

    public void doDecode() {
        final String out = this.out.toLowerCase();
        final RLPList list = RLP.decode2(Hex.decode(out));
        checkRLPAgainstJson(list.get(0), in);
    }

    private byte[] buildRLP(final Object in) {
        if (in instanceof ArrayList) {
            final List<byte[]> elementList = new Vector<>();
            for (final Object o : ((ArrayList) in).toArray()) {
                elementList.add(buildRLP(o));
            }
            final byte[][] elements = elementList.toArray(new byte[elementList.size()][]);
            return RLP.encodeList(elements);
        } else {
            if (in instanceof String) {
                final String s = in.toString();
                if (s.contains("#")) {
                    return RLP.encode(new BigInteger(s.substring(1)));
                }
            } else if (in instanceof Integer) {
                return RLP.encodeInt(Integer.parseInt(in.toString()));
            }
            return RLP.encode(in);
        }
    }

    private void checkRLPAgainstJson(final RLPElement element, final Object in) {
        if (in instanceof List) {
            final Object[] array = ((List) in).toArray();
            final RLPList list = (RLPList) element;
            for (int i = 0; i < array.length; i++) {
                checkRLPAgainstJson(list.get(i), array[i]);
            }
        } else if (in instanceof Number) {
            final int computed = ByteUtil.byteArrayToInt(element.getRLPData());
            this.computed.add(Integer.toString(computed));
            this.expected.add(in.toString());
        } else if (in instanceof String) {
            String s = in.toString();
            if (s.contains("#")) {
                s = s.substring(1);
                final BigInteger expected = new BigInteger(s);
                final byte[] payload = element.getRLPData();
                final BigInteger computed = new BigInteger(1, payload);
                this.computed.add(computed.toString());
                this.expected.add(expected.toString());
            } else {
                final String expected = new String(element.getRLPData() != null ? element.getRLPData() :
                        new byte[0], StandardCharsets.UTF_8);
                this.expected.add(expected);
                this.computed.add(s);
            }
        } else {
            throw new RuntimeException("Unexpected type: " + in.getClass());
        }
    }
}
