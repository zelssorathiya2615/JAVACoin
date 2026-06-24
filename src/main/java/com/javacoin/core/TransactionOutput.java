package com.javacoin.core;

import java.io.Serializable;
import java.security.PublicKey;

public class TransactionOutput implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;                // Will become UTXO ID after confirmation
    private PublicKey recipient;      // Who will own this output
    private double value;             // Amount in JAVACoin
    private String parentTransactionId; // Transaction creating this output
    
    public TransactionOutput(PublicKey recipient, double value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
    }
    
    public boolean isMine(PublicKey publicKey) {
        return this.recipient.equals(publicKey);
    }
    
    public UTXO toUTXO(int outputIndex) {
        // FIXED: Use parentTransactionId which should be set correctly
        return new UTXO(parentTransactionId, outputIndex, recipient, value);
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    // ADD THIS METHOD if it doesn't exist:
    public void setParentTransactionId(String parentTransactionId) {
        this.parentTransactionId = parentTransactionId;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public PublicKey getRecipient() {
        return recipient;
    }
    
    public double getValue() {
        return value;
    }
    
    public String getParentTransactionId() {
        return parentTransactionId;
    }
    
    @Override
    public String toString() {
        return String.format("Output{value=%.2f JAC, recipient='%s...'}", 
                           value, 
                           recipient.toString().substring(0, Math.min(20, recipient.toString().length())));
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.