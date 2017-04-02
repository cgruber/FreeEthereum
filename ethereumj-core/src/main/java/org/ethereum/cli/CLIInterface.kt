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

package org.ethereum.cli

import org.ethereum.config.SystemProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.util.*

/**
 * @author Roman Mandeleil
 * *
 * @since 13.11.2014
 */
@Component
object CLIInterface {

    private val logger = LoggerFactory.getLogger("general")


    @JvmStatic fun call(args: Array<String>) {

        try {
            val cliOptions = HashMap<String, Any>()
            for (i in args.indices) {

                // override the db directory
                if (args[i] == "--help") {

                    printHelp()
                    System.exit(1)
                }

                // override the db directory
                if (args[i] == "-db" && i + 1 < args.size) {
                    val db = args[i + 1]
                    logger.info("DB directory set to [{}]", db)
                    cliOptions.put(SystemProperties.PROPERTY_DB_DIR, db)
                }

                // override the listen port directory
                if (args[i] == "-listen" && i + 1 < args.size) {
                    val port = args[i + 1]
                    logger.info("Listen port set to [{}]", port)
                    cliOptions.put(SystemProperties.PROPERTY_LISTEN_PORT, port)
                }

                // override the connect host:port directory
                if (args[i].startsWith("-connect") && i + 1 < args.size) {
                    val connectStr = args[i + 1]
                    logger.info("Connect URI set to [{}]", connectStr)
                    val uri = URI(connectStr)
                    if (uri.scheme != "enode")
                        throw RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT")
                    val peerActiveList = listOf<Map<String, String>>(Collections.singletonMap("url", connectStr))
                    cliOptions.put(SystemProperties.PROPERTY_PEER_ACTIVE, peerActiveList)
                }

                if (args[i] == "-connectOnly") {
                    cliOptions.put(SystemProperties.PROPERTY_PEER_DISCOVERY_ENABLED, false)
                }

                // override the listen port directory
                if (args[i] == "-reset" && i + 1 < args.size) {
                    val resetStr = interpret(args[i + 1])
                    logger.info("Resetting db set to [{}]", resetStr)
                    cliOptions.put(SystemProperties.PROPERTY_DB_RESET, resetStr.toString())
                }
            }

            if (cliOptions.size > 0) {
                logger.info("Overriding config file with CLI options: " + cliOptions)
            }
            SystemProperties.getDefault()!!.overrideParams(cliOptions)

        } catch (e: Throwable) {
            logger.error("Error parsing command line: [{}]", e.message)
            System.exit(1)
        }

    }

    private fun interpret(arg: String): Boolean {

        if (arg == "on" || arg == "true" || arg == "yes") return true
        if (arg == "off" || arg == "false" || arg == "no") return false

        throw Error("Can't interpret the answer: " + arg)
    }

    private fun printHelp() {

        println("--help                -- this help message ")
        println("-reset <yes/no>       -- reset yes/no the all database ")
        println("-db <db>              -- to setup the path for the database directory ")
        println("-listen  <port>       -- port to listen on for incoming connections ")
        println("-connect <enode://pubKey@host:port>  -- address actively connect to  ")
        println("-connectOnly <enode://pubKey@host:port>  -- like 'connect', but will not attempt to connect to other peers  ")
        println("")
        println("e.g: cli -reset no -db db-1 -listen 20202 -connect enode://0be5b4@poc-7.ethdev.com:30300 ")
        println("")

    }


}
