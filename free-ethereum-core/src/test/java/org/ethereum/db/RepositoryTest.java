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

package org.ethereum.db;

import org.ethereum.core.Genesis;
import org.ethereum.core.Repository;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.NoDeleteSource;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.vm.DataWord;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.junit.Assert.*;

/**
 * @author Roman Mandeleil
 * @since 17.11.2014
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RepositoryTest {


    private boolean running = true;

    @Test
    public void test1() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        repository.increaseNonce(cow);
        repository.increaseNonce(horse);

        assertEquals(BigInteger.ONE, repository.getNonce(cow));
//        assertEquals(BigInteger.ONE, repository.getNonce(horse));

        System.out.println(repository.getTrieDump());

        repository.increaseNonce(cow);
        System.out.println(repository.getTrieDump());


        repository.close();
    }

    @Test
    public void test2() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        repository.addBalance(cow, BigInteger.TEN);
        repository.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, repository.getBalance(cow));
        assertEquals(BigInteger.ONE, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test3() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowCode = Hex.decode("A1A2A3");
        final byte[] horseCode = Hex.decode("B1B2B3");

        repository.saveCode(cow, cowCode);
        repository.saveCode(horse, horseCode);

        assertArrayEquals(cowCode, repository.getCode(cow));
        assertArrayEquals(horseCode, repository.getCode(horse));

        repository.close();
    }

    @Test
    public void test4() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowKey = Hex.decode("A1A2A3");
        final byte[] cowValue = Hex.decode("A4A5A6");

        final byte[] horseKey = Hex.decode("B1B2B3");
        final byte[] horseValue = Hex.decode("B4B5B6");

        track.addStorageRow(cow, new DataWord(cowKey), new DataWord(cowValue));
        track.addStorageRow(horse, new DataWord(horseKey), new DataWord(horseValue));
        track.commit();

        assertEquals(new DataWord(cowValue), repository.getStorageValue(cow, new DataWord(cowKey)));
        assertEquals(new DataWord(horseValue), repository.getStorageValue(horse, new DataWord(horseKey)));

        repository.close();
    }

    @Test
    public void test5() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);

        track.increaseNonce(horse);

        track.commit();

        assertEquals(BigInteger.TEN, repository.getNonce(cow));
        assertEquals(BigInteger.ONE, repository.getNonce(horse));

        repository.close();
    }

    @Test
    public void test6() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);
        track.increaseNonce(cow);

        track.increaseNonce(horse);

        assertEquals(BigInteger.TEN, track.getNonce(cow));
        assertEquals(BigInteger.ONE, track.getNonce(horse));

        track.rollback();

        assertEquals(BigInteger.ZERO, repository.getNonce(cow));
        assertEquals(BigInteger.ZERO, repository.getNonce(horse));

        repository.close();
    }

    @Test
    public void test7() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        track.addBalance(cow, BigInteger.TEN);
        track.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, track.getBalance(cow));
        assertEquals(BigInteger.ONE, track.getBalance(horse));

        track.commit();

        assertEquals(BigInteger.TEN, repository.getBalance(cow));
        assertEquals(BigInteger.ONE, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test8() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        track.addBalance(cow, BigInteger.TEN);
        track.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, track.getBalance(cow));
        assertEquals(BigInteger.ONE, track.getBalance(horse));

        track.rollback();

        assertEquals(BigInteger.ZERO, repository.getBalance(cow));
        assertEquals(BigInteger.ZERO, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test7_1() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track1 = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        track1.addBalance(cow, BigInteger.TEN);
        track1.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, track1.getBalance(cow));
        assertEquals(BigInteger.ONE, track1.getBalance(horse));

        final Repository track2 = track1.startTracking();

        assertEquals(BigInteger.TEN, track2.getBalance(cow));
        assertEquals(BigInteger.ONE, track2.getBalance(horse));

        track2.addBalance(cow, BigInteger.TEN);
        track2.addBalance(cow, BigInteger.TEN);
        track2.addBalance(cow, BigInteger.TEN);

        track2.commit();

        track1.commit();

        assertEquals(new BigInteger("40"), repository.getBalance(cow));
        assertEquals(BigInteger.ONE, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test7_2() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track1 = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        track1.addBalance(cow, BigInteger.TEN);
        track1.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, track1.getBalance(cow));
        assertEquals(BigInteger.ONE, track1.getBalance(horse));

        final Repository track2 = track1.startTracking();

        assertEquals(BigInteger.TEN, track2.getBalance(cow));
        assertEquals(BigInteger.ONE, track2.getBalance(horse));

        track2.addBalance(cow, BigInteger.TEN);
        track2.addBalance(cow, BigInteger.TEN);
        track2.addBalance(cow, BigInteger.TEN);

        track2.commit();

        track1.rollback();

        assertEquals(BigInteger.ZERO, repository.getBalance(cow));
        assertEquals(BigInteger.ZERO, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test9() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final DataWord cowKey = new DataWord(Hex.decode("A1A2A3"));
        final DataWord cowValue = new DataWord(Hex.decode("A4A5A6"));

        final DataWord horseKey = new DataWord(Hex.decode("B1B2B3"));
        final DataWord horseValue = new DataWord(Hex.decode("B4B5B6"));

        track.addStorageRow(cow, cowKey, cowValue);
        track.addStorageRow(horse, horseKey, horseValue);

        assertEquals(cowValue, track.getStorageValue(cow, cowKey));
        assertEquals(horseValue, track.getStorageValue(horse, horseKey));

        track.commit();

        assertEquals(cowValue, repository.getStorageValue(cow, cowKey));
        assertEquals(horseValue, repository.getStorageValue(horse, horseKey));

        repository.close();
    }

    @Test
    public void test10() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final DataWord cowKey = new DataWord(Hex.decode("A1A2A3"));
        final DataWord cowValue = new DataWord(Hex.decode("A4A5A6"));

        final DataWord horseKey = new DataWord(Hex.decode("B1B2B3"));
        final DataWord horseValue = new DataWord(Hex.decode("B4B5B6"));

        track.addStorageRow(cow, cowKey, cowValue);
        track.addStorageRow(horse, horseKey, horseValue);

        assertEquals(cowValue, track.getStorageValue(cow, cowKey));
        assertEquals(horseValue, track.getStorageValue(horse, horseKey));

        track.rollback();

        assertEquals(null, repository.getStorageValue(cow, cowKey));
        assertEquals(null, repository.getStorageValue(horse, horseKey));

        repository.close();
    }

    @Test
    public void test11() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowCode = Hex.decode("A1A2A3");
        final byte[] horseCode = Hex.decode("B1B2B3");

        track.saveCode(cow, cowCode);
        track.saveCode(horse, horseCode);

        assertArrayEquals(cowCode, track.getCode(cow));
        assertArrayEquals(horseCode, track.getCode(horse));

        track.commit();

        assertArrayEquals(cowCode, repository.getCode(cow));
        assertArrayEquals(horseCode, repository.getCode(horse));

        repository.close();
    }

    @Test
    public void test12() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowCode = Hex.decode("A1A2A3");
        final byte[] horseCode = Hex.decode("B1B2B3");

        track.saveCode(cow, cowCode);
        track.saveCode(horse, horseCode);

        assertArrayEquals(cowCode, track.getCode(cow));
        assertArrayEquals(horseCode, track.getCode(horse));

        track.rollback();

        assertArrayEquals(EMPTY_BYTE_ARRAY, repository.getCode(cow));
        assertArrayEquals(EMPTY_BYTE_ARRAY, repository.getCode(horse));

        repository.close();
    }

    @Test  // Let's upload genesis pre-mine just like in the real world
    public void test13() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final Genesis genesis = (Genesis) Genesis.getInstance();
        Genesis.populateRepository(track, genesis);

        track.commit();

        assertArrayEquals(Genesis.getInstance().getStateRoot(), repository.getRoot());

        repository.close();
    }

    @Test
    public void test14() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");
        final BigInteger ELEVEN = BigInteger.TEN.add(BigInteger.ONE);


        // changes level_1
        final Repository track1 = repository.startTracking();
        track1.addBalance(cow, BigInteger.TEN);
        track1.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, track1.getBalance(cow));
        assertEquals(BigInteger.ONE, track1.getBalance(horse));


        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addBalance(cow, BigInteger.ONE);
        track2.addBalance(horse, BigInteger.TEN);

        assertEquals(ELEVEN, track2.getBalance(cow));
        assertEquals(ELEVEN, track2.getBalance(horse));

        track2.commit();
        track1.commit();

        assertEquals(ELEVEN, repository.getBalance(cow));
        assertEquals(ELEVEN, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test15() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");
        final BigInteger ELEVEN = BigInteger.TEN.add(BigInteger.ONE);


        // changes level_1
        final Repository track1 = repository.startTracking();
        track1.addBalance(cow, BigInteger.TEN);
        track1.addBalance(horse, BigInteger.ONE);

        assertEquals(BigInteger.TEN, track1.getBalance(cow));
        assertEquals(BigInteger.ONE, track1.getBalance(horse));

        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addBalance(cow, BigInteger.ONE);
        track2.addBalance(horse, BigInteger.TEN);

        assertEquals(ELEVEN, track2.getBalance(cow));
        assertEquals(ELEVEN, track2.getBalance(horse));

        track2.rollback();
        track1.commit();

        assertEquals(BigInteger.TEN, repository.getBalance(cow));
        assertEquals(BigInteger.ONE, repository.getBalance(horse));

        repository.close();
    }

    @Test
    public void test16() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowKey1 = "key-c-1".getBytes();
        final byte[] cowValue1 = "val-c-1".getBytes();

        final byte[] horseKey1 = "key-h-1".getBytes();
        final byte[] horseValue1 = "val-h-1".getBytes();

        final byte[] cowKey2 = "key-c-2".getBytes();
        final byte[] cowValue2 = "val-c-2".getBytes();

        final byte[] horseKey2 = "key-h-2".getBytes();
        final byte[] horseValue2 = "val-h-2".getBytes();

        // changes level_1
        final Repository track1 = repository.startTracking();
        track1.addStorageRow(cow, new DataWord(cowKey1), new DataWord(cowValue1));
        track1.addStorageRow(horse, new DataWord(horseKey1), new DataWord(horseValue1));

        assertEquals(new DataWord(cowValue1), track1.getStorageValue(cow, new DataWord(cowKey1)));
        assertEquals(new DataWord(horseValue1), track1.getStorageValue(horse, new DataWord(horseKey1)));

        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addStorageRow(cow, new DataWord(cowKey2), new DataWord(cowValue2));
        track2.addStorageRow(horse, new DataWord(horseKey2), new DataWord(horseValue2));

        assertEquals(new DataWord(cowValue1), track2.getStorageValue(cow, new DataWord(cowKey1)));
        assertEquals(new DataWord(horseValue1), track2.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), track2.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), track2.getStorageValue(horse, new DataWord(horseKey2)));

        track2.commit();
        // leaving level_2

        assertEquals(new DataWord(cowValue1), track1.getStorageValue(cow, new DataWord(cowKey1)));
        assertEquals(new DataWord(horseValue1), track1.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), track1.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), track1.getStorageValue(horse, new DataWord(horseKey2)));

        track1.commit();
        // leaving level_1

        assertEquals(new DataWord(cowValue1), repository.getStorageValue(cow, new DataWord(cowKey1)));
        assertEquals(new DataWord(horseValue1), repository.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), repository.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), repository.getStorageValue(horse, new DataWord(horseKey2)));

        repository.close();
    }

    @Test
    public void test16_2() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowKey1 = "key-c-1".getBytes();
        final byte[] cowValue1 = "val-c-1".getBytes();

        final byte[] horseKey1 = "key-h-1".getBytes();
        final byte[] horseValue1 = "val-h-1".getBytes();

        final byte[] cowKey2 = "key-c-2".getBytes();
        final byte[] cowValue2 = "val-c-2".getBytes();

        final byte[] horseKey2 = "key-h-2".getBytes();
        final byte[] horseValue2 = "val-h-2".getBytes();

        // changes level_1
        final Repository track1 = repository.startTracking();

        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addStorageRow(cow, new DataWord(cowKey2), new DataWord(cowValue2));
        track2.addStorageRow(horse, new DataWord(horseKey2), new DataWord(horseValue2));

        assertNull(track2.getStorageValue(cow, new DataWord(cowKey1)));
        assertNull(track2.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), track2.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), track2.getStorageValue(horse, new DataWord(horseKey2)));

        track2.commit();
        // leaving level_2

        assertNull(track1.getStorageValue(cow, new DataWord(cowKey1)));
        assertNull(track1.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), track1.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), track1.getStorageValue(horse, new DataWord(horseKey2)));

        track1.commit();
        // leaving level_1

        assertEquals(null, repository.getStorageValue(cow, new DataWord(cowKey1)));
        assertEquals(null, repository.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), repository.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), repository.getStorageValue(horse, new DataWord(horseKey2)));

        repository.close();
    }

    @Test
    public void test16_3() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowKey1 = "key-c-1".getBytes();
        final byte[] cowValue1 = "val-c-1".getBytes();

        final byte[] horseKey1 = "key-h-1".getBytes();
        final byte[] horseValue1 = "val-h-1".getBytes();

        final byte[] cowKey2 = "key-c-2".getBytes();
        final byte[] cowValue2 = "val-c-2".getBytes();

        final byte[] horseKey2 = "key-h-2".getBytes();
        final byte[] horseValue2 = "val-h-2".getBytes();

        // changes level_1
        final Repository track1 = repository.startTracking();

        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addStorageRow(cow, new DataWord(cowKey2), new DataWord(cowValue2));
        track2.addStorageRow(horse, new DataWord(horseKey2), new DataWord(horseValue2));

        assertNull(track2.getStorageValue(cow, new DataWord(cowKey1)));
        assertNull(track2.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), track2.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), track2.getStorageValue(horse, new DataWord(horseKey2)));

        track2.commit();
        // leaving level_2

        assertNull(track1.getStorageValue(cow, new DataWord(cowKey1)));
        assertNull(track1.getStorageValue(horse, new DataWord(horseKey1)));

        assertEquals(new DataWord(cowValue2), track1.getStorageValue(cow, new DataWord(cowKey2)));
        assertEquals(new DataWord(horseValue2), track1.getStorageValue(horse, new DataWord(horseKey2)));

        track1.rollback();
        // leaving level_1

        assertNull(repository.getStorageValue(cow, new DataWord(cowKey1)));
        assertNull(repository.getStorageValue(horse, new DataWord(horseKey1)));

        assertNull(repository.getStorageValue(cow, new DataWord(cowKey2)));
        assertNull(repository.getStorageValue(horse, new DataWord(horseKey2)));

        repository.close();
    }

    @Test
    public void test16_4() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowKey1 = "key-c-1".getBytes();
        final byte[] cowValue1 = "val-c-1".getBytes();

        final byte[] horseKey1 = "key-h-1".getBytes();
        final byte[] horseValue1 = "val-h-1".getBytes();

        final byte[] cowKey2 = "key-c-2".getBytes();
        final byte[] cowValue2 = "val-c-2".getBytes();

        final byte[] horseKey2 = "key-h-2".getBytes();
        final byte[] horseValue2 = "val-h-2".getBytes();

        final Repository track = repository.startTracking();
        track.addStorageRow(cow, new DataWord(cowKey1), new DataWord(cowValue1));
        track.commit();

        // changes level_1
        final Repository track1 = repository.startTracking();

        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addStorageRow(cow, new DataWord(cowKey2), new DataWord(cowValue2));

        track2.commit();
        // leaving level_2

        track1.commit();
        // leaving level_1

        assertEquals(new DataWord(cowValue1), track1.getStorageValue(cow, new DataWord(cowKey1)));
        assertEquals(new DataWord(cowValue2), track1.getStorageValue(cow, new DataWord(cowKey2)));


        repository.close();
    }

    @Test
    public void test16_5() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final byte[] cowKey1 = "key-c-1".getBytes();
        final byte[] cowValue1 = "val-c-1".getBytes();

        final byte[] horseKey1 = "key-h-1".getBytes();
        final byte[] horseValue1 = "val-h-1".getBytes();

        final byte[] cowKey2 = "key-c-2".getBytes();
        final byte[] cowValue2 = "val-c-2".getBytes();

        final byte[] horseKey2 = "key-h-2".getBytes();
        final byte[] horseValue2 = "val-h-2".getBytes();

        // changes level_1
        final Repository track1 = repository.startTracking();
        track1.addStorageRow(cow, new DataWord(cowKey2), new DataWord(cowValue2));

        // changes level_2
        final Repository track2 = track1.startTracking();
        assertEquals(new DataWord(cowValue2), track1.getStorageValue(cow, new DataWord(cowKey2)));
        assertNull(track1.getStorageValue(cow, new DataWord(cowKey1)));

        track2.commit();
        // leaving level_2

        track1.commit();
        // leaving level_1

        assertEquals(new DataWord(cowValue2), track1.getStorageValue(cow, new DataWord(cowKey2)));
        assertNull(track1.getStorageValue(cow, new DataWord(cowKey1)));

        repository.close();
    }

    @Test
    public void test17() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");

        final byte[] cowKey1 = "key-c-1".getBytes();
        final byte[] cowValue1 = "val-c-1".getBytes();

        // changes level_1
        final Repository track1 = repository.startTracking();

        // changes level_2
        final Repository track2 = track1.startTracking();
        track2.addStorageRow(cow, new DataWord(cowKey1), new DataWord(cowValue1));
        assertEquals(new DataWord(cowValue1), track2.getStorageValue(cow, new DataWord(cowKey1)));
        track2.rollback();
        // leaving level_2

        track1.commit();
        // leaving level_1

        Assert.assertEquals(Hex.toHexString(HashUtil.EMPTY_TRIE_HASH), Hex.toHexString(repository.getRoot()));
        repository.close();
    }

    @Test
    public void test18() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository repoTrack2 = repository.startTracking(); //track

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");
        final byte[] pig = Hex.decode("F0B8C9D84DD2B877E0B952130B73E218106FEC04");
        final byte[] precompiled = Hex.decode("0000000000000000000000000000000000000002");

        final byte[] cowCode = Hex.decode("A1A2A3");
        final byte[] horseCode = Hex.decode("B1B2B3");

        repository.saveCode(cow, cowCode);
        repository.saveCode(horse, horseCode);

        repository.delete(horse);

        assertEquals(true, repoTrack2.isExist(cow));
        assertEquals(false, repoTrack2.isExist(horse));
        assertEquals(false, repoTrack2.isExist(pig));
        assertEquals(false, repoTrack2.isExist(precompiled));
    }

    @Test
    public void test19() {

        final RepositoryRoot repository = new RepositoryRoot(new HashMapDB());
        final Repository track = repository.startTracking();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final DataWord cowKey1 = new DataWord("c1");
        final DataWord cowVal1 = new DataWord("c0a1");
        final DataWord cowVal0 = new DataWord("c0a0");

        final DataWord horseKey1 = new DataWord("e1");
        final DataWord horseVal1 = new DataWord("c0a1");
        final DataWord horseVal0 = new DataWord("c0a0");

        track.addStorageRow(cow, cowKey1, cowVal0);
        track.addStorageRow(horse, horseKey1, horseVal0);
        track.commit();

        final Repository track2 = repository.startTracking(); //track

        track2.addStorageRow(horse, horseKey1, horseVal0);
        final Repository track3 = track2.startTracking();

        final ContractDetails cowDetails = track3.getContractDetails(cow);
        cowDetails.put(cowKey1, cowVal1);

        final ContractDetails horseDetails = track3.getContractDetails(horse);
        horseDetails.put(horseKey1, horseVal1);

        track3.commit();
        track2.rollback();

        final ContractDetails cowDetailsOrigin = repository.getContractDetails(cow);
        final DataWord cowValOrin = cowDetailsOrigin.get(cowKey1);

        final ContractDetails horseDetailsOrigin = repository.getContractDetails(horse);
        final DataWord horseValOrin = horseDetailsOrigin.get(horseKey1);

        assertEquals(cowVal0, cowValOrin);
        assertEquals(horseVal0, horseValOrin);
    }

    @Test // testing for snapshot
    public void test20() {

//        MapDB stateDB = new MapDB();
        final Source<byte[], byte[]> stateDB = new NoDeleteSource<>(new HashMapDB<byte[]>());
        final RepositoryRoot repository = new RepositoryRoot(stateDB);
        final byte[] root = repository.getRoot();

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");
        final byte[] horse = Hex.decode("13978AEE95F38490E9769C39B2773ED763D9CD5F");

        final DataWord cowKey1 = new DataWord("c1");
        final DataWord cowKey2 = new DataWord("c2");
        final DataWord cowVal1 = new DataWord("c0a1");
        final DataWord cowVal0 = new DataWord("c0a0");

        final DataWord horseKey1 = new DataWord("e1");
        final DataWord horseKey2 = new DataWord("e2");
        final DataWord horseVal1 = new DataWord("c0a1");
        final DataWord horseVal0 = new DataWord("c0a0");

        Repository track2 = repository.startTracking(); //track
        track2.addStorageRow(cow, cowKey1, cowVal1);
        track2.addStorageRow(horse, horseKey1, horseVal1);
        track2.commit();
        repository.commit();

        final byte[] root2 = repository.getRoot();

        track2 = repository.startTracking(); //track
        track2.addStorageRow(cow, cowKey2, cowVal0);
        track2.addStorageRow(horse, horseKey2, horseVal0);
        track2.commit();
        repository.commit();

        final byte[] root3 = repository.getRoot();

        Repository snapshot = new RepositoryRoot(stateDB, root);
        ContractDetails cowDetails = snapshot.getContractDetails(cow);
        ContractDetails horseDetails = snapshot.getContractDetails(horse);
        assertEquals(null, cowDetails.get(cowKey1) );
        assertEquals(null, cowDetails.get(cowKey2) );
        assertEquals(null, horseDetails.get(horseKey1) );
        assertEquals(null, horseDetails.get(horseKey2) );


        snapshot = new RepositoryRoot(stateDB, root2);
        cowDetails = snapshot.getContractDetails(cow);
        horseDetails = snapshot.getContractDetails(horse);
        assertEquals(cowVal1, cowDetails.get(cowKey1));
        assertEquals(null, cowDetails.get(cowKey2));
        assertEquals(horseVal1, horseDetails.get(horseKey1) );
        assertEquals(null, horseDetails.get(horseKey2) );

        snapshot = new RepositoryRoot(stateDB, root3);
        cowDetails = snapshot.getContractDetails(cow);
        horseDetails = snapshot.getContractDetails(horse);
        assertEquals(cowVal1, cowDetails.get(cowKey1));
        assertEquals(cowVal0, cowDetails.get(cowKey2));
        assertEquals(horseVal1, horseDetails.get(horseKey1) );
        assertEquals(horseVal0, horseDetails.get(horseKey2) );
    }

    @Test // testing for snapshot
    public void testMultiThread() throws InterruptedException {
        // Add logging line to {@link org.ethereum.datasource.WriteCache} in the beginning of flushImpl() method:
        //    System.out.printf("Flush start: %s%n", this);
        // to increase chance of failing. Also increasing waiting time may be helpful.
        final RepositoryImpl repository = new RepositoryRoot(new HashMapDB());

        final byte[] cow = Hex.decode("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826");

        final DataWord cowKey1 = new DataWord("c1");
        final DataWord cowKey2 = new DataWord("c2");
        final DataWord cowVal0 = new DataWord("c0a0");

        final Repository track2 = repository.startTracking(); //track
        track2.addStorageRow(cow, cowKey2, cowVal0);
        track2.commit();
        repository.flush();

        final ContractDetails cowDetails = repository.getContractDetails(cow);
        assertEquals(cowVal0, cowDetails.get(cowKey2));

        final CountDownLatch failSema = new CountDownLatch(1);

        for (int i = 0; i < 10; ++i) {
            new Thread(() -> {
                try {
                    int cnt = 1;
                    while (running) {
                        final Repository snap = repository.getSnapshotTo(repository.getRoot()).startTracking();
                        snap.addBalance(cow, BigInteger.TEN);
                        snap.addStorageRow(cow, cowKey1, new DataWord(cnt));
                        snap.rollback();
                        cnt++;
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                    failSema.countDown();
                }
            }).start();
        }

        new Thread(() -> {
            int cnt = 1;
            try {
                while (running) {
                    final Repository track21 = repository.startTracking(); //track
                    final DataWord cVal = new DataWord(cnt);
                    track21.addStorageRow(cow, cowKey1, cVal);
                    track21.addBalance(cow, BigInteger.ONE);
                    track21.commit();

                    repository.flush();

                    assertEquals(BigInteger.valueOf(cnt), repository.getBalance(cow));
                    assertEquals(cVal, repository.getStorageValue(cow, cowKey1));
                    assertEquals(cowVal0, repository.getStorageValue(cow, cowKey2));
                    cnt++;
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                try {
                    repository.addStorageRow(cow, cowKey1, new DataWord(123));
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
                failSema.countDown();
            }
        }).start();

        failSema.await(10, TimeUnit.SECONDS);
        running = false;

        if (failSema.getCount() == 0) {
            throw new RuntimeException("Test failed.");
        }
    }
}
