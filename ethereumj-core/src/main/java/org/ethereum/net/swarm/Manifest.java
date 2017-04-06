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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hierarchical structure of path items
 * The item can be one of two kinds:
 * - the leaf item which contains reference to the actual data with its content type
 * - the non-leaf item which contains reference to the child manifest with the dedicated content type
 */
public class Manifest {
    private final static String MANIFEST_MIME_TYPE = "application/bzz-manifest+json";
    private final StringTrie<ManifestEntry> trie;
    private final DPA dpa;

    /**
     * Constructs the Manifest instance with backing DPA storage
     *
     * @param dpa DPA
     */
    public Manifest(final DPA dpa) {
        this(dpa, new ManifestEntry(null, ""));
    }

    private Manifest(final DPA dpa, final ManifestEntry root) {
        this.dpa = dpa;
        trie = new StringTrie<ManifestEntry>(root.setThisMF(this)) {
        };
    }

    /**
     * Loads the manifest with the specified hashKey from the DPA storage
     */
    public static Manifest loadManifest(final DPA dpa, final String hashKey) {
        final ManifestRoot manifestRoot = load(dpa, hashKey);

        final Manifest ret = new Manifest(dpa);
        for (final Manifest.ManifestEntry entry : manifestRoot.entries) {
            ret.add(entry);
        }
        return ret;
    }

    private static Manifest.ManifestRoot load(final DPA dpa, final String hashKey) {
        try {
            final SectionReader sr = dpa.retrieve(new Key(hashKey));
            final ObjectMapper om = new ObjectMapper();
            final String s = Util.readerToString(sr);
            final ManifestRoot manifestRoot = om.readValue(s, ManifestRoot.class);
            return manifestRoot;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the entry with the specified path loading necessary nested manifests on demand
     */
    public ManifestEntry get(final String path) {
        return trie.get(path);
    }

    /**
     * Adds a new entry to the manifest hierarchy with loading necessary nested manifests on demand.
     * The entry path should contain the absolute path relative to this manifest root
     */
    public void add(final ManifestEntry entry) {
        add(null, entry);
    }

    private void add(final ManifestEntry parent, final ManifestEntry entry) {
        final ManifestEntry added = parent == null ? trie.add(entry.path) : trie.add(parent, entry.path);
        added.hash = entry.hash;
        added.contentType = entry.contentType;
        added.status = entry.status;
    }

    /**
     * Deletes the leaf manifest entry with the specified path
     */
    public void delete(final String path) {
        trie.delete(path);
    }

    /**
     * Saves this manifest (all its modified nodes) to this manifest DPA storage
     *
     * @return hashKey of the saved Manifest
     */
    public String save() {
        return save(trie.rootNode);
    }

    private String save(final ManifestEntry e) {
        if (e.isValid()) return e.hash;
        for (final ManifestEntry c : e.getChildren()) {
            save(c);
        }
        e.hash = serialize(dpa, e);
        return e.hash;
    }

    private String serialize(final DPA dpa, final ManifestEntry manifest) {
        try {
            final ObjectMapper om = new ObjectMapper();

            final ManifestRoot mr = new ManifestRoot(manifest);
            final String s = om.writeValueAsString(mr);

            final String hash = dpa.store(Util.stringToReader(s)).getHexString();
            return hash;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return manifest dump for debug purposes
     */
    public String dump() {
        return Util.dumpTree(trie.rootNode);
    }

    public enum Status {
        OK(200),
        NOT_FOUND(404);

        private final int code;

        Status(final int code) {
            this.code = code;
        }
    }

    // used for Json [de]serialization only
    private static class ManifestRoot {
        public List<ManifestEntry> entries = new ArrayList<>();

        public ManifestRoot() {
        }

        public ManifestRoot(final List<ManifestEntry> entries) {
            this.entries = entries;
        }

        public ManifestRoot(final ManifestEntry parent) {
            entries.addAll(parent.getChildren());
        }
    }

    /**
     *  Manifest item
     */
    @JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE,
                    fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
                    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class ManifestEntry extends StringTrie.TrieNode<ManifestEntry> {
        public String hash;
        public String contentType;
        public Status status;

        private Manifest thisMF;

        public ManifestEntry() {
            super(null, "");
        }

        public ManifestEntry(final String path, final String hash, final String contentType, final Status status) {
            super(null, "");
            this.path = path;
            this.hash = hash;
            this.contentType = contentType;
            this.status = status;
        }

        ManifestEntry(final ManifestEntry parent, final String path) {
            super(parent, path);
            this.path = path;
        }

        /**
         *  Indicates if this entry contains reference to a child manifest
         */
        public boolean isManifestType() { return MANIFEST_MIME_TYPE.equals(contentType);}
        boolean isValid() {return hash != null;}
        void invalidate() {hash = null;}

        @Override
        public boolean isLeaf() {
            return !(isManifestType() || !children.isEmpty());
        }

        /**
         *  loads the child manifest
         */
        @Override
        protected Map<String, ManifestEntry> loadChildren() {
            if (isManifestType() && children.isEmpty() && isValid()) {
                final ManifestRoot manifestRoot = load(thisMF.dpa, hash);
                children = new HashMap<>();
                for (final Manifest.ManifestEntry entry : manifestRoot.entries) {
                    children.put(getKey(entry.path), entry);
                }
            }
            return children;
        }

        @JsonProperty
        public String getPath() {
            return path;
        }

        @JsonProperty
        public void setPath(final String path) {
            this.path = path;
        }

        @Override
        protected ManifestEntry createNode(final ManifestEntry parent, final String path) {
            return new ManifestEntry(parent, path).setThisMF(parent.thisMF);
        }

        @Override
        protected void nodeChanged() {
            if (!isLeaf()) {
                contentType = MANIFEST_MIME_TYPE;
                invalidate();
            }
        }

        ManifestEntry setThisMF(final Manifest thisMF) {
            this.thisMF = thisMF;
            return this;
        }

        @Override
        public String toString() {
            return "ManifestEntry{" +
                    "path='" + path + '\'' +
                    ", hash='" + hash + '\'' +
                    ", contentType='" + contentType + '\'' +
                    ", status=" + status +
                    '}';
        }
    }
}
