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

    private List<WhisperMessage> messages = new ArrayList<>();

    public ShhEnvelopeMessage(byte[] encoded) {
        super(encoded);
        parse();
    }

    public ShhEnvelopeMessage(WhisperMessage ... msg) {
        Collections.addAll(messages, msg);
        parsed = true;
    }

    public ShhEnvelopeMessage(Collection<WhisperMessage> msg) {
        messages.addAll(msg);
        parsed = true;
    }

    @Override
    public ShhMessageCodes getCommand() {
        return MESSAGE;
    }

    public void addMessage(WhisperMessage msg) {
        messages.add(msg);
    }

    public void parse() {
        if (!parsed) {
            RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

            for (RLPElement aParamsList : paramsList) {
                messages.add(new WhisperMessage(aParamsList.getRLPData()));
            }
            this.parsed = true;
        }
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) {
            byte[][] encodedMessages = new byte[messages.size()][];
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
