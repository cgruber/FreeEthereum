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
        final int STEP = 1;
        return STEP;
    }

    public int getSSTORE() {
        final int SSTORE = 300;
        return SSTORE;
    }

    public int getZEROSTEP() {
        final int ZEROSTEP = 0;
        return ZEROSTEP;
    }

    public int getQUICKSTEP() {
        final int QUICKSTEP = 2;
        return QUICKSTEP;
    }

    public int getFASTESTSTEP() {
        final int FASTESTSTEP = 3;
        return FASTESTSTEP;
    }

    public int getFASTSTEP() {
        final int FASTSTEP = 5;
        return FASTSTEP;
    }

    public int getMIDSTEP() {
        final int MIDSTEP = 8;
        return MIDSTEP;
    }

    public int getSLOWSTEP() {
        final int SLOWSTEP = 10;
        return SLOWSTEP;
    }

    public int getEXTSTEP() {
        final int EXTSTEP = 20;
        return EXTSTEP;
    }

    public int getGENESISGASLIMIT() {
        final int GENESISGASLIMIT = 1000000;
        return GENESISGASLIMIT;
    }

    public int getMINGASLIMIT() {
        final int MINGASLIMIT = 125000;
        return MINGASLIMIT;
    }

    public int getBALANCE() {
        final int BALANCE = 20;
        return BALANCE;
    }

    public int getSHA3() {
        final int SHA3 = 30;
        return SHA3;
    }

    public int getSHA3_WORD() {
        final int SHA3_WORD = 6;
        return SHA3_WORD;
    }

    public int getSLOAD() {
        final int SLOAD = 50;
        return SLOAD;
    }

    public int getSTOP() {
        final int STOP = 0;
        return STOP;
    }

    public int getSUICIDE() {
        final int SUICIDE = 0;
        return SUICIDE;
    }

    public int getCLEAR_SSTORE() {
        final int CLEAR_SSTORE = 5000;
        return CLEAR_SSTORE;
    }

    public int getSET_SSTORE() {
        final int SET_SSTORE = 20000;
        return SET_SSTORE;
    }

    public int getRESET_SSTORE() {
        final int RESET_SSTORE = 5000;
        return RESET_SSTORE;
    }

    public int getREFUND_SSTORE() {
        final int REFUND_SSTORE = 15000;
        return REFUND_SSTORE;
    }

    public int getCREATE() {
        final int CREATE = 32000;
        return CREATE;
    }

    public int getJUMPDEST() {
        final int JUMPDEST = 1;
        return JUMPDEST;
    }

    public int getCREATE_DATA_BYTE() {
        final int CREATE_DATA_BYTE = 5;
        return CREATE_DATA_BYTE;
    }

    public int getCALL() {
        final int CALL = 40;
        return CALL;
    }

    public int getSTIPEND_CALL() {
        final int STIPEND_CALL = 2300;
        return STIPEND_CALL;
    }

    public int getVT_CALL() {
        final int VT_CALL = 9000;
        return VT_CALL;
    }

    public int getNEW_ACCT_CALL() {
        final int NEW_ACCT_CALL = 25000;
        return NEW_ACCT_CALL;
    }

    public int getNEW_ACCT_SUICIDE() {
        final int NEW_ACCT_SUICIDE = 0;
        return NEW_ACCT_SUICIDE;
    }

    public int getMEMORY() {
        final int MEMORY = 3;
        return MEMORY;
    }

    public int getSUICIDE_REFUND() {
        final int SUICIDE_REFUND = 24000;
        return SUICIDE_REFUND;
    }

    public int getQUAD_COEFF_DIV() {
        final int QUAD_COEFF_DIV = 512;
        return QUAD_COEFF_DIV;
    }

    public int getCREATE_DATA() {
        final int CREATE_DATA = 200;
        return CREATE_DATA;
    }

    public int getTX_NO_ZERO_DATA() {
        final int TX_NO_ZERO_DATA = 68;
        return TX_NO_ZERO_DATA;
    }

    public int getTX_ZERO_DATA() {
        final int TX_ZERO_DATA = 4;
        return TX_ZERO_DATA;
    }

    public int getTRANSACTION() {
        final int TRANSACTION = 21000;
        return TRANSACTION;
    }

    public int getTRANSACTION_CREATE_CONTRACT() {
        final int TRANSACTION_CREATE_CONTRACT = 53000;
        return TRANSACTION_CREATE_CONTRACT;
    }

    public int getLOG_GAS() {
        final int LOG_GAS = 375;
        return LOG_GAS;
    }

    public int getLOG_DATA_GAS() {
        final int LOG_DATA_GAS = 8;
        return LOG_DATA_GAS;
    }

    public int getLOG_TOPIC_GAS() {
        final int LOG_TOPIC_GAS = 375;
        return LOG_TOPIC_GAS;
    }

    public int getCOPY_GAS() {
        final int COPY_GAS = 3;
        return COPY_GAS;
    }

    public int getEXP_GAS() {
        final int EXP_GAS = 10;
        return EXP_GAS;
    }

    public int getEXP_BYTE_GAS() {
        final int EXP_BYTE_GAS = 10;
        return EXP_BYTE_GAS;
    }

    public int getIDENTITY() {
        final int IDENTITY = 15;
        return IDENTITY;
    }

    public int getIDENTITY_WORD() {
        final int IDENTITY_WORD = 3;
        return IDENTITY_WORD;
    }

    public int getRIPEMD160() {
        final int RIPEMD160 = 600;
        return RIPEMD160;
    }

    public int getRIPEMD160_WORD() {
        final int RIPEMD160_WORD = 120;
        return RIPEMD160_WORD;
    }

    public int getSHA256() {
        final int SHA256 = 60;
        return SHA256;
    }

    public int getSHA256_WORD() {
        final int SHA256_WORD = 12;
        return SHA256_WORD;
    }

    public int getEC_RECOVER() {
        final int EC_RECOVER = 3000;
        return EC_RECOVER;
    }

    public int getEXT_CODE_SIZE() {
        final int EXT_CODE_SIZE = 20;
        return EXT_CODE_SIZE;
    }

    public int getEXT_CODE_COPY() {
        final int EXT_CODE_COPY = 20;
        return EXT_CODE_COPY;
    }
}
