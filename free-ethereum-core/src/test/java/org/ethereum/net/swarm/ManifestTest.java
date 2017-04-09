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

package org.ethereum.net.swarm;

import org.junit.Test;

/**
 * Created by Admin on 11.06.2015.
 */
public class ManifestTest {

    private static final String testManifest = "{\"entries\":[\n" +
            "  {\"path\":\"a/b\"},\n" +
            "  {\"path\":\"a\"},\n" +
            "  {\"path\":\"a/bb\"},\n" +
            "  {\"path\":\"a/bd\"},\n" +
            "  {\"path\":\"a/bb/c\"}\n" +
            "]}";

    private static final DPA dpa = new SimpleDPA();


    @Test
    public void simpleTest() {
        final Manifest mf = new Manifest(dpa);
        mf.add(new Manifest.ManifestEntry("a", "hash1", "image/jpeg", Manifest.Status.OK));
        mf.add(new Manifest.ManifestEntry("ab", "hash2", "image/jpeg", Manifest.Status.OK));
        System.out.println(mf.dump());
        final String hash = mf.save();
        System.out.println("Hash: " + hash);
        System.out.println(dpa);

        final Manifest mf1 = Manifest.loadManifest(dpa, hash);
        System.out.println(mf1.dump());

        final Manifest.ManifestEntry ab = mf1.get("ab");
        System.out.println(ab);
        final Manifest.ManifestEntry a = mf1.get("a");
        System.out.println(a);

        System.out.println(mf1.dump());
    }

    @Test
    public void readWriteReadTest() throws Exception {
        final String testManiHash = dpa.store(Util.stringToReader(testManifest)).getHexString();
        final Manifest m = Manifest.loadManifest(dpa, testManiHash);
        System.out.println(m.dump());

        final String nHash = m.save();

        final Manifest m1 = Manifest.loadManifest(dpa, nHash);
    }
}
