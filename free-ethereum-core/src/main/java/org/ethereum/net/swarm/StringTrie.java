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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trie (or prefix tree) structure, which stores Nodes with String keys in highly branched structure
 * for quick search by string prefixes.
 * E.g. if there is a single node 'aaa' and the node 'aab' is added then the tree will look like:
 *      aa
 *     /  \
 *    a    b
 * Where leaf nodes correspond to added elements
 *
 * @param <P> Subclass of TrieNode
 */
abstract class StringTrie<P extends StringTrie.TrieNode<P>> {

    final P rootNode;

    public StringTrie(final P rootNode) {
        this.rootNode = rootNode;
    }

    public P get(final String path) {
        return rootNode.getMostSuitableChild(path);
    }

    public P add(final String path) {
        return add(rootNode, path);
    }

    public P add(final P parent, final String path) {
        return parent.addChild(path);
    }

    public P delete(final String path) {
        final P p = get(path);
        if (path.equals(p.getAbsolutePath()) && p.isLeaf()) {
            p.delete();
            return p;
        } else {
            return null;
        }
    }

    /**
     * Tree Node
     */
    public static abstract class TrieNode<N extends TrieNode<N>> {
        N parent;
        Map<String, N> children = new LinkedHashMap<>();
        String path; // path relative to parent

        /**
         * Create a root node (null parent and empty path)
         */
        TrieNode() {
            this(null, "");
        }

        public TrieNode(final N parent, final String relPath) {
            this.parent = parent;
            this.path = relPath;
        }

        public String getRelativePath() {
            return path;
        }

        /**
         * Calculates absolute path relative to the root node
         */
        public String getAbsolutePath() {
            return (parent != null ? parent.getAbsolutePath() : "") + path;
        }

        public N getParent() {
            return parent;
        }

        /**
         *  Finds the descendant which has longest common path prefix with the passed path
         */
        N getMostSuitableChild(final String relPath) {
            final N n = loadChildren().get(getKey(relPath));
            if (n == null) return (N) this;
            if (relPath.startsWith(n.getRelativePath())) {
                return n.getMostSuitableChild(relPath.substring(n.getRelativePath().length()));
            } else {
                return (N) this;
            }
        }

        /**
         * @return the direct child which has the key the same as the relPath key
         * null if no such child
         */
        N getChild(final String relPath) {
            return loadChildren().get(getKey(relPath));
        }

        /**
         * Add a new descendant with specified path
         * @param relPath the path relative to this node
         * @return added node wich path is [relPath] relative to this node
         */
        N add(final String relPath) {
            return addChild(relPath);
        }

        N addChild(final String relPath) {
            final N child = getChild(relPath);
            if (child == null) {
                final N newNode = createNode((N) this, relPath);
                putChild(newNode);
                return  newNode;
            } else {
                if (!child.isLeaf() && relPath.startsWith(child.getRelativePath())) {
                    return child.addChild(relPath.substring(child.getRelativePath().length()));
                }
                if (child.isLeaf() && relPath.equals(child.getRelativePath())) {
                    return child;
                }
                final String commonPrefix = Util.getCommonPrefix(relPath, child.getRelativePath());
                final N newSubRoot = createNode((N) this, commonPrefix);
                child.path = child.path.substring(commonPrefix.length());
                child.parent = newSubRoot;
                final N newNode = createNode(newSubRoot, relPath.substring(commonPrefix.length()));

                newSubRoot.putChild(child);
                newSubRoot.putChild(newNode);
                this.putChild(newSubRoot);

                newSubRoot.nodeChanged();
                this.nodeChanged();
                child.nodeChanged();

                return newNode;
            }
        }

        /**
         * Deletes current leaf node, rebalancing the tree if needed
         * @throws RuntimeException if this node is not leaf
         */
        void delete() {
            if (!isLeaf()) throw new RuntimeException("Can't delete non-leaf entry: " + this);
            final N parent = getParent();
            parent.loadChildren().remove(getKey(getRelativePath()));
            if (parent.loadChildren().size() == 1 && parent.parent != null) {
                final Map<String, N> c = parent.loadChildren();
                final N singleChild = c.values().iterator().next();
                singleChild.path = parent.path + singleChild.path;
                singleChild.parent = parent.parent;
                parent.parent.loadChildren().remove(getKey(parent.path));
                parent.parent.putChild(singleChild);
                parent.parent.nodeChanged();
                singleChild.nodeChanged();
            }
        }

        void putChild(final N n) {
            loadChildren().put(getKey(n.path), n);
        }

        /**
         * Returns the children if any. Doesn't cause any lazy loading
         */
        public Collection<N> getChildren() {
            return children.values();
        }

        /**
         * Returns the children after loading (if required)
         */
        Map<String, N> loadChildren() {
            return children;
        }

        public boolean isLeaf() {
            return loadChildren().isEmpty();
        }

        /**
         * Calculates the key for the string prefix.
         * The longer the key the more children remain on the same tree level
         * I.e. if the key is the first character of the path (e.g. with ASCII only chars),
         * the max number of children in a single node is 128.
         * @return Key corresponding to this path
         */
        String getKey(final String path) {
            return path.length() > 0 ? path.substring(0,1) : "";
        }

        /**
         * Subclass should create the instance of its own class
         * normally the implementation should invoke TrieNode(parent, path) superconstructor.
         */
        protected abstract N createNode(N parent, String path);

        /**
         * The node is notified on changes (either its path or direct children changed)
         */
        void nodeChanged() {
        }
    }

    /**
     * @return Pre-order walk tree elements iterator
     */
//    public Iterator<P> iterator() {
//        return new Iterator<P>() {
//            P curNode = rootNode;
//            Stack<Iterator<P>> childIndices = new Stack<>();
//
//            @Override
//            public boolean hasNext() {
//                return curNode != null;
//            }
//
//            @Override
//            public P next() {
//                P ret = curNode;
//                if (!curNode.getChildren().isEmpty()) {
//                    Iterator<P> it = curNode.getChildren().iterator();
//                    childIndices.push(it);
//                    curNode = it.next();
//                } else {
//                    curNode = null;
//                    while(curNode != null && !childIndices.isEmpty()) {
//                        Iterator<P> peek = childIndices.peek();
//                        if (peek.hasNext()) {
//                            curNode = peek.next();
//                        } else {
//                            childIndices.pop();
//                        }
//                    }
//                }
//                return ret;
//            }
//        };
//    }
//
//    /**
//     * @return Pre-order walk tree non-leaf elements iterator
//     */
//    public Iterator<P> nonLeafIterator() {
//        return new Iterator<P>() {
//            Iterator<P> leafIt = iterator();
//            P cur = findNext();
//
//            @Override
//            public boolean hasNext() {
//                return cur != null;
//            }
//
//            @Override
//            public P next() {
//                P ret = cur;
//                cur = findNext();
//                return ret;
//            }
//
//            private P findNext() {
//                P ret = null;
//                while(leafIt.hasNext() && (ret = leafIt.next()).isLeaf());
//                return ret;
//            }
//        };
//    }

//    protected abstract P createNode(P parent, String path);
//
//    protected void nodeChanged(P node) {}
}
