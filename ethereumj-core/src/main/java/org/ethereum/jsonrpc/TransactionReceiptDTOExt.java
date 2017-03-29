package org.ethereum.jsonrpc;

import org.ethereum.core.Block;
import org.ethereum.core.TransactionInfo;

import static org.ethereum.jsonrpc.TypeConverter.toJsonHex;

/**
 * Created by Anton Nashatyrev on 05.08.2016.
 */
public class TransactionReceiptDTOExt extends TransactionReceiptDTO {

    private final String returnData;
    private final String error;

    public TransactionReceiptDTOExt(Block block, TransactionInfo txInfo) {
        super(block, txInfo);
        returnData = toJsonHex(txInfo.getReceipt().getExecutionResult());
        error = txInfo.getReceipt().getError();
    }
}
