package org.ethereum.vm;

/**
 * The fundamental network cost unit. Paid for exclusively by Ether, which is converted
 * freely to and from Gas as required. Gas does not exist outside of the internal Ethereum
 * computation engine; its price is set by the Transaction and miners are free to
 * ignore Transactions whose Gas price is too low.
 */
public class GasCost {

    /* backwards compatibility, remove eventually */

    public int getSTEP() {
        int STEP = 1;
        return STEP;
    }

    public int getSSTORE() {
        int SSTORE = 300;
        return SSTORE;
    }

    public int getZEROSTEP() {
        int ZEROSTEP = 0;
        return ZEROSTEP;
    }

    public int getQUICKSTEP() {
        int QUICKSTEP = 2;
        return QUICKSTEP;
    }

    public int getFASTESTSTEP() {
        int FASTESTSTEP = 3;
        return FASTESTSTEP;
    }

    public int getFASTSTEP() {
        int FASTSTEP = 5;
        return FASTSTEP;
    }

    public int getMIDSTEP() {
        int MIDSTEP = 8;
        return MIDSTEP;
    }

    public int getSLOWSTEP() {
        int SLOWSTEP = 10;
        return SLOWSTEP;
    }

    public int getEXTSTEP() {
        int EXTSTEP = 20;
        return EXTSTEP;
    }

    public int getGENESISGASLIMIT() {
        int GENESISGASLIMIT = 1000000;
        return GENESISGASLIMIT;
    }

    public int getMINGASLIMIT() {
        int MINGASLIMIT = 125000;
        return MINGASLIMIT;
    }

    public int getBALANCE() {
        int BALANCE = 20;
        return BALANCE;
    }

    public int getSHA3() {
        int SHA3 = 30;
        return SHA3;
    }

    public int getSHA3_WORD() {
        int SHA3_WORD = 6;
        return SHA3_WORD;
    }

    public int getSLOAD() {
        int SLOAD = 50;
        return SLOAD;
    }

    public int getSTOP() {
        int STOP = 0;
        return STOP;
    }

    public int getSUICIDE() {
        int SUICIDE = 0;
        return SUICIDE;
    }

    public int getCLEAR_SSTORE() {
        int CLEAR_SSTORE = 5000;
        return CLEAR_SSTORE;
    }

    public int getSET_SSTORE() {
        int SET_SSTORE = 20000;
        return SET_SSTORE;
    }

    public int getRESET_SSTORE() {
        int RESET_SSTORE = 5000;
        return RESET_SSTORE;
    }

    public int getREFUND_SSTORE() {
        int REFUND_SSTORE = 15000;
        return REFUND_SSTORE;
    }

    public int getCREATE() {
        int CREATE = 32000;
        return CREATE;
    }

    public int getJUMPDEST() {
        int JUMPDEST = 1;
        return JUMPDEST;
    }

    public int getCREATE_DATA_BYTE() {
        int CREATE_DATA_BYTE = 5;
        return CREATE_DATA_BYTE;
    }

    public int getCALL() {
        int CALL = 40;
        return CALL;
    }

    public int getSTIPEND_CALL() {
        int STIPEND_CALL = 2300;
        return STIPEND_CALL;
    }

    public int getVT_CALL() {
        int VT_CALL = 9000;
        return VT_CALL;
    }

    public int getNEW_ACCT_CALL() {
        int NEW_ACCT_CALL = 25000;
        return NEW_ACCT_CALL;
    }

    public int getNEW_ACCT_SUICIDE() {
        int NEW_ACCT_SUICIDE = 0;
        return NEW_ACCT_SUICIDE;
    }

    public int getMEMORY() {
        int MEMORY = 3;
        return MEMORY;
    }

    public int getSUICIDE_REFUND() {
        int SUICIDE_REFUND = 24000;
        return SUICIDE_REFUND;
    }

    public int getQUAD_COEFF_DIV() {
        int QUAD_COEFF_DIV = 512;
        return QUAD_COEFF_DIV;
    }

    public int getCREATE_DATA() {
        int CREATE_DATA = 200;
        return CREATE_DATA;
    }

    public int getTX_NO_ZERO_DATA() {
        int TX_NO_ZERO_DATA = 68;
        return TX_NO_ZERO_DATA;
    }

    public int getTX_ZERO_DATA() {
        int TX_ZERO_DATA = 4;
        return TX_ZERO_DATA;
    }

    public int getTRANSACTION() {
        int TRANSACTION = 21000;
        return TRANSACTION;
    }

    public int getTRANSACTION_CREATE_CONTRACT() {
        int TRANSACTION_CREATE_CONTRACT = 53000;
        return TRANSACTION_CREATE_CONTRACT;
    }

    public int getLOG_GAS() {
        int LOG_GAS = 375;
        return LOG_GAS;
    }

    public int getLOG_DATA_GAS() {
        int LOG_DATA_GAS = 8;
        return LOG_DATA_GAS;
    }

    public int getLOG_TOPIC_GAS() {
        int LOG_TOPIC_GAS = 375;
        return LOG_TOPIC_GAS;
    }

    public int getCOPY_GAS() {
        int COPY_GAS = 3;
        return COPY_GAS;
    }

    public int getEXP_GAS() {
        int EXP_GAS = 10;
        return EXP_GAS;
    }

    public int getEXP_BYTE_GAS() {
        int EXP_BYTE_GAS = 10;
        return EXP_BYTE_GAS;
    }

    public int getIDENTITY() {
        int IDENTITY = 15;
        return IDENTITY;
    }

    public int getIDENTITY_WORD() {
        int IDENTITY_WORD = 3;
        return IDENTITY_WORD;
    }

    public int getRIPEMD160() {
        int RIPEMD160 = 600;
        return RIPEMD160;
    }

    public int getRIPEMD160_WORD() {
        int RIPEMD160_WORD = 120;
        return RIPEMD160_WORD;
    }

    public int getSHA256() {
        int SHA256 = 60;
        return SHA256;
    }

    public int getSHA256_WORD() {
        int SHA256_WORD = 12;
        return SHA256_WORD;
    }

    public int getEC_RECOVER() {
        int EC_RECOVER = 3000;
        return EC_RECOVER;
    }

    public int getEXT_CODE_SIZE() {
        int EXT_CODE_SIZE = 20;
        return EXT_CODE_SIZE;
    }

    public int getEXT_CODE_COPY() {
        int EXT_CODE_COPY = 20;
        return EXT_CODE_COPY;
    }
}
