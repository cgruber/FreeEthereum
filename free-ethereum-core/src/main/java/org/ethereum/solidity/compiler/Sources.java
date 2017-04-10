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

package org.ethereum.solidity.compiler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class Sources {

    private final Map<String, SourceArtifact> artifacts = new HashMap<>();
    private String targetArtifact;

    public Sources(final File[] files) {
        for (final File file : files) {
            artifacts.put(file.getName(), new SourceArtifact(file));
        }
    }

    public void resolveDependencies() {
        for (final String srcName : artifacts.keySet()) {
            final SourceArtifact src = artifacts.get(srcName);
            for (final String dep : src.getDependencies()) {
                final SourceArtifact depArtifact = artifacts.get(dep);
                if (depArtifact == null) {
                    throw ContractException.Companion.assembleError("can't resolve dependency: dependency '%s' not found.", dep);
                }
                src.injectDependency(depArtifact);
            }
        }

        for (final SourceArtifact artifact : artifacts.values()) {
            if (!artifact.hasDependentArtifacts()) {
                targetArtifact = artifact.getName();
            }
        }
    }
    
    public String plainSource() {
        return artifacts.get(targetArtifact).plainSource();
    }
}
