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

package org.ethereum.trie;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.text.StrBuilder;
import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;
import static org.ethereum.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.ethereum.util.RLP.*;

public class TrieImpl implements Trie<byte[]> {
    private final static Object NULL_NODE = new Object();
    private final static int MIN_BRANCHES_CONCURRENTLY = 3;
    private static ExecutorService executor;
    private final Source<byte[], byte[]> cache;
    private Node root;
    private boolean async = true;

    public TrieImpl() {
        this((byte[]) null);
    }

    TrieImpl(final byte[] root) {
        this(new HashMapDB<>(), root);
    }

    public TrieImpl(final Source<byte[], byte[]> cache) {
        this(cache, null);
    }

    public TrieImpl(final Source<byte[], byte[]> cache, final byte[] root) {
        this.cache = cache;
        setRoot(root);
    }

    private static ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(4,
                    new ThreadFactoryBuilder().setNameFormat("trie-calc-thread-%d").build());
        }
        return executor;
    }

    private static String hash2str(final byte[] hash, final boolean shortHash) {
        final String ret = Hex.toHexString(hash);
        return "0x" + (shortHash ? ret.substring(0, 8) : ret);
    }

    private static String val2str(final byte[] val, final boolean shortHash) {
        String ret = Hex.toHexString(val);
        if (val.length > 16) {
            ret = ret.substring(0, 10) + "... len " + val.length;
        }
        return "\"" + ret + "\"";
    }

    public void setAsync(final boolean async) {
        this.async = async;
    }

    private void encode() {
        if (root != null) {
            root.encode();
        }
    }

    public void setRoot(final byte[] root) {
        if (root != null && !FastByteComparisons.equal(root, EMPTY_TRIE_HASH)) {
            this.root = new Node(root);
        } else {
            this.root = null;
        }

    }

    private boolean hasRoot() {
        return root != null && root.resolveCheck();
    }

    public Source<byte[], byte[]> getCache() {
        return cache;
    }

    private byte[] getHash(final byte[] hash) {
        return cache.get(hash);
    }

    private void addHash(final byte[] hash, final byte[] ret) {
        cache.put(hash, ret);
    }

    private void deleteHash(final byte[] hash) {
        cache.delete(hash);
    }

    public byte[] get(final byte[] key) {
        if (!hasRoot()) return null; // treating unknown root hash as empty trie
        final TrieKey k = TrieKey.fromNormal(key);
        return get(root, k);
    }

    private byte[] get(final Node n, final TrieKey k) {
        if (n == null) return null;

        final NodeType type = n.getType();
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) return n.branchNodeGetValue();
            final Node childNode = n.branchNodeGetChild(k.getHex(0));
            return get(childNode, k.shift(1));
        } else {
            final TrieKey k1 = k.matchAndShift(n.kvNodeGetKey());
            if (k1 == null) return null;
            if (type == NodeType.KVNodeValue) {
                return k1.isEmpty() ? n.kvNodeGetValue() : null;
            } else {
                return get(n.kvNodeGetChildNode(), k1);
            }
        }
    }

    public void put(final byte[] key, final byte[] value) {
        final TrieKey k = TrieKey.fromNormal(key);
        if (root == null) {
            if (value != null && value.length > 0) {
                root = new Node(k, value);
            }
        } else {
            if (value == null || value.length == 0) {
                root = delete(root, k);
            } else {
                root = insert(root, k, value);
            }
        }
    }

    private Node insert(final Node n, final TrieKey k, final Object nodeOrValue) {
        final NodeType type = n.getType();
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) return n.branchNodeSetValue((byte[]) nodeOrValue);
            final Node childNode = n.branchNodeGetChild(k.getHex(0));
            if (childNode != null) {
                return n.branchNodeSetChild(k.getHex(0), insert(childNode, k.shift(1), nodeOrValue));
            } else {
                final TrieKey childKey = k.shift(1);
                final Node newChildNode;
                if (!childKey.isEmpty()) {
                    newChildNode = new Node(childKey, nodeOrValue);
                } else {
                    newChildNode = nodeOrValue instanceof Node ?
                            (Node) nodeOrValue : new Node(childKey, nodeOrValue);
                }
                return n.branchNodeSetChild(k.getHex(0), newChildNode);
            }
        } else {
            final TrieKey commonPrefix = k.getCommonPrefix(n.kvNodeGetKey());
            if (commonPrefix.isEmpty()) {
                final Node newBranchNode = new Node();
                insert(newBranchNode, n.kvNodeGetKey(), n.kvNodeGetValueOrNode());
                insert(newBranchNode, k, nodeOrValue);
                n.dispose();
                return newBranchNode;
            } else if (commonPrefix.equals(k)) {
                return n.kvNodeSetValueOrNode(nodeOrValue);
            } else if (commonPrefix.equals(n.kvNodeGetKey())) {
                insert(n.kvNodeGetChildNode(), k.shift(commonPrefix.getLength()), nodeOrValue);
                return n.invalidate();
            } else {
                final Node newBranchNode = new Node();
                final Node newKvNode = new Node(commonPrefix, newBranchNode);
                // TODO can be optimized
                insert(newKvNode, n.kvNodeGetKey(), n.kvNodeGetValueOrNode());
                insert(newKvNode, k, nodeOrValue);
                n.dispose();
                return newKvNode;
            }
        }
    }

    @Override
    public void delete(final byte[] key) {
        final TrieKey k = TrieKey.fromNormal(key);
        if (root != null) {
            root = delete(root, k);
        }
    }

    private Node delete(final Node n, final TrieKey k) {
        final NodeType type = n.getType();
        final Node newKvNode;
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) {
                n.branchNodeSetValue(null);
            } else {
                final int idx = k.getHex(0);
                final Node child = n.branchNodeGetChild(idx);
                if (child == null) return n; // no key found

                final Node newNode = delete(child, k.shift(1));
                n.branchNodeSetChild(idx, newNode);
                if (newNode != null) return n; // newNode != null thus number of children didn't decrease
            }

            // child node or value was deleted and the branch node may need to be compacted
            final int compactIdx = n.branchNodeCompactIdx();
            if (compactIdx < 0) return n; // no compaction is required

            // only value or a single child left - compact branch node to kvNode
            n.dispose();
            if (compactIdx == 16) { // only value left
                return new Node(TrieKey.empty(true), n.branchNodeGetValue());
            } else { // only single child left
                newKvNode = new Node(TrieKey.singleHex(compactIdx), n.branchNodeGetChild(compactIdx));
            }
        } else { // n - kvNode
            final TrieKey k1 = k.matchAndShift(n.kvNodeGetKey());
            if (k1 == null) {
                // no key found
                return n;
            } else if (type == NodeType.KVNodeValue) {
                if (k1.isEmpty()) {
                    // delete this kvNode
                    n.dispose();
                    return null;
                } else {
                    // else no key found
                    return n;
                }
            } else {
                final Node newChild = delete(n.kvNodeGetChildNode(), k1);
                if (newChild == null) throw new RuntimeException("Shouldn't happen");
                newKvNode = n.kvNodeSetValueOrNode(newChild);
            }
        }

        // if we get here a new kvNode was created, now need to check
        // if it should be compacted with child kvNode
        final Node newChild = newKvNode.kvNodeGetChildNode();
        if (newChild.getType() != NodeType.BranchNode) {
            // two kvNodes should be compacted into a single one
            final TrieKey newKey = newKvNode.kvNodeGetKey().concat(newChild.kvNodeGetKey());
            final Node newNode = new Node(newKey, newChild.kvNodeGetValueOrNode());
            newChild.dispose();
            return newNode;
        } else {
            // no compaction needed
            return newKvNode;
        }
    }

    @Override
    public byte[] getRootHash() {
        encode();
        return root != null ? root.hash : EMPTY_TRIE_HASH;
    }

    @Override
    public void clear() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean flush() {
        if (root != null && root.dirty) {
            // persist all dirty nodes to underlying Source
            encode();
            // release all Trie Node instances for GC
            root = new Node(root.hash);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TrieImpl trieImpl1 = (TrieImpl) o;

        return FastByteComparisons.equal(getRootHash(), trieImpl1.getRootHash());

    }

    public String dumpStructure() {
        return root == null ? "<empty>" : root.dumpStruct("", "");
    }

    public String dumpTrie() {
        return dumpTrie(true);
    }

    private String dumpTrie(final boolean compact) {
        if (root == null) return "<empty>";
        encode();
        final StrBuilder ret = new StrBuilder();
        final List<String> strings = root.dumpTrieNode(compact);
        ret.append("Root: ").append(hash2str(getRootHash(), compact)).append("\n");
        for (final String s : strings) {
            ret.append(s).append('\n');
        }
        return ret.toString();
    }

    public void scanTree(final ScanAction scanAction) {
        scanTree(root, TrieKey.empty(false), scanAction);
    }

    private void scanTree(final Node node, final TrieKey k, final ScanAction scanAction) {
        if (node == null) return;
        if (node.hash != null) {
            scanAction.doOnNode(node.hash, node);
        }
        if (node.getType() == NodeType.BranchNode) {
            if (node.branchNodeGetValue() != null)
                scanAction.doOnValue(node.hash, node, k.toNormal(), node.branchNodeGetValue());
            for (int i = 0; i < 16; i++) {
                scanTree(node.branchNodeGetChild(i), k.concat(TrieKey.singleHex(i)), scanAction);
            }
        } else if (node.getType() == NodeType.KVNodeNode) {
            scanTree(node.kvNodeGetChildNode(), k.concat(node.kvNodeGetKey()), scanAction);
        } else {
            scanAction.doOnValue(node.hash, node, k.concat(node.kvNodeGetKey()).toNormal(), node.kvNodeGetValue());
        }
    }

    public enum NodeType {
        BranchNode,
        KVNodeValue,
        KVNodeNode
    }


    public interface ScanAction {

        void doOnNode(byte[] hash, Node node);

        void doOnValue(byte[] nodeHash, Node node, byte[] key, byte[] value);
    }

    public final class Node {
        private byte[] hash = null;
        private byte[] rlp = null;
        private RLP.LList parsedRlp = null;
        private boolean dirty = false;

        private Object[] children = null;

        // new empty BranchNode
        public Node() {
            children = new Object[17];
            dirty = true;
        }

        // new KVNode with key and (value or node)
        public Node(final TrieKey key, final Object valueOrNode) {
            this(new Object[]{key, valueOrNode});
            dirty = true;
        }

        // new Node with hash or RLP
        public Node(final byte[] hashOrRlp) {
            if (hashOrRlp.length == 32) {
                this.hash = hashOrRlp;
            } else {
                this.rlp = hashOrRlp;
            }
        }

        private Node(final RLP.LList parsedRlp) {
            this.parsedRlp = parsedRlp;
        }

        private Node(final Object[] children) {
            this.children = children;
        }

        public boolean resolveCheck() {
            if (rlp != null || parsedRlp != null || hash == null) return true;
            rlp = getHash(hash);
            return rlp != null;
        }

        private void resolve() {
            if (!resolveCheck()) {
                throw new RuntimeException("Invalid Trie state, can't resolve hash " + Hex.toHexString(hash));
            }
        }

        public byte[] encode() {
            return encode(1, true);
        }

        private byte[] encode(final int depth, final boolean forceHash) {
            if (!dirty) {
                return hash != null ? encodeElement(hash) : rlp;
            } else {
                final NodeType type = getType();
                final byte[] ret;
                if (type == NodeType.BranchNode) {
                    if (depth == 1 && async) {
                        // parallelize encode() on the first trie level only and if there are at least
                        // MIN_BRANCHES_CONCURRENTLY branches are modified
                        final Object[] encoded = new Object[17];
                        int encodeCnt = 0;
                        for (int i = 0; i < 16; i++) {
                            final Node child = branchNodeGetChild(i);
                            if (child == null) {
                                encoded[i] = EMPTY_ELEMENT_RLP;
                            } else if (!child.dirty) {
                                encoded[i] = child.encode(depth + 1, false);
                            } else {
                                encodeCnt++;
                            }
                        }
                        for (int i = 0; i < 16; i++) {
                            if (encoded[i] == null) {
                                final Node child = branchNodeGetChild(i);
                                if (encodeCnt >= MIN_BRANCHES_CONCURRENTLY) {
                                    encoded[i] = getExecutor().submit(() -> child.encode(depth + 1, false));
                                } else {
                                    encoded[i] = child.encode(depth + 1, false);
                                }
                            }
                        }
                        final byte[] value = branchNodeGetValue();
                        encoded[16] = constantFuture(encodeElement(value));
                        try {
                            ret = encodeRlpListFutures(encoded);
                        } catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        final byte[][] encoded = new byte[17][];
                        for (int i = 0; i < 16; i++) {
                            final Node child = branchNodeGetChild(i);
                            encoded[i] = child == null ? EMPTY_ELEMENT_RLP : child.encode(depth + 1, false);
                        }
                        final byte[] value = branchNodeGetValue();
                        encoded[16] = encodeElement(value);
                        ret = encodeList(encoded);
                    }
                } else if (type == NodeType.KVNodeNode) {
                    ret = encodeList(encodeElement(kvNodeGetKey().toPacked()), kvNodeGetChildNode().encode(depth + 1, false));
                } else {
                    final byte[] value = kvNodeGetValue();
                    ret = encodeList(encodeElement(kvNodeGetKey().toPacked()),
                                    encodeElement(value == null ? EMPTY_BYTE_ARRAY : value));
                }
                if (hash != null) {
                    deleteHash(hash);
                }
                dirty = false;
                if (ret.length < 32 && !forceHash) {
                    rlp = ret;
                    return ret;
                } else {
                    hash = HashUtil.sha3(ret);
                    addHash(hash, ret);
                    return encodeElement(hash);
                }
            }
        }

        @SafeVarargs
        private final byte[] encodeRlpListFutures(final Object... list) throws ExecutionException, InterruptedException {
            final byte[][] vals = new byte[list.length][];
            for (int i = 0; i < list.length; i++) {
                if (list[i] instanceof Future) {
                    vals[i] = ((Future<byte[]>) list[i]).get();
                } else {
                    vals[i] = (byte[]) list[i];
                }
            }
            return encodeList(vals);
        }

        private void parse() {
            if (children != null) return;
            resolve();

            final RLP.LList list = parsedRlp == null ? RLP.decodeLazyList(rlp) : parsedRlp;

            if (list.size() == 2) {
                children = new Object[2];
                final TrieKey key = TrieKey.fromPacked(list.getBytes(0));
                children[0] = key;
                if (key.isTerminal()) {
                    children[1] = list.getBytes(1);
                } else {
                    children[1] = list.isList(1) ? new Node(list.getList(1)) : new Node(list.getBytes(1));
                }
            } else {
                children = new Object[17];
                parsedRlp = list;
            }
        }

        public Node branchNodeGetChild(final int hex) {
            parse();
            assert getType() == NodeType.BranchNode;
            Object n = children[hex];
            if (n == null && parsedRlp != null) {
                if (parsedRlp.isList(hex)) {
                    n = new Node(parsedRlp.getList(hex));
                } else {
                    final byte[] bytes = parsedRlp.getBytes(hex);
                    if (bytes.length == 0) {
                        n = NULL_NODE;
                    } else {
                        n = new Node(bytes);
                    }
                }
                children[hex] = n;
            }
            return n == NULL_NODE ? null : (Node) n;
        }

        public Node branchNodeSetChild(final int hex, final Node node) {
            parse();
            assert getType() == NodeType.BranchNode;
            children[hex] = node == null ? NULL_NODE : node;
            dirty = true;
            return this;
        }

        public byte[] branchNodeGetValue() {
            parse();
            assert getType() == NodeType.BranchNode;
            Object n = children[16];
            if (n == null && parsedRlp != null) {
                final byte[] bytes = parsedRlp.getBytes(16);
                if (bytes.length == 0) {
                    n = NULL_NODE;
                } else {
                    n = bytes;
                }
                children[16] = n;
            }
            return n == NULL_NODE ? null : (byte[]) n;
        }

        public Node branchNodeSetValue(final byte[] val) {
            parse();
            assert getType() == NodeType.BranchNode;
            children[16] = val == null ? NULL_NODE : val;
            dirty = true;
            return this;
        }

        public int branchNodeCompactIdx() {
            parse();
            assert getType() == NodeType.BranchNode;
            int cnt = 0;
            int idx = -1;
            for (int i = 0; i < 16; i++) {
                if (branchNodeGetChild(i) != null) {
                    cnt++;
                    idx = i;
                    if (cnt > 1) return -1;
                }
            }
            return cnt > 0 ? idx : (branchNodeGetValue() == null ? -1 : 16);
        }
        public boolean branchNodeCanCompact() {
            parse();
            assert getType() == NodeType.BranchNode;
            int cnt = 0;
            for (int i = 0; i < 16; i++) {
                cnt += branchNodeGetChild(i) == null ? 0 : 1;
                if (cnt > 1) return false;
            }
            return cnt == 0 || branchNodeGetValue() == null;
        }

        public TrieKey kvNodeGetKey() {
            parse();
            assert getType() != NodeType.BranchNode;
            return (TrieKey) children[0];
        }

        public Node kvNodeGetChildNode() {
            parse();
            assert getType() == NodeType.KVNodeNode;
            return (Node) children[1];
        }

        public byte[] kvNodeGetValue() {
            parse();
            assert getType() == NodeType.KVNodeValue;
            return (byte[]) children[1];
        }

        public Node kvNodeSetValue(final byte[] value) {
            parse();
            assert getType() == NodeType.KVNodeValue;
            children[1] = value;
            dirty = true;
            return this;
        }

        public Object kvNodeGetValueOrNode() {
            parse();
            assert getType() != NodeType.BranchNode;
            return children[1];
        }

        public Node kvNodeSetValueOrNode(final Object valueOrNode) {
            parse();
            assert getType() != NodeType.BranchNode;
            children[1] = valueOrNode;
            dirty = true;
            return this;
        }

        public NodeType getType() {
            parse();

            return children.length == 17 ? NodeType.BranchNode :
                    (children[1] instanceof Node ? NodeType.KVNodeNode : NodeType.KVNodeValue);
        }

        public void dispose() {
            if (hash != null) {
                deleteHash(hash);
            }
        }

        public Node invalidate() {
            dirty = true;
            return this;
        }

        /***********  Dump methods  ************/

        public String dumpStruct(final String indent, final String prefix) {
            final StringBuilder ret = new StringBuilder(indent + prefix + getType() + (dirty ? " *" : "") +
                    (hash == null ? "" : "(hash: " + Hex.toHexString(hash).substring(0, 6) + ")"));
            if (getType() == NodeType.BranchNode) {
                final byte[] value = branchNodeGetValue();
                ret.append(value == null ? "" : " [T] = " + Hex.toHexString(value)).append("\n");
                for (int i = 0; i < 16; i++) {
                    final Node child = branchNodeGetChild(i);
                    if (child != null) {
                        ret.append(child.dumpStruct(indent + "  ", "[" + i + "] "));
                    }
                }

            } else if (getType() == NodeType.KVNodeNode) {
                ret.append(" [").append(kvNodeGetKey()).append("]\n");
                ret.append(kvNodeGetChildNode().dumpStruct(indent + "  ", ""));
            } else {
                ret.append(" [").append(kvNodeGetKey()).append("] = ").append(Hex.toHexString(kvNodeGetValue())).append("\n");
            }
            return ret.toString();
        }

        public List<String> dumpTrieNode(final boolean compact) {
            final List<String> ret = new ArrayList<>();
            if (hash != null) {
                ret.add(hash2str(hash, compact) + " ==> " + dumpContent(false, compact));
            }

            if (getType() == NodeType.BranchNode) {
                for (int i = 0; i < 16; i++) {
                    final Node child = branchNodeGetChild(i);
                    if (child != null) ret.addAll(child.dumpTrieNode(compact));
                }
            } else if (getType() == NodeType.KVNodeNode) {
                ret.addAll(kvNodeGetChildNode().dumpTrieNode(compact));
            }
            return ret;
        }

        private String dumpContent(final boolean recursion, final boolean compact) {
            if (recursion && hash != null) return hash2str(hash, compact);
            final StringBuilder ret;
            if (getType() == NodeType.BranchNode) {
                ret = new StringBuilder("[");
                for (int i = 0; i < 16; i++) {
                    final Node child = branchNodeGetChild(i);
                    ret.append(i == 0 ? "" : ",");
                    ret.append(child == null ? "" : child.dumpContent(true, compact));
                }
                final byte[] value = branchNodeGetValue();
                ret.append(value == null ? "" : ", " + val2str(value, compact));
                ret.append("]");
            } else if (getType() == NodeType.KVNodeNode) {
                ret = new StringBuilder("[<" + kvNodeGetKey() + ">, " + kvNodeGetChildNode().dumpContent(true, compact) + "]");
            } else {
                ret = new StringBuilder("[<" + kvNodeGetKey() + ">, " + val2str(kvNodeGetValue(), compact) + "]");
            }
            return ret.toString();
        }

        @Override
        public String toString() {
            return getType() + (dirty ? " *" : "") + (hash == null ? "" : "(hash: " + Hex.toHexString(hash) + " )");
        }
    }
}
