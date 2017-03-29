package org.ethereum.jsonrpc;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;

/**
 * Created by Ruben on 8/1/2016.
 */
public class TransactionResultDTO {

    public String hash;
    public String v;
    public String r;
    public String s;
    private String nonce;
    private String blockHash;
    private String blockNumber;
    private String transactionIndex;
    private String from;
    private String to;
    private String gas;
    private String gasPrice;
    private String value;
    private String input;

    public TransactionResultDTO() {
    }

    public TransactionResultDTO (Block b, int index, Transaction tx) {
        hash =  TypeConverter.toJsonHex(tx.getHash());
        nonce = TypeConverter.toJsonHex(tx.getNonce());
        blockHash = b == null ? null : TypeConverter.toJsonHex(b.getHash());
        blockNumber = b == null ? null : TypeConverter.toJsonHex(b.getNumber());
        transactionIndex = b == null ? null : TypeConverter.toJsonHex(index);
        from= TypeConverter.toJsonHex(tx.getSender());
        to = tx.getReceiveAddress() == null ? null : TypeConverter.toJsonHex(tx.getReceiveAddress());
        gas = TypeConverter.toJsonHex(tx.getGasLimit());
        gasPrice = TypeConverter.toJsonHex(tx.getGasPrice());
        value = TypeConverter.toJsonHex(tx.getValue());
        input  = tx.getData() != null ? TypeConverter.toJsonHex(tx.getData()) : null;
    }

    @Override
    public String toString() {
        return "TransactionResultDTO{" +
                "hash='" + hash + '\'' +
                ", nonce='" + nonce + '\'' +
                ", blockHash='" + blockHash + '\'' +
                ", blockNumber='" + blockNumber + '\'' +
                ", transactionIndex='" + transactionIndex + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", gas='" + gas + '\'' +
                ", gasPrice='" + gasPrice + '\'' +
                ", value='" + value + '\'' +
                ", input='" + input + '\'' +
                '}';
    }
}