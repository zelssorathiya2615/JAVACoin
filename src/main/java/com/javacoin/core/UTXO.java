package com.javacoin.core;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;

/**
 * UTXO - Unspent Transaction Output
 * Represents a single "coin" that can be spent
 * This is the fundamental unit of value in JAVACoin
 */
public class UTXO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;                    // Unique identifier: txHash + ":" + outputIndex
    private String parentTransactionId;   // Transaction that created this UTXO
    private int outputIndex;              // Position in parent transaction's outputs
    private PublicKey recipient;          // Who owns this UTXO (public key)
    private String recipientAddress;      // Simplified address for display
    private double value;                 // Amount of JAVACoin
    private long timestamp;               // When this UTXO was created
    
    /**
     * Creates a new UTXO
     * @param parentTransactionId Transaction that created this output
     * @param outputIndex Position in outputs array
     * @param recipient Owner's public key
     * @param value Amount of JAVACoin
     */
    public UTXO(String parentTransactionId, int outputIndex, PublicKey recipient, double value) {
        this.parentTransactionId = parentTransactionId;
        this.outputIndex = outputIndex;
        this.recipient = recipient;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        
        // Generate unique ID
        this.id = parentTransactionId + ":" + outputIndex;
        
        // Generate simplified address from public key
        this.recipientAddress = generateAddress(recipient);
    }
    
    /**
     * Generates simplified address from public key
     * @param publicKey Public key
     * @return Shortened Base64 address
     */
    private String generateAddress(PublicKey publicKey) {
        String fullAddress = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return fullAddress.substring(0, Math.min(32, fullAddress.length()));
    }
    
    /**
     * Checks if this UTXO belongs to a specific public key
     * @param publicKey Public key to check
     * @return true if owned by this key
     */
    public boolean isMine(PublicKey publicKey) {
        return this.recipient.equals(publicKey);
    }
    
    /**
     * Checks if this UTXO can be spent (always true for UTXO objects)
     * In a real system, might check if it's in the UTXO set
     * @return true
     */
    public boolean canBeSpent() {
        return true;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getParentTransactionId() {
        return parentTransactionId;
    }
    
    public int getOutputIndex() {
        return outputIndex;
    }
    
    public PublicKey getRecipient() {
        return recipient;
    }
    
    public String getRecipientAddress() {
        return recipientAddress;
    }
    
    public double getValue() {
        return value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("UTXO{id='%s', value=%.2f JAC, owner='%s'}", 
                           id, value, recipientAddress);
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        return String.format(
            "┌─────────────────────────────────────────┐\n" +
            "│ UTXO (Unspent Coin)                     │\n" +
            "├─────────────────────────────────────────┤\n" +
            "│ ID: %s...│\n" +
            "│ Value: %.2f JAVACoin                    │\n" +
            "│ Owner: %s...        │\n" +
            "└─────────────────────────────────────────┘",
            id.substring(0, Math.min(20, id.length())),
            value,
            recipientAddress.substring(0, Math.min(15, recipientAddress.length()))
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UTXO utxo = (UTXO) obj;
        return id.equals(utxo.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.