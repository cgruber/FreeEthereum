package org.ethereum.trie;

/**
 * @author Roman Mandeleil
 * @since 29.08.2014
 */
public class CountAllNodes implements TrieImpl.ScanAction {

    private int counted = 0;

    @Override
    public void doOnNode(byte[] hash, TrieImpl.Node node) {
        ++counted;
    }

    @Override
    public void doOnValue(byte[] nodeHash, TrieImpl.Node node, byte[] key, byte[] value) {}

    public int getCounted() {
        return counted;
    }
}
