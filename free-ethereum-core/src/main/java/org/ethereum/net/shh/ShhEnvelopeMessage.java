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

import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.ethereum.net.shh.ShhMessageCodes.MESSAGE;

/**
 * Created by Anton Nashatyrev on 25.09.2015.
 */
public class ShhEnvelopeMessage extends ShhMessage {

    private final List<WhisperMessage> messages = new ArrayList<>();

    public ShhEnvelopeMessage(final byte[] encoded) {
        super(encoded);
        parse();
    }

    public ShhEnvelopeMessage(final WhisperMessage... msg) {
        Collections.addAll(messages, msg);
        parsed = true;
    }

    public ShhEnvelopeMessage(final Collection<WhisperMessage> msg) {
        messages.addAll(msg);
        parsed = true;
    }

    @Override
    public ShhMessageCodes getCommand() {
        return MESSAGE;
    }

    public void addMessage(final WhisperMessage msg) {
        messages.add(msg);
    }

    private void parse() {
        if (!parsed) {
            final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

            for (final RLPElement aParamsList : paramsList) {
                messages.add(new WhisperMessage(aParamsList.getRLPData()));
            }
            this.parsed = true;
        }
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) {
            final byte[][] encodedMessages = new byte[messages.size()][];
            for (int i = 0; i < encodedMessages.length; i++) {
                encodedMessages[i] = messages.get(i).getEncoded();
            }
            encoded = RLP.encodeList(encodedMessages);
        }
        return encoded;
    }

    public List<WhisperMessage> getMessages() {
        return messages;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public String toString() {
        return "[ENVELOPE " + messages.toString() + "]";
    }
}
