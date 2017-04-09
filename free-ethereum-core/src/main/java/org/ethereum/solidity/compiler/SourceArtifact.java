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
import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.disjunction;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringsBetween;
import static org.ethereum.solidity.compiler.ContractException.assembleError;

class SourceArtifact {

    private final Set<SourceArtifact> injectedDependencies = new HashSet<>();
    private final Set<SourceArtifact> dependentArtifacts = new HashSet<>();
    private String name;
    private List<String> dependencies;
    private String source;

    public SourceArtifact(final String name, final String source) {
        this.name = name;
        this.dependencies = extractDependencies(source);
        this.source = source.replaceAll("import\\s\"\\.*?\\.sol\";", "");
    }

    public SourceArtifact(final File f) {

    }

    private static List<String> extractDependencies(final String source) {
        final String[] deps = substringsBetween(source, "import \"", "\";");
        return deps == null ? Collections.emptyList() : asList(deps);
    }

//    public SourceArtifact(MultipartFile srcFile) throws IOException {
//        this(srcFile.getOriginalFilename(), new String(srcFile.getBytes(), "UTF-8"));
//    }

    public void injectDependency(final SourceArtifact srcArtifact) {
        injectedDependencies.add(srcArtifact);
        srcArtifact.addDependentArtifact(this);
    }

    private void addDependentArtifact(final SourceArtifact srcArtifact) {
        dependentArtifacts.add(srcArtifact);
    }

    public boolean hasDependentArtifacts() {
        return !dependentArtifacts.isEmpty();
    }

    private Collection<String> getUnresolvedDependencies() {
        final Set<String> ret = new HashSet<>();
        for (final SourceArtifact injectedDependency : injectedDependencies) {
            ret.add(injectedDependency.getName());
        }

        return disjunction(dependencies, ret);
    }

    public String plainSource() {
        final Collection<String> unresolvedDeps = getUnresolvedDependencies();
        if (isNotEmpty(unresolvedDeps)) {
            throw assembleError("Followed dependencies aren't resolved: %s", unresolvedDeps);
        }

        String result = this.source;
        for (final SourceArtifact dependencyArtifact : injectedDependencies) {
            final String importDefinition = format("import \"%s\";", dependencyArtifact.getName());
            final String dependencySrc = format("// %s\n%s", importDefinition, dependencyArtifact.plainSource());

            result = result.replace(importDefinition, dependencySrc);
        }

        return result;
    }

    public String getName() {
        return name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}
