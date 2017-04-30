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

package org.ethereum.facade

import org.ethereum.config.DefaultConfig
import org.ethereum.config.SystemProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component


/**
 * @author Roman Mandeleil
 * *
 * @since 13.11.2014
 */
@Component
object EthereumFactory {

    private val logger = LoggerFactory.getLogger("general")

    @JvmOverloads fun createEthereum(userSpringConfig: Class<*>? = null as Class<*>?): Ethereum {
        return if (userSpringConfig == null)
            createEthereum(*arrayOf<Class<*>>(DefaultConfig::class.java))
        else
            createEthereum(DefaultConfig::class.java, userSpringConfig)
    }

    /**
     * @param config  Not used
     * *
     * @param userSpringConfig   User Spring configuration class
     * *
     * @return  Fully initialized Ethereum instance
     */
    @Deprecated("The config parameter is not used anymore. The configuration is passed\n      via 'systemProperties' bean either from the DefaultConfig or from supplied userSpringConfig\n      ")
    fun createEthereum(config: SystemProperties, userSpringConfig: Class<*>?): Ethereum {

        return if (userSpringConfig == null)
            createEthereum(*arrayOf<Class<*>>(DefaultConfig::class.java))
        else
            createEthereum(DefaultConfig::class.java, userSpringConfig)
    }

    private fun createEthereum(vararg springConfigs: Class<*>): Ethereum {
        logger.info("Starting EthereumJ...")
        val context = AnnotationConfigApplicationContext(*springConfigs)

        return context.getBean(Ethereum::class.java)
    }
}
