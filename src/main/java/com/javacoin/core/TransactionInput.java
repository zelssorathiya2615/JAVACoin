package com.javacoin.core;

import java.io.Serializable;

/**
 * TransactionInput - References a UTXO to be spent
 * Points to a previous transaction's output
 */
public class TransactionInput implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String utxoId;                  // ID of UTXO being spent (txHash:index)
    private String previousTransactionId;   // Transaction that created the UTXO
    private int outputIndex;                // Which output in that transaction
    private transient UTXO utxo;            // Reference to actual UTXO (not serialized)
    
    /**
     * Creates input referencing a UTXO
     * @param utxoId Unique UTXO identifier
     */
    public TransactionInput(String utxoId) {
        this.utxoId = utxoId;
        parseUtxoId(utxoId);
    }
    
    /**
     * Creates input from UTXO object
     * @param utxo UTXO to spend
     */
    public TransactionInput(UTXO utxo) {
        this.utxo = utxo;
        this.utxoId = utxo.getId();
        this.previousTransactionId = utxo.getParentTransactionId();
        this.outputIndex = utxo.getOutputIndex();
    }
    
    /**
     * Parses UTXO ID into transaction hash and output index
     * Format: "transactionHash:outputIndex"
     * @param utxoId UTXO ID string
     */
    private void parseUtxoId(String utxoId) {
        String[] parts = utxoId.split(":");
        if (parts.length == 2) {
            this.previousTransactionId = parts[0];
            try {
                this.outputIndex = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                this.outputIndex = 0;
            }
        }
    }
    
    /**
     * Gets the value of the referenced UTXO
     * @return Value in JAVACoin
     */
    public double getValue() {
        return utxo != null ? utxo.getValue() : 0.0;
    }
    
    /**
     * Links this input to its UTXO from the UTXO set
     * @param utxo UTXO object
     */
    public void setUtxo(UTXO utxo) {
        this.utxo = utxo;
    }
    
    // Getters
    
    public String getUtxoId() {
        return utxoId;
    }
    
    public String getPreviousTransactionId() {
        return previousTransactionId;
    }
    
    public int getOutputIndex() {
        return outputIndex;
    }
    
    public UTXO getUtxo() {
        return utxo;
    }
    
    @Override
    public String toString() {
        return String.format("Input{utxo='%s', value=%.2f}", 
                           utxoId, getValue());
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.