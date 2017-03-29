package org.ethereum.net.submit;

import org.ethereum.core.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Roman Mandeleil
 * @since 23.05.2014
 */
public class TransactionExecutor {

    public static final TransactionExecutor instance;

    static {
        instance = new TransactionExecutor();
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    public Future<List<Transaction>> submitTransaction(TransactionTask task) {
        return executor.submit(task);
    }
}
