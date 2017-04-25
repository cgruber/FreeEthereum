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

package org.ethereum.trie

import org.ethereum.core.AccountState
import org.ethereum.crypto.HashUtil
import org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH
import org.ethereum.crypto.HashUtil.sha3
import org.ethereum.datasource.*
import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.datasource.inmem.HashMapDBSimple
import org.ethereum.util.ByteUtil.intToBytes
import org.ethereum.util.Value
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

class TrieTest {
    private val randomDictionary = "spinneries, archipenko, prepotency, herniotomy, preexpress, relaxative, insolvably, debonnaire, apophysate, virtuality, cavalryman, utilizable, diagenesis, vitascopic, governessy, abranchial, cyanogenic, gratulated, signalment, predicable, subquality, crystalize, prosaicism, oenologist, repressive, impanelled, cockneyism, bordelaise, compigne, konstantin, predicated, unsublimed, hydrophane, phycomyces, capitalise, slippingly, untithable, unburnable, deoxidizer, misteacher, precorrect, disclaimer, solidified, neuraxitis, caravaning, betelgeuse, underprice, uninclosed, acrogynous, reirrigate, dazzlingly, chaffiness, corybantes, intumesced, intentness, superexert, abstrusely, astounding, pilgrimage, posttarsal, prayerless, nomologist, semibelted, frithstool, unstinging, ecalcarate, amputating, megascopic, graphalloy, platteland, adjacently, mingrelian, valentinus, appendical, unaccurate, coriaceous, waterworks, sympathize, doorkeeper, overguilty, flaggingly, admonitory, aeriferous, normocytic, parnellism, catafalque, odontiasis, apprentice, adulterous, mechanisma, wilderness, undivorced, reinterred, effleurage, pretrochal, phytogenic, swirlingly, herbarized, unresolved, classifier, diosmosing, microphage, consecrate, astarboard, predefying, predriving, lettergram, ungranular, overdozing, conferring, unfavorite, peacockish, coinciding, erythraeum, freeholder, zygophoric, imbitterer, centroidal, appendixes, grayfishes, enological, indiscreet, broadcloth, divulgated, anglophobe, stoopingly, bibliophil, laryngitis, separatist, estivating, bellarmine, greasiness, typhlology, xanthation, mortifying, endeavorer, aviatrices, unequalise, metastatic, leftwinger, apologizer, quatrefoil, nonfouling, bitartrate, outchiding, undeported, poussetted, haemolysis, asantehene, montgomery, unjoinable, cedarhurst, unfastener, nonvacuums, beauregard, animalized, polyphides, cannizzaro, gelatinoid, apologised, unscripted, tracheidal, subdiscoid, gravelling, variegated, interabang, inoperable, immortelle, laestrygon, duplicatus, proscience, deoxidised, manfulness, channelize, nondefense, ectomorphy, unimpelled, headwaiter, hexaemeric, derivation, prelexical, limitarian, nonionized, prorefugee, invariably, patronizer, paraplegia, redivision, occupative, unfaceable, hypomnesia, psalterium, doctorfish, gentlefolk, overrefine, heptastich, desirously, clarabelle, uneuphonic, autotelism, firewarden, timberjack, fumigation, drainpipes, spathulate, novelvelle, bicorporal, grisliness, unhesitant, supergiant, unpatented, womanpower, toastiness, multichord, paramnesia, undertrick, contrarily, neurogenic, gunmanship, settlement, brookville, gradualism, unossified, villanovan, ecospecies, organising, buckhannon, prefulfill, johnsonese, unforegone, unwrathful, dunderhead, erceldoune, unwadeable, refunction, understuff, swaggering, freckliest, telemachus, groundsill, outslidden, bolsheviks, recognizer, hemangioma, tarantella, muhammedan, talebearer, relocation, preemption, chachalaca, septuagint, ubiquitous, plexiglass, humoresque, biliverdin, tetraploid, capitoline, summerwood, undilating, undetested, meningitic, petrolatum, phytotoxic, adiphenine, flashlight, protectory, inwreathed, rawishness, tendrillar, hastefully, bananaquit, anarthrous, unbedimmed, herborized, decenniums, deprecated, karyotypic, squalidity, pomiferous, petroglyph, actinomere, peninsular, trigonally, androgenic, resistance, unassuming, frithstool, documental, eunuchised, interphone, thymbraeus, confirmand, expurgated, vegetation, myographic, plasmagene, spindrying, unlackeyed, foreknower, mythically, albescence, rebudgeted, implicitly, unmonastic, torricelli, mortarless, labialized, phenacaine, radiometry, sluggishly, understood, wiretapper, jacobitely, unbetrayed, stadholder, directress, emissaries, corelation, sensualize, uncurbable, permillage, tentacular, thriftless, demoralize, preimagine, iconoclast, acrobatism, firewarden, transpired, bluethroat, wanderjahr, groundable, pedestrian, unulcerous, preearthly, freelanced, sculleries, avengingly, visigothic, preharmony, bressummer, acceptable, unfoolable, predivider, overseeing, arcosolium, piriformis, needlecord, homebodies, sulphation, phantasmic, unsensible, unpackaged, isopiestic, cytophagic, butterlike, frizzliest, winklehawk, necrophile, mesothorax, cuchulainn, unrentable, untangible, unshifting, unfeasible, poetastric, extermined, gaillardia, nonpendent, harborside, pigsticker, infanthood, underrower, easterling, jockeyship, housebreak, horologium, undepicted, dysacousma, incurrable, editorship, unrelented, peritricha, interchaff, frothiness, underplant, proafrican, squareness, enigmatise, reconciled, nonnumeral, nonevident, hamantasch, victualing, watercolor, schrdinger, understand, butlerlike, hemiglobin, yankeeland"
    //    public TrieCache mockDb = new TrieCache();
    //    public TrieCache mockDb_2 = new TrieCache();
    private val mockDb = NoDoubleDeleteMapDB()
    private val mockDb_2 = NoDoubleDeleteMapDB()

