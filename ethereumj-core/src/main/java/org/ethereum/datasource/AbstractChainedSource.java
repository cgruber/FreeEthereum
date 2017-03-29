package org.ethereum.datasource;

/**
 * Abstract Source implementation with underlying backing Source
 * The class has control whether the backing Source should be flushed
 * in 'cascade' manner
 *
 * Created by Anton Nashatyrev on 06.12.2016.
 */
public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements Source<Key, Value> {

    boolean flushSource;
    private Source<SourceKey, SourceValue> source;

    /**
     * Intended for subclasses which wishes to initialize the source
     * later via {@link #setSource(Source)} method
     */
    AbstractChainedSource() {
    }

    AbstractChainedSource(Source<SourceKey, SourceValue> source) {
        this.source = source;
    }

    public Source<SourceKey, SourceValue> getSource() {
        return source;
    }

    /**
     * Intended for subclasses which wishes to initialize the source later
     */
    protected void setSource(Source<SourceKey, SourceValue> src) {
        source = src;
    }

    public void setFlushSource(boolean flushSource) {
        this.flushSource = flushSource;
    }

    /**
     * Invokes {@link #flushImpl()} and does backing Source flush if required
     * @return true if this or source flush did any changes
     */
    @Override
    public synchronized boolean flush() {
        boolean ret = flushImpl();
        if (flushSource) {
            ret |= getSource().flush();
        }
        return ret;
    }

    /**
     * Should be overridden to do actual source flush
     */
    protected abstract boolean flushImpl();
}
