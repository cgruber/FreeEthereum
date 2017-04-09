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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Whisper implementation which works through JSON RPC API
 *
 * Created by Anton Nashatyrev on 05.10.2015.
 */
public class JsonRpcWhisper extends Whisper {
    private final static Logger logger = LoggerFactory.getLogger("net.shh");

    private final URL rpcUrl;
    private final Map<Integer, MessageWatcher> watchers = new HashMap<>();

    public JsonRpcWhisper(final URL rpcUrl) {
        this.rpcUrl = rpcUrl;

        final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
        poller.scheduleAtFixedRate(() -> {
            try {
                pollFilters();
            } catch (final Exception e) {
                logger.error("Unhandled exception", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private static String add0X(final String s) {
        if (s == null) return null;
        return s.startsWith("0x") ? s : "0x" + s;
    }

    private static String del0X(final String s) {
        if (s == null) return null;
        return s.startsWith("0x") ? s.substring(2) : s;
    }

    private static String encodeString(final String s) {
        return s == null ? null : "0x" + Hex.toHexString(s.getBytes());
    }

    private static String decodeString(String s) {
        if (s.startsWith("0x")) s = s.substring(2);
        return new String(Hex.decode(s));
    }

    public static void main(final String[] args) throws Exception {
        final String json = "{\"jsonrpc\":\"2.0\",\n" +
                " \n" +
                " \"method\":\"shh_newIdentity\",\n" +
                " \"params\": [{  \"payload\": \"Hello\",  \"ttl\": \"100\", \"to\" : \"0xbd27a63c91fe3233c5777e6d3d7b39204d398c8f92655947eb5a373d46e1688f022a1632d264725cbc7dc43ee1cfebde42fa0a86d08b55d2acfbb5e9b3b48dc5\", \"from\": \"id1\" }],\n" +
                " \"id\":1001\n" +
                "}";
        final JsonRpcWhisper rpcWhisper = new JsonRpcWhisper(new URL("http://localhost:8545"));
//        JsonRpcResponse resp = rpcWhisper.sendJson(new JsonRpcRequest("shh_post",
//                new PostParams("Hello").to("0xbd27a63c91fe3233c5777e6d3d7b39204d398c8f92655947eb5a373d46e1688f022a1632d264725cbc7dc43ee1cfebde42fa0a86d08b55d2acfbb5e9b3b48dc5")));
//        Hex.decode("7d04a8170c432240dcf544e27610cc3a10a32c6a5f8ff8cf5a06d26ee0d37da4075701ff03cee88d50885ff56bcd9a5070ff98b9a3045d6ff32e0f1821c21f87")
        rpcWhisper.send(null, null, "Hello C++ Whisper".getBytes(), Topic.createTopics("ATopic"), 60, 1);
        rpcWhisper.watch(new MessageWatcher(null,
                null, Topic.createTopics("ATopic")) {
            @Override
            protected void newMessage(final WhisperMessage msg) {
                System.out.println("JsonRpcWhisper.newMessage:" + "msg = [" + msg + "]");
            }
        });

        Thread.sleep(1000000000);
//        String resp = rpcWhisper.sendPost(json);
//        System.out.println("Resp: " + resp);

    }

    @Override
    public String addIdentity(final ECKey key) {
        throw new RuntimeException("Not supported by public JSON RPC API");
    }

    @Override
    public String newIdentity() {
        final SimpleResponse resp = sendJson(new JsonRpcRequest("shh_newIdentity", null), SimpleResponse.class);
        return del0X(resp.result);
    }

    @Override
    public void watch(final MessageWatcher f) {
        final String[] topics = f.getTopics().length == 0 ? null : new String[f.getTopics().length];
        for (int i = 0; i < f.getTopics().length; i++) {
            topics[i] = f.getTopics()[i].getOriginalTopic();
        }
        final FilterParams params = new FilterParams(add0X(f.getTo()), topics);
        final SimpleResponse resp = sendJson(new JsonRpcRequest("shh_newFilter", params), SimpleResponse.class);
        final int filterId = Integer.parseInt(del0X(resp.result), 16);
        watchers.put(filterId, f);
    }

    @Override
    public void unwatch(final MessageWatcher f) {
        int filterId = -1;
        for (final Map.Entry<Integer, MessageWatcher> entry : watchers.entrySet()) {
            if (entry.getValue() == f) {
                filterId = entry.getKey();
                break;
            }
        }
        if (filterId == -1) return;
        sendJson(new JsonRpcRequest("shh_uninstallFilter",
                add0X(Integer.toHexString(filterId))), SimpleResponse.class);
    }

    private String fromAddress(String s) {
        if (s == null) return null;
        s = del0X(s);
        final BigInteger i = new BigInteger(s, 16);
        if (i.bitCount() > 0) {
            return s;
        }
        return null;
    }

    private void pollFilters() {
        for (final Map.Entry<Integer, MessageWatcher> entry : watchers.entrySet()) {
            final MessagesResponse ret = sendJson(new JsonRpcRequest("shh_getFilterChanges",
                    add0X(Integer.toHexString(entry.getKey()))), MessagesResponse.class);
            for (final MessageParams msg : ret.result) {
                final Topic[] topics = msg.topics == null ? null : new Topic[msg.topics.length];
                for (int i = 0; topics != null && i < topics.length; i++) {
                    topics[i] = new Topic(Hex.decode(del0X(msg.topics[i])));
                }
                final WhisperMessage m = new WhisperMessage()
                        .setPayload(decodeString(msg.payload))
                        .setFrom(fromAddress(msg.from))
                        .setTo(fromAddress(msg.to))
                        .setTopics(topics);
                entry.getValue().newMessage(m);
            }
        }
    }

    @Override
    public void send(final String from, final String to, final byte[] payload, final Topic[] topics, final int ttl, final int workToProve) {
        final String[] topicsS = new String[topics.length];
        for (int i = 0; i < topics.length; i++) {
            topicsS[i] = topics[i].getOriginalTopic();
        }
        final SimpleResponse res = sendJson(new JsonRpcRequest("shh_post",
                new MessageParams(new String(payload), add0X(to), add0X(from),
                        topicsS, ttl, workToProve)), SimpleResponse.class);
        if (!"true".equals(res.result)) {
            throw new RuntimeException("Shh post failed: " + res);
        }
    }

    private <RespType extends JsonRpcResponse> RespType sendJson(final JsonRpcRequest req, final Class<RespType> respClazz) {
        String out = null, in = null;
        try {
            final ObjectMapper om = new ObjectMapper();
            out = om.writeValueAsString(req);
            logger.debug("JSON RPC Outbound: " + out);
            in = sendPost(out);
            logger.debug("JSON RPC Inbound: " + in);
            final RespType resp = om.readValue(in, respClazz);
            resp.throwIfError();
            return resp;
        } catch (final IOException e) {
            throw new RuntimeException("Error processing JSON (Sent: " + out + ", Received: " + in + ")", e);
        }
    }

    private String sendPost(final String urlParams) {

        try {
            final HttpURLConnection con = (HttpURLConnection) rpcUrl.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");

            // Send post request
            con.setDoOutput(true);
            final DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();

            final int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP Response: " + responseCode);
            }
            final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            final StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (final IOException e) {
            throw new RuntimeException("Error sending POST to " + rpcUrl, e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonRpcRequest<ParamT> {
        private static final AtomicInteger idCount = new AtomicInteger(0);

        public final String jsonrpc = "2.0";
        public final String method;
        public final List<ParamT> params;
        public int id = idCount.incrementAndGet();

        public JsonRpcRequest(final String method, final ParamT params) {
            this.method = method;
            this.params = params == null ? null : Collections.singletonList(params);
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageParams {
        public String payload;
        public String to;
        public String from;
        public String[] topics;
        public String ttl = "0x60";
        public Integer priority;

        // response fields
        public String hash;
        public String expiry;
        public String sent;
        public String workProved;

        public MessageParams() {
        }

        public MessageParams(final String payload, final String to, final String from, final String[] topics, final Integer ttl, final Integer priority) {
            this.payload = encodeString(payload);
            this.to = to;
            this.from = from;
            this.topics = topics;
            for (int i = 0; i < this.topics.length; i++) {
                this.topics[i] = encodeString(this.topics[i]);
            }
            this.ttl = "0x" + Integer.toHexString(ttl);
            this.priority = priority;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FilterParams {
        public final String to;
        public final String[] topics;

        public FilterParams(final String to, final String[] topics) {
            this.to = to;
            this.topics = topics;
            for (int i = 0; topics != null && i < this.topics.length; i++) {
                this.topics[i] = encodeString(this.topics[i]);
            }
        }
    }

    public static class JsonRpcResponse {
        public int id;
        public String jsonrpc;
        public Error error;

        public void throwIfError() {
            if (error != null) {
                throw new RuntimeException("JSON RPC returned error (" + error.code + "): " + error.message);
            }
        }

        public static class Error {
            public int code;
            public String message;
        }
    }

    public static class SimpleResponse extends JsonRpcResponse {
        public String result;

        @Override
        public String toString() {
            return "JsonRpcResponse{" +
                    "id=" + id +
                    ", jsonrpc='" + jsonrpc + '\'' +
                    ", result='" + result + '\'' +
                    '}';
        }
    }

    public static class MessagesResponse extends JsonRpcResponse {
        public List<MessageParams> result;

        @Override
        public String toString() {
            return "MessagesResponse{" +
                    "id=" + id +
                    ", jsonrpc='" + jsonrpc + '\'' +
                    "result=" + result +
                    '}';
        }
    }
}
