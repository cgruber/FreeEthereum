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

import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.util.ByteUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Mandeleil
 * @since 28.06.2014
 */
public class TestCase {

    //            "pre": { ... },
    private final Map<ByteArrayWrapper, AccountState> pre = new HashMap<>();
    //            "callcreates": { ... }
    private final List<CallCreate> callCreateList = new ArrayList<>();
    private String name = "";
    //            "env": { ... },
    private Env env;
    //
    private Logs logs;
    //            "exec": { ... },
    private Exec exec;
    //            "gas": { ... },
    private byte[] gas;
    //            "out": { ... },
    private byte[] out;
    //            "post": { ... },
    private Map<ByteArrayWrapper, AccountState> post = null;

    public TestCase(final String name, final JSONObject testCaseJSONObj) throws ParseException {

        this(testCaseJSONObj);
        this.name = name;
    }

    private TestCase(final JSONObject testCaseJSONObj) throws ParseException {

        try {

            final JSONObject envJSON = (JSONObject) testCaseJSONObj.get("env");
            final JSONObject execJSON = (JSONObject) testCaseJSONObj.get("exec");
            final JSONObject preJSON = (JSONObject) testCaseJSONObj.get("pre");
            JSONObject postJSON = new JSONObject();
            if (testCaseJSONObj.containsKey("post")) {
                // in cases where there is no post dictionary (when testing for
                // exceptions for example)
                // there are cases with empty post state, they shouldn't be mixed up with no "post" entry
                post = new HashMap<>();
                postJSON = (JSONObject) testCaseJSONObj.get("post");
            }
            JSONArray callCreates = new JSONArray();
            if (testCaseJSONObj.containsKey("callcreates"))
                callCreates = (JSONArray) testCaseJSONObj.get("callcreates");

            JSONArray logsJSON = new JSONArray();
            if (testCaseJSONObj.containsKey("logs"))
                logsJSON = (JSONArray) testCaseJSONObj.get("logs");
            logs = new Logs(logsJSON);

            String gasString = "0";
            if (testCaseJSONObj.containsKey("gas"))
                gasString = testCaseJSONObj.get("gas").toString();
            this.gas = BigIntegers.asUnsignedByteArray(toBigInt(gasString));

            String outString = null;
            if (testCaseJSONObj.containsKey("out"))
                outString = testCaseJSONObj.get("out").toString();
            if (outString != null && outString.length() > 2)
                this.out = Hex.decode(outString.substring(2));
            else
                this.out = ByteUtil.EMPTY_BYTE_ARRAY;

            for (final Object key : preJSON.keySet()) {

                final byte[] keyBytes = Hex.decode(key.toString());
                final AccountState accountState =
                        new AccountState(keyBytes, (JSONObject) preJSON.get(key));

                pre.put(new ByteArrayWrapper(keyBytes), accountState);
            }

            for (final Object key : postJSON.keySet()) {

                final byte[] keyBytes = Hex.decode(key.toString());
                final AccountState accountState =
                        new AccountState(keyBytes, (JSONObject) postJSON.get(key));

                post.put(new ByteArrayWrapper(keyBytes), accountState);
            }

            for (final Object callCreate : callCreates) {

                final CallCreate cc = new CallCreate((JSONObject) callCreate);
                this.callCreateList.add(cc);
            }

            if (testCaseJSONObj.containsKey("env"))
              this.env = new Env(envJSON);

            if (testCaseJSONObj.containsKey("exec"))
              this.exec = new Exec(execJSON);

        } catch (final Throwable e) {
            e.printStackTrace();
            throw new ParseException(0, e);
        }
    }

    static BigInteger toBigInt(final String s) {
        if (s.startsWith("0x")) {
            if (s.equals("0x")) return new BigInteger("0");
            return new BigInteger(s.substring(2), 16);
        } else {
            return new BigInteger(s);
        }
    }

    public Env getEnv() {
        return env;
    }

    public Exec getExec() {
        return exec;
    }

    public Logs getLogs() {
        return logs;
    }

    public byte[] getGas() {
        return gas;
    }

    public byte[] getOut() {
        return out;
    }

    public Map<ByteArrayWrapper, AccountState> getPre() {
        return pre;
    }

    public Map<ByteArrayWrapper, AccountState> getPost() {
        return post;
    }

    public List<CallCreate> getCallCreateList() {
        return callCreateList;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "" + env +
                ", " + exec +
                ", gas=" + Hex.toHexString(gas) +
                ", out=" + Hex.toHexString(out) +
                ", pre=" + pre +
                ", post=" + post +
                ", callcreates=" + callCreateList +
                '}';
    }
}
