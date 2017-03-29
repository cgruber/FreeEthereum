package org.ethereum.datasource;

/**
 * Just a convenient class to store arbitrary Objects into byte[] value backing
 * Source.
 * Includes ReadCache for caching deserialized objects and object Serializer
 *
 * Created by Anton Nashatyrev on 06.12.2016.
 */
public class ObjectDataSource<V> extends SourceChainBox<byte[], V, byte[], byte[]> {
    private final SourceCodec<byte[], V, byte[], byte[]> codec;
    private final Source<byte[], byte[]> byteSource;
    private ReadCache<byte[], V> cache;

    /**
     * Creates new instance
     * @param byteSource baking store
     * @param serializer for encode/decode byte[] <=> V
     * @param readCacheEntries number of entries to cache
     */
    public ObjectDataSource(Source<byte[], byte[]> byteSource, Serializer<V, byte[]> serializer, int readCacheEntries) {
        super(byteSource);
        this.byteSource = byteSource;
        add(codec = new SourceCodec<>(byteSource, new Serializers.Identity<>(), serializer));
        if (readCacheEntries > 0) {
            add(cache = new ReadCache.BytesKey<>(codec).withMaxCapacity(readCacheEntries));
        }
    }
}
