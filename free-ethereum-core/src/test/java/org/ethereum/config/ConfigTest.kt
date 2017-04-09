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

package org.ethereum.config

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import org.junit.Assert
import org.junit.Test
import java.io.File

class ConfigTest {

    @Test
    fun simpleTest() {
        val config = ConfigFactory.parseResources("ethereumj.conf")
        println(config.root().render(ConfigRenderOptions.defaults().setComments(false)))
        for (entry in config.entrySet()) {
            //            System.out.println("Name:  " + entry.getKey());
            //            System.out.println(entry);
        }
        println("peer.listen.port: " + config.getInt("peer.listen.port"))
        println("peer.discovery.ip.list: " + config.getAnyRefList("peer.discovery.ip.list"))
        println("peer.discovery.ip.list: " + config.getAnyRefList("peer.active"))
        val list = config.getObjectList("peer.active")
        for (configObject in list) {
            if (configObject["url"] != null) {
                println("URL: " + configObject["url"])
            }
            if (configObject["ip"] != null) {
                println("IP: " + configObject)
            }
        }

        println("blocks.loader = " + config.hasPath("blocks.loader"))
        println("blocks.loader = " + config.getAnyRef("blocks.loader"))
    }

    @Test
    fun fallbackTest() {
        System.setProperty("blocks.loader", "bla-bla")
        val config = ConfigFactory.load("ethereumj.conf")
        // Ignore this assertion since the SystemProperties are loaded by the static initializer
        // so if the ConfigFactory was used prior to this test the setProperty() has no effect
        //        Assert.assertEquals("bla-bla", config.getString("blocks.loader"));
        val string = config.getString("keyvalue.datasource")
        Assert.assertNotNull(string)

        val overrides = ConfigFactory.parseString("blocks.loader=another, peer.active=[{url=sdfsfd}]")
        val merged = overrides.withFallback(config)
        Assert.assertEquals("another", merged.getString("blocks.loader"))
        Assert.assertTrue(merged.getObjectList("peer.active").size == 1)
        Assert.assertNotNull(merged.getString("keyvalue.datasource"))

        val emptyConf = ConfigFactory.parseFile(File("nosuchfile.conf"), ConfigParseOptions.defaults())
        Assert.assertFalse(emptyConf.hasPath("blocks.loader"))
    }

    @Test
    fun ethereumjConfTest() {
        println("'" + SystemProperties.getDefault()!!.databaseDir() + "'")
        println(SystemProperties.getDefault()!!.peerActive())
    }
}
