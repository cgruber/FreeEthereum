package org.ethereum.net.submit;

import org.ethereum.core.Transaction;

/**
 * @author Roman Mandeleil
 * @since 23.05.2014
 */
class WalletTransaction {

    private int approved = 0; // each time the tx got from the wire this value increased

    public WalletTransaction(Transaction tx) {
        Transaction tx1 = tx;
    }

    public void incApproved() {
        approved++;
    }

    public int getApproved() {
        return approved;
    }
}