    //      ROOT: [ '\x16', A ]
    //      A: [ '', '', '', '', B, '', '', '', C, '', '', '', '', '', '', '', '' ]
    //      B: [ '\x00\x6f', D ]
    //      D: [ '', '', '', '', '', '', E, '', '', '', '', '', '', '', '', '', 'verb' ]
    //      E: [ '\x17', F ]
    //      F: [ '', '', '', '', '', '', G, '', '', '', '', '', '', '', '', '', 'puppy' ]
    //      G: [ '\x35', 'coin' ]
    //      C: [ '\x20\x6f\x72\x73\x65', 'stallion' ]

    @After
    @Throws(IOException::class)
    fun closeMockDb() {
    }

    @Test
    fun testEmptyKey() {
        val trie = StringTrie(mockDb, null)

        trie.put("", dog)
        assertEquals(dog, trie[""])
    }

    @Test
    fun testInsertShortString() {
        val trie = StringTrie(mockDb)

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])
    }

    @Test
    fun testInsertLongString() {
        val trie = StringTrie(mockDb)

        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])
    }

    @Test
    fun testInsertMultipleItems1() {
        val trie = StringTrie(mockDb)
        trie.put(ca, dude)
        assertEquals(dude, trie[ca])

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])

        trie.put(dog, test)
        assertEquals(test, trie[dog])

        trie.put(doge, LONG_STRING)
        assertEquals(LONG_STRING, trie[doge])

        trie.put(test, LONG_STRING)
        assertEquals(LONG_STRING, trie[test])

        // Test if everything is still there
        assertEquals(dude, trie[ca])
        assertEquals(dog, trie[cat])
        assertEquals(test, trie[dog])
        assertEquals(LONG_STRING, trie[doge])
        assertEquals(LONG_STRING, trie[test])
    }

    @Test
    fun testInsertMultipleItems2() {
        val trie = StringTrie(mockDb)

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])

        println(trie.trieDump)

        trie.put(ca, dude)
        assertEquals(dude, trie[ca])

        println(trie.trieDump)

        trie.put(doge, LONG_STRING)
        assertEquals(LONG_STRING, trie[doge])

        println(trie.trieDump)

        trie.put(dog, test)
        assertEquals(test, trie[dog])

        trie.put(test, LONG_STRING)
        assertEquals(LONG_STRING, trie[test])

        // Test if everything is still there
        assertEquals(dog, trie[cat])
        assertEquals(dude, trie[ca])
        assertEquals(LONG_STRING, trie[doge])
        assertEquals(test, trie[dog])
        assertEquals(LONG_STRING, trie[test])

        println(trie.trieDump)

        val trieNew = TrieImpl(mockDb.db, trie.rootHash)
        assertEquals(dog, String(trieNew[cat.toByteArray()]))
        assertEquals(dude, String(trieNew[ca.toByteArray()]))
        assertEquals(LONG_STRING, String(trieNew[doge.toByteArray()]))
        assertEquals(test, String(trieNew[dog.toByteArray()]))
        assertEquals(LONG_STRING, String(trieNew[test.toByteArray()]))
    }

    @Test
    fun newImplTest() {
        val db = HashMapDBSimple<ByteArray>()
        val btrie = TrieImpl(db, null)
        val trie = SourceCodec(btrie, STR_SERIALIZER, STR_SERIALIZER)

        trie.put("cat", "dog")
        trie.put("ca", "dude")

        assertEquals(trie["cat"], "dog")
        assertEquals(trie["ca"], "dude")

        trie.put(doge, LONG_STRING)

        println(btrie.dumpStructure())
        println(btrie.dumpTrie())

        assertEquals(LONG_STRING, trie[doge])
        assertEquals(dog, trie[cat])


        trie.put(dog, test)
        assertEquals(test, trie[dog])
        assertEquals(dog, trie[cat])

        trie.put(test, LONG_STRING)
        assertEquals(LONG_STRING, trie[test])

        println(btrie.dumpStructure())
        println(btrie.dumpTrie())

        assertEquals(dog, trie[cat])
        assertEquals(dude, trie[ca])
        assertEquals(LONG_STRING, trie[doge])
        assertEquals(test, trie[dog])
        assertEquals(LONG_STRING, trie[test])
    }

    @Test
    fun testUpdateShortToShortString() {
        val trie = StringTrie(mockDb)

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])

        trie.put(cat, dog + "1")
        assertEquals(dog + "1", trie[cat])
    }

    @Test
    fun testUpdateLongToLongString() {
        val trie = StringTrie(mockDb)
        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])
        trie.put(cat, LONG_STRING + "1")
        assertEquals(LONG_STRING + "1", trie[cat])
    }

    @Test
    fun testUpdateShortToLongString() {
        val trie = StringTrie(mockDb)

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])

        trie.put(cat, LONG_STRING + "1")
        assertEquals(LONG_STRING + "1", trie[cat])
    }

    @Test
    fun testUpdateLongToShortString() {
        val trie = StringTrie(mockDb)

        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])

        trie.put(cat, dog + "1")
        assertEquals(dog + "1", trie[cat])
    }

    @Test
    fun testDeleteShortString1() {
        val ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee"
        val ROOT_HASH_AFTER = "fc5120b4a711bca1f5bb54769525b11b3fb9a8d6ac0b8bf08cbb248770521758"
        val trie = StringTrie(mockDb)

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])

        trie.put(ca, dude)
        assertEquals(dude, trie[ca])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(ca)
        assertEquals("", trie[ca])
        assertEquals(ROOT_HASH_AFTER, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteShortString2() {
        val ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee"
        val ROOT_HASH_AFTER = "b25e1b5be78dbadf6c4e817c6d170bbb47e9916f8f6cc4607c5f3819ce98497b"
        val trie = StringTrie(mockDb)

        trie.put(ca, dude)
        assertEquals(dude, trie[ca])

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(cat)
        assertEquals("", trie[cat])
        assertEquals(ROOT_HASH_AFTER, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteShortString3() {
        val ROOT_HASH_BEFORE = "778ab82a7e8236ea2ff7bb9cfa46688e7241c1fd445bf2941416881a6ee192eb"
        val ROOT_HASH_AFTER = "05875807b8f3e735188d2479add82f96dee4db5aff00dc63f07a7e27d0deab65"
        val trie = StringTrie(mockDb)

        trie.put(cat, dude)
        assertEquals(dude, trie[cat])

        trie.put(dog, test)
        assertEquals(test, trie[dog])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(dog)
        assertEquals("", trie[dog])
        assertEquals(ROOT_HASH_AFTER, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteLongString1() {
        val ROOT_HASH_BEFORE = "318961a1c8f3724286e8e80d312352f01450bc4892c165cc7614e1c2e5a0012a"
        val ROOT_HASH_AFTER = "63356ecf33b083e244122fca7a9b128cc7620d438d5d62e4f8b5168f1fb0527b"
        val trie = StringTrie(mockDb)

        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])

        trie.put(dog, LONG_STRING)
        assertEquals(LONG_STRING, trie[dog])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(dog)
        assertEquals("", trie[dog])
        assertEquals(ROOT_HASH_AFTER, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteLongString2() {
        val ROOT_HASH_BEFORE = "e020de34ca26f8d373ff2c0a8ac3a4cb9032bfa7a194c68330b7ac3584a1d388"
        val ROOT_HASH_AFTER = "334511f0c4897677b782d13a6fa1e58e18de6b24879d57ced430bad5ac831cb2"
        val trie = StringTrie(mockDb)

        trie.put(ca, LONG_STRING)
        assertEquals(LONG_STRING, trie[ca])

        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(cat)
        assertEquals("", trie[cat])
        assertEquals(ROOT_HASH_AFTER, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteLongString3() {
        val ROOT_HASH_BEFORE = "e020de34ca26f8d373ff2c0a8ac3a4cb9032bfa7a194c68330b7ac3584a1d388"
        val ROOT_HASH_AFTER = "63356ecf33b083e244122fca7a9b128cc7620d438d5d62e4f8b5168f1fb0527b"
        val trie = StringTrie(mockDb)

        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])

        trie.put(ca, LONG_STRING)
        assertEquals(LONG_STRING, trie[ca])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(ca)
        assertEquals("", trie[ca])
        assertEquals(ROOT_HASH_AFTER, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteCompletellyDiferentItems() {
        val trie = TrieImpl(mockDb)

        val val_1 = "1000000000000000000000000000000000000000000000000000000000000000"
        val val_2 = "2000000000000000000000000000000000000000000000000000000000000000"
        val val_3 = "3000000000000000000000000000000000000000000000000000000000000000"

        trie.put(Hex.decode(val_1), Hex.decode(val_1))
        trie.put(Hex.decode(val_2), Hex.decode(val_2))

        val root1 = Hex.toHexString(trie.rootHash)

        trie.put(Hex.decode(val_3), Hex.decode(val_3))
        trie.delete(Hex.decode(val_3))
        val root1_ = Hex.toHexString(trie.rootHash)

        Assert.assertEquals(root1, root1_)
    }

    @Test
    fun testDeleteMultipleItems1() {
        val ROOT_HASH_BEFORE = "3a784eddf1936515f0313b073f99e3bd65c38689021d24855f62a9601ea41717"
        val ROOT_HASH_AFTER1 = "60a2e75cfa153c4af2783bd6cb48fd6bed84c6381bc2c8f02792c046b46c0653"
        val ROOT_HASH_AFTER2 = "a84739b4762ddf15e3acc4e6957e5ab2bbfaaef00fe9d436a7369c6f058ec90d"
        val trie = StringTrie(mockDb)

        trie.put(cat, dog)
        assertEquals(dog, trie[cat])

        trie.put(ca, dude)
        assertEquals(dude, trie[ca])

        trie.put(doge, LONG_STRING)
        assertEquals(LONG_STRING, trie[doge])

        trie.put(dog, test)
        assertEquals(test, trie[dog])

        trie.put(test, LONG_STRING)
        assertEquals(LONG_STRING, trie[test])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        println(trie.dumpStructure())
        trie.delete(dog)
        println(trie.dumpStructure())
        assertEquals("", trie[dog])
        assertEquals(ROOT_HASH_AFTER1, Hex.toHexString(trie.rootHash))

        trie.delete(test)
        println(trie.dumpStructure())
        assertEquals("", trie[test])
        assertEquals(ROOT_HASH_AFTER2, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDeleteMultipleItems2() {
        val ROOT_HASH_BEFORE = "cf1ed2b6c4b6558f70ef0ecf76bfbee96af785cb5d5e7bfc37f9804ad8d0fb56"
        val ROOT_HASH_AFTER1 = "f586af4a476ba853fca8cea1fbde27cd17d537d18f64269fe09b02aa7fe55a9e"
        val ROOT_HASH_AFTER2 = "c59fdc16a80b11cc2f7a8b107bb0c954c0d8059e49c760ec3660eea64053ac91"

        val trie = StringTrie(mockDb)
        val c = "c"
        trie.put(c, LONG_STRING)
        assertEquals(LONG_STRING, trie[c])

        trie.put(ca, LONG_STRING)
        assertEquals(LONG_STRING, trie[ca])

        trie.put(cat, LONG_STRING)
        assertEquals(LONG_STRING, trie[cat])
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(ca)
        assertEquals("", trie[ca])
        assertEquals(ROOT_HASH_AFTER1, Hex.toHexString(trie.rootHash))

        trie.delete(cat)
        assertEquals("", trie[cat])
        assertEquals(ROOT_HASH_AFTER2, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testMassiveDelete() {
        val trie = TrieImpl(mockDb)
        var rootHash1: ByteArray? = null
        for (i in 0..10999) {
            trie.put(HashUtil.sha3(intToBytes(i)), HashUtil.sha3(intToBytes(i + 1000000)))
            if (i == 10000) {
                rootHash1 = trie.rootHash
            }
        }
        for (i in 10001..10999) {
            trie.delete(HashUtil.sha3(intToBytes(i)))
        }

        val rootHash2 = trie.rootHash
        assertArrayEquals(rootHash1, rootHash2)
    }

    @Test
    fun testDeleteAll() {
        val ROOT_HASH_BEFORE = "a84739b4762ddf15e3acc4e6957e5ab2bbfaaef00fe9d436a7369c6f058ec90d"
        val trie = StringTrie(mockDb)
        assertEquals(ROOT_HASH_EMPTY, Hex.toHexString(trie.rootHash))

        trie.put(ca, dude)
        trie.put(cat, dog)
        trie.put(doge, LONG_STRING)
        assertEquals(ROOT_HASH_BEFORE, Hex.toHexString(trie.rootHash))

        trie.delete(ca)
        trie.delete(cat)
        trie.delete(doge)
        assertEquals(ROOT_HASH_EMPTY, Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testTrieEquals() {
        val trie1 = StringTrie(mockDb)
        val trie2 = StringTrie(mockDb)

        trie1.put(doge, LONG_STRING)
        trie2.put(doge, LONG_STRING)
        assertTrue("Expected tries to be equal", trie1 == trie2)
        assertEquals(Hex.toHexString(trie1.rootHash), Hex.toHexString(trie2.rootHash))

        trie1.put(dog, LONG_STRING)
        trie2.put(cat, LONG_STRING)
        assertFalse("Expected tries not to be equal", trie1 == trie2)
        assertNotEquals(Hex.toHexString(trie1.rootHash), Hex.toHexString(trie2.rootHash))
    }

    @Test
    fun testSingleItem() {
        val trie = StringTrie(mockDb)
        trie.put("A", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")

        assertEquals("d23786fb4a010da3ce639d66d5e904a11dbc02746d1ce25029e53290cabf28ab", Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testDogs() {
        val trie = StringTrie(mockDb)
        trie.put("doe", "reindeer")
        assertEquals("11a0327cfcc5b7689b6b6d727e1f5f8846c1137caaa9fc871ba31b7cce1b703e", Hex.toHexString(trie.rootHash))

        trie.put("dog", "puppy")
        assertEquals("05ae693aac2107336a79309e0c60b24a7aac6aa3edecaef593921500d33c63c4", Hex.toHexString(trie.rootHash))

        trie.put("dogglesworth", "cat")
        assertEquals("8aad789dff2f538bca5d8ea56e8abe10f4c7ba3a5dea95fea4cd6e7c3a1168d3", Hex.toHexString(trie.rootHash))
    }

    // Using tests from: https://github.com/ethereum/tests/blob/master/trietest.json

    @Test
    fun testPuppy() {
        val trie = StringTrie(mockDb)
        trie.put("do", "verb")
        trie.put("doge", "coin")
        trie.put("horse", "stallion")
        trie.put("dog", "puppy")

        assertEquals("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84", Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testEmptyValues() {
        val trie = StringTrie(mockDb)
        trie.put("do", "verb")
        trie.put("ether", "wookiedoo")
        trie.put("horse", "stallion")
        trie.put("shaman", "horse")
        trie.put("doge", "coin")
        trie.put("ether", "")
        trie.put("dog", "puppy")
        trie.put("shaman", "")

        assertEquals("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84", Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testFoo() {
        val trie = StringTrie(mockDb)
        trie.put("foo", "bar")
        trie.put("food", "bat")
        trie.put("food", "bass")

        assertEquals("17beaa1648bafa633cda809c90c04af50fc8aed3cb40d16efbddee6fdf63c4c3", Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testSmallValues() {
        val trie = StringTrie(mockDb)

        trie.put("be", "e")
        trie.put("dog", "puppy")
        trie.put("bed", "d")
        assertEquals("3f67c7a47520f79faa29255d2d3c084a7a6df0453116ed7232ff10277a8be68b", Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testTesty() {
        val trie = StringTrie(mockDb)

        trie.put("test", "test")
        assertEquals("85d106d4edff3b7a4889e91251d0a87d7c17a1dda648ebdba8c6060825be23b8", Hex.toHexString(trie.rootHash))

        trie.put("te", "testy")
        assertEquals("8452568af70d8d140f58d941338542f645fcca50094b20f3c3d8c3df49337928", Hex.toHexString(trie.rootHash))
    }

    @Test
    fun testMasiveUpdate() {
        val massiveUpdateTestEnabled = false

        if (massiveUpdateTestEnabled) {
            val randomWords = Arrays.asList(*randomDictionary.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray())
            val testerMap = HashMap<String, String>()

            val trie = StringTrie(mockDb)
            val generator = Random()

            // Random insertion
            for (i in 0..99999) {

                val randomIndex1 = generator.nextInt(randomWords.size)
                val randomIndex2 = generator.nextInt(randomWords.size)

                val word1 = randomWords[randomIndex1].trim { it <= ' ' }
                val word2 = randomWords[randomIndex2].trim { it <= ' ' }

                trie.put(word1, word2)
                testerMap.put(word1, word2)
            }

            val half = testerMap.size / 2
            for (r in 0..half - 1) {

                val randomIndex = generator.nextInt(randomWords.size)
                val word1 = randomWords[randomIndex].trim { it <= ' ' }

                testerMap.remove(word1)
                trie.delete(word1)
            }

            // Assert the result now
            for (mapWord1 in testerMap.keys) {

                val mapWord2 = testerMap[mapWord1]
                val treeWord2 = trie[mapWord1]

                Assert.assertEquals(mapWord2, treeWord2)
            }
        }
    }

    @Ignore
    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun testMasiveDetermenisticUpdate() {

        // should be root: cfd77c0fcb037adefce1f4e2eb94381456a4746379d2896bb8f309c620436d30

        val massiveUpload_1 = ClassLoader
                .getSystemResource("trie/massive-upload.dmp")

        val file = File(massiveUpload_1.toURI())
        val strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)

        // *** Part - 1 ***
        // 1. load the data from massive-upload.dmp
        //    which includes deletes/upadtes (5000 operations)
        val trieSingle = StringTrie(mockDb_2)
        strData
                .map { it.split("=".toRegex()).dropLastWhile(String::isEmpty).toTypedArray() }
                .forEach { keyVal ->
                    if (keyVal[0] == "*")
                        trieSingle.delete(keyVal[1].trim { it <= ' ' })
                    else
                        trieSingle.put(keyVal[0].trim { it <= ' ' }, keyVal[1].trim { it <= ' ' })
                }


        println("root_1:  => " + Hex.toHexString(trieSingle.rootHash))

        // *** Part - 2 ***
        // pre. we use the same data from massive-upload.dmp
        //      which includes deletes/upadtes (100000 operations)
        // 1. part of the data loaded
        // 2. the trie cache sync to the db
        // 3. the rest of the data loaded with part of the trie not in the cache
        val trie = StringTrie(mockDb)

        (0..1999)
                .map { strData[it].split("=".toRegex()).dropLastWhile(String::isEmpty).toTypedArray() }
                .forEach { keyVal ->
                    if (keyVal[0] == "*")
                        trie.delete(keyVal[1].trim { it <= ' ' })
                    else
                        trie.put(keyVal[0].trim { it <= ' ' }, keyVal[1].trim { it <= ' ' })
                }

        val trie2 = StringTrie(mockDb, trie.rootHash)

        (2000..strData.size - 1)
                .map { strData[it].split("=".toRegex()).dropLastWhile(String::isEmpty).toTypedArray() }
                .forEach { keyVal ->
                    if (keyVal[0] == "*")
                        trie2.delete(keyVal[1].trim { it <= ' ' })
                    else
                        trie2.put(keyVal[0].trim { it <= ' ' }, keyVal[1].trim { it <= ' ' })
                }

        println("root_2:  => " + Hex.toHexString(trie2.rootHash))

        assertEquals(trieSingle.rootHash, trie2.rootHash)

    }

    @Test //  tests saving keys to the file  //
    fun testMasiveUpdateFromDB() {
        val massiveUpdateFromDBEnabled = false

        if (massiveUpdateFromDBEnabled) {
            val randomWords = Arrays.asList(*randomDictionary.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray())
            val testerMap = HashMap<String, String>()

            val trie = StringTrie(mockDb)
            val generator = Random()

            // Random insertion
            for (i in 0..49999) {

                val randomIndex1 = generator.nextInt(randomWords.size)
                val randomIndex2 = generator.nextInt(randomWords.size)

                val word1 = randomWords[randomIndex1].trim { it <= ' ' }
                val word2 = randomWords[randomIndex2].trim { it <= ' ' }

                trie.put(word1, word2)
                testerMap.put(word1, word2)
            }

            //            trie.cleanCache();
            //            trie.sync();

            // Assert the result now
            var keys = testerMap.keys.iterator()
            while (keys.hasNext()) {

                val mapWord1 = keys.next()
                val mapWord2 = testerMap[mapWord1]
                val treeWord2 = trie[mapWord1]

                Assert.assertEquals(mapWord2, treeWord2)
            }

            val trie2 = TrieImpl(mockDb, trie.rootHash)

            // Assert the result now
            keys = testerMap.keys.iterator()
            while (keys.hasNext()) {

                val mapWord1 = keys.next()
                val mapWord2 = testerMap[mapWord1]
                val treeWord2 = String(trie2[mapWord1.toByteArray()])

                Assert.assertEquals(mapWord2, treeWord2)
            }
        }
    }

    @Test
    @Throws(URISyntaxException::class, IOException::class)
    fun testRollbackTrie() {

        var trieSingle = StringTrie(mockDb)

        val massiveUpload_1 = ClassLoader
                .getSystemResource("trie/massive-upload.dmp")

        val file = File(massiveUpload_1.toURI())
        val strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)

        val roots = ArrayList<ByteArray>()
        val trieDumps = HashMap<String, String>()

        for (i in 0..99) {

            val keyVal = strData[i].split("=".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()

            if (keyVal[0] == "*")
                trieSingle.delete(keyVal[1].trim { it <= ' ' })
            else
                trieSingle.put(keyVal[0].trim { it <= ' ' }, keyVal[1].trim { it <= ' ' })

            val hash = trieSingle.rootHash
            roots.add(hash)

            val key = Hex.toHexString(hash)
            val dump = trieSingle.trieDump
            trieDumps.put(key, dump)
        }

        // compare all 100 rollback dumps and
        // the originaly saved dumps
        for (i in 1..roots.size - 1) {

            val root = roots[i]
            logger.info("rollback over root : {}", Hex.toHexString(root))

            trieSingle = StringTrie(mockDb, root)
            val currDump = trieSingle.trieDump
            val originDump = trieDumps[Hex.toHexString(root)]
            Assert.assertEquals(currDump, originDump)
        }

    }

    @Test
    fun testGetFromRootNode() {
        val trie1 = StringTrie(mockDb)
        trie1.put(cat, LONG_STRING)
        val trie2 = TrieImpl(mockDb, trie1.rootHash)
        assertEquals(LONG_STRING, String(trie2[cat.toByteArray()]))
    }

    @Test
    fun storageHashCalc_1() {

        val key1 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000010")
        val key2 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000014")
        val key3 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000016")
        val key4 = Hex.decode("0000000000000000000000000000000000000000000000000000000000000017")

        val val1 = Hex.decode("947e70f9460402290a3e487dae01f610a1a8218fda")
        val val2 = Hex.decode("40")
        val val3 = Hex.decode("94412e0c4f0102f3f0ac63f0a125bce36ca75d4e0d")
        val val4 = Hex.decode("01")

        val storage = TrieImpl()
        storage.put(key1, val1)
        storage.put(key2, val2)
        storage.put(key3, val3)
        storage.put(key4, val4)

        val hash = Hex.toHexString(storage.rootHash)

        println(hash)
        Assert.assertEquals("517eaccda568f3fa24915fed8add49d3b743b3764c0bc495b19a47c54dbc3d62", hash)
    }

    @Test
    @Throws(URISyntaxException::class, IOException::class, ParseException::class)
    fun testFromDump_1() {


        // LOAD: real dump from real state run
        val dbDump = ClassLoader
                .getSystemResource("dbdump/dbdump.json")

        val dbDumpFile = File(dbDump.toURI())
        val testData = Files.readAllBytes(dbDumpFile.toPath())
        val testSrc = String(testData)

        val parser = JSONParser()
        val dbDumpJSONArray = parser.parse(testSrc) as JSONArray

        //        KeyValueDataSource keyValueDataSource = new LevelDbDataSource("testState");
        //        keyValueDataSource.init();

        val dataSource = HashMapDB<ByteArray>()

        for (aDbDumpJSONArray in dbDumpJSONArray) {

            val obj = aDbDumpJSONArray as JSONObject
            val key = Hex.decode(obj["key"].toString())
            val `val` = Hex.decode(obj["val"].toString())

            dataSource.put(key, `val`)
        }

        // TEST: load trie out of this run up to block#33
        val rootNode = Hex.decode("bb690805d24882bc7ccae6fc0f80ac146274d5b81c6a6e9c882cd9b0a649c9c7")
        val trie = TrieImpl(dataSource, rootNode)

        // first key added in genesis
        val val1 = trie[Hex.decode("51ba59315b3a95761d0863b05ccc7a7f54703d99")]
        val accountState1 = AccountState(val1)

        assertEquals(BigInteger.valueOf(2).pow(200), accountState1.balance)
        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", Hex.toHexString(accountState1.codeHash))
        assertEquals(BigInteger.ZERO, accountState1.nonce)
        assertEquals(null, accountState1.stateRoot)

        // last key added up to block#33
        val val2 = trie[Hex.decode("a39c2067eb45bc878818946d0f05a836b3da44fa")]
        val accountState2 = AccountState(val2)

        assertEquals(BigInteger("1500000000000000000"), accountState2.balance)
        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", Hex.toHexString(accountState2.codeHash))
        assertEquals(BigInteger.ZERO, accountState2.nonce)
        assertEquals(null, accountState2.stateRoot)

        //        keyValueDataSource.close();
    }

    @Test // update the trie with blog key/val
            // each time dump the entire trie
    fun testSample_1() {

        val trie = StringTrie(mockDb)

        trie.put("dog", "puppy")
        var dmp = trie.trieDump
        println(dmp)
        println()
        Assert.assertEquals("ed6e08740e4a267eca9d4740f71f573e9aabbcc739b16a2fa6c1baed5ec21278", Hex.toHexString(trie.rootHash))

        trie.put("do", "verb")
        dmp = trie.trieDump
        println(dmp)
        println()
        Assert.assertEquals("779db3986dd4f38416bfde49750ef7b13c6ecb3e2221620bcad9267e94604d36", Hex.toHexString(trie.rootHash))

        trie.put("doggiestan", "aeswome_place")
        dmp = trie.trieDump
        println(dmp)
        println()
        Assert.assertEquals("8bd5544747b4c44d1274aa99a6293065fe319b3230e800203317e4c75a770099", Hex.toHexString(trie.rootHash))
    }


    /*
        0x7645b9fbf1b51e6b980801fafe6bbc22d2ebe218 0x517eaccda568f3fa24915fed8add49d3b743b3764c0bc495b19a47c54dbc3d62 0x 0x1
        0x0000000000000000000000000000000000000000000000000000000000000010 0x947e70f9460402290a3e487dae01f610a1a8218fda
        0x0000000000000000000000000000000000000000000000000000000000000014 0x40
        0x0000000000000000000000000000000000000000000000000000000000000016 0x94412e0c4f0102f3f0ac63f0a125bce36ca75d4e0d
        0x0000000000000000000000000000000000000000000000000000000000000017 0x01
*/

    @Test
    fun testSecureTrie() {

        val trie = SecureTrie(mockDb)

        val k1 = "do".toByteArray()
        val v1 = "verb".toByteArray()

        val k2 = "ether".toByteArray()
        val v2 = "wookiedoo".toByteArray()

        val k3 = "horse".toByteArray()
        val v3 = "stallion".toByteArray()

        val k4 = "shaman".toByteArray()
        val v4 = "horse".toByteArray()

        val k5 = "doge".toByteArray()
        val v5 = "coin".toByteArray()

        val k6 = "ether".toByteArray()
        val v6 = "".toByteArray()

        val k7 = "dog".toByteArray()
        val v7 = "puppy".toByteArray()

        val k8 = "shaman".toByteArray()
        val v8 = "".toByteArray()

        trie.put(k1, v1)
        trie.put(k2, v2)
        trie.put(k3, v3)
        trie.put(k4, v4)
        trie.put(k5, v5)
        trie.put(k6, v6)
        trie.put(k7, v7)
        trie.put(k8, v8)

        val root = trie.rootHash

        logger.info("root: " + Hex.toHexString(root))

        Assert.assertEquals("29b235a58c3c25ab83010c327d5932bcf05324b7d6b1185e650798034783ca9d", Hex.toHexString(root))
    }

    // this case relates to a bug which led us to conflict on Morden network (block #486248)
    // first part of the new Value was converted to String by #asString() during key deletion
    // and some lines after String.getBytes() returned byte array which differed to array before converting
    @Test
    @Throws(ParseException::class, IOException::class, URISyntaxException::class)
    fun testBugFix() {

        val dataMap = HashMap<String, String>()
        dataMap.put("6e929251b981389774af84a07585724c432e2db487381810719c3dd913192ae2", "00000000000000000000000000000000000000000000000000000000000000be")
        dataMap.put("6e92718d00dae27b2a96f6853a0bf11ded08bc658b2e75904ca0344df5aff9ae", "00000000000000000000000000000000000000000000002f0000000000000000")

        val trie = TrieImpl()

        for ((key, value) in dataMap) {
            trie.put(Hex.decode(key), Hex.decode(value))
        }

        assertArrayEquals(trie[Hex.decode("6e929251b981389774af84a07585724c432e2db487381810719c3dd913192ae2")],
                Hex.decode("00000000000000000000000000000000000000000000000000000000000000be"))

        assertArrayEquals(trie[Hex.decode("6e92718d00dae27b2a96f6853a0bf11ded08bc658b2e75904ca0344df5aff9ae")],
                Hex.decode("00000000000000000000000000000000000000000000002f0000000000000000"))

        trie.delete(Hex.decode("6e9286c946c6dd1f5d97f35683732dc8a70dc511133a43d416892f527dfcd243"))

        assertArrayEquals(trie[Hex.decode("6e929251b981389774af84a07585724c432e2db487381810719c3dd913192ae2")],
                Hex.decode("00000000000000000000000000000000000000000000000000000000000000be"))

        assertArrayEquals(trie[Hex.decode("6e92718d00dae27b2a96f6853a0bf11ded08bc658b2e75904ca0344df5aff9ae")],
                Hex.decode("00000000000000000000000000000000000000000000002f0000000000000000"))
    }

    @Ignore
    @Test
    fun perfTestGet() {
        val db = HashMapDBSimple<ByteArray>()
        val trieCache = TrieCache()

        val trie: TrieImpl = TrieImpl(db)

        val keys = arrayOfNulls<ByteArray>(100000)

        println("Filling trie...")
        for (i in 0..99999) {
            val k = sha3(intToBytes(i))
            trie.put(k, k)
            if (i < keys.size) {
                keys[i] = k
            }
        }

        //        Trie trie1 = new TrieImpl(new ReadCache.BytesKey<>(trieCache), trie.getRootHash());
        //        Trie trie1 = new TrieImpl(trieCache.getDb(), trie.getRootHash());

        println("Benching...")
        while (true) {
            val s = System.nanoTime()
            for (j in 0..4) {
                for (k in 0..99) {
                    //                    Trie trie1 = new TrieImpl(new ReadCache.BytesKey<>(trieCache), trie.getRootHash());
                    //                    Trie trie1 = new TrieImpl(trieCache.getDb(), trie.getRootHash());
                    for (i in 0..999) {
                        val trie1 = TrieImpl(trieCache.db, trie.rootHash)
                        //                        Trie trie1 = new TrieImpl(trieCache, trie.getRootHash());
                        trie1[keys[k * 100 + i]!!]
                    }
                }
            }
            println(((System.nanoTime() - s) / 1000000).toString() + " ms")
        }
    }

    @Ignore
    @Test
    fun perfTestRoot() {

        while (true) {
            val db = HashMapDB<ByteArray>()
            val trieCache = TrieCache()

            //        TrieImpl trie = new TrieImpl(trieCache);
            val trie = TrieImpl(db, null)
            trie.setAsync(true)

            //            System.out.println("Filling trie...");
            val s = System.nanoTime()
            (0..199999)
                    .map { sha3(intToBytes(it)) }
                    .forEach { trie.put(it, ByteArray(512)) }
            val s1 = System.nanoTime()
            //            System.out.println("Calculating root...");
            println(Hex.toHexString(trie.rootHash))
            println(((System.nanoTime() - s) / 1000000).toString() + " ms, root: " + (System.nanoTime() - s1) / 1000000 + " ms")
        }
    }

    //    private static class StringTrie extends SourceCodec<String, String, byte[], byte[]> {
    //        public StringTrie(Source<byte[], Value> src) {
    //            this(src, null);
    //        }
    //        public StringTrie(Source<byte[], Value> src, byte[] root) {
    //            super(new TrieImpl(new NoDeleteSource<>(src), root), STR_SERIALIZER, STR_SERIALIZER);
    //        }
    //
    //        public byte[] getRootHash() {
    //            return ((TrieImpl) getSource()).getRootHash();
    //        }
    //
    //        public String getTrieDump() {
    //            return ((TrieImpl) getSource()).getTrieDump();
    //        }
    //
    //        @Override
    //        public boolean equals(Object obj) {
    //            return getSource().equals(((StringTrie) obj).getSource());
    //        }
    //    }
    private class StringTrie @JvmOverloads constructor(src: Source<ByteArray, ByteArray>, root: ByteArray? = null) : SourceCodec<String, String, ByteArray, ByteArray>(TrieImpl(NoDeleteSource(src), root), STR_SERIALIZER, STR_SERIALIZER) {

        val rootHash: ByteArray
            get() = (source as TrieImpl).rootHash

        val trieDump: String
            get() = (source as TrieImpl).dumpTrie()

        fun dumpStructure(): String {
            return (source as TrieImpl).dumpStructure()
        }

        override fun get(s: String): String {
            val ret = super.get(s)
            return ret ?: ""
        }

        fun put(s: String, `val`: String?) {
            if (`val` == null || `val`.isEmpty()) {
                super.delete(s)
            } else {
                super.put(s, `val`)
            }
        }

        override fun equals(obj: Any?): Boolean {
            return source == (obj as StringTrie).source
        }
    }

    inner class NoDoubleDeleteMapDB : HashMapDB<ByteArray>() {
        @Synchronized override fun delete(key: ByteArray) {
            if (storage[key] == null) {
                throw RuntimeException("Trying delete non-existing entry: " + Hex.toHexString(key))
            }
            super.delete(key)
        }

        val db: NoDoubleDeleteMapDB
            get() = this
    }

    inner class TrieCache : SourceCodec<ByteArray, Value, ByteArray, ByteArray>(NoDoubleDeleteMapDB(), Serializers.Identity<ByteArray>(), Serializers.TrieNodeSerializer) {

        val db: NoDoubleDeleteMapDB
            get() = source as NoDoubleDeleteMapDB
    }

    companion object {

        private val logger = LoggerFactory.getLogger("test")

        private val LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ"
        private val ROOT_HASH_EMPTY = Hex.toHexString(EMPTY_TRIE_HASH)

        private val ca = "ca"
        private val cat = "cat"
        private val dog = "dog"
        private val doge = "doge"
        private val test = "test"
        private val dude = "dude"
        private val STR_SERIALIZER = object : Serializer<String, ByteArray> {
            override fun serialize(`object`: String?): ByteArray? {
                return `object`?.toByteArray()
            }

            override fun deserialize(stream: ByteArray?): String? {
                return if (stream == null) null else String(stream)
            }
        }
    }
}
