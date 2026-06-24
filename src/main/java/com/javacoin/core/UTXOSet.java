package com.javacoin.core;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UTXOSet - Thread-safe manager for all unspent transaction outputs
 * This is the "ledger" - tracks who owns which coins
 */
public class UTXOSet {
    
    // Thread-safe map: UTXO ID -> UTXO object
    private ConcurrentHashMap<String, UTXO> utxos;
    
    /**
     * Creates empty UTXO set
     */
    public UTXOSet() {
        this.utxos = new ConcurrentHashMap<>();
    }
    
    /**
     * Adds a UTXO to the set
     * @param utxo UTXO to add
     */
    public void addUTXO(UTXO utxo) {
        utxos.put(utxo.getId(), utxo);
    }
    
    /**
     * Removes a UTXO from the set (spent)
     * @param utxoId UTXO ID to remove
     * @return Removed UTXO, or null if not found
     */
    public UTXO removeUTXO(String utxoId) {
        return utxos.remove(utxoId);
    }
    
    /**
     * Gets a UTXO by ID
     * @param utxoId UTXO ID
     * @return UTXO object, or null if not found
     */
    public UTXO getUTXO(String utxoId) {
        return utxos.get(utxoId);
    }
    
    /**
     * Checks if a UTXO exists (is unspent)
     * @param utxoId UTXO ID
     * @return true if exists
     */
    public boolean contains(String utxoId) {
        return utxos.containsKey(utxoId);
    }
    
    /**
     * Gets all UTXOs owned by a specific public key
     * @param publicKey Owner's public key
     * @return List of owned UTXOs
     */
    public List<UTXO> getUTXOsForPublicKey(PublicKey publicKey) {
        List<UTXO> owned = new ArrayList<>();
        for (UTXO utxo : utxos.values()) {
            if (utxo.isMine(publicKey)) {
                owned.add(utxo);
            }
        }
        return owned;
    }
    
    /**
     * Calculates balance for a public key
     * @param publicKey Owner's public key
     * @return Total balance in JAVACoin
     */
    public double getBalance(PublicKey publicKey) {
        double balance = 0.0;
        for (UTXO utxo : utxos.values()) {
            if (utxo.isMine(publicKey)) {
                balance += utxo.getValue();
            }
        }
        return balance;
    }
    
    /**
     * Gets all UTXOs in the set
     * @return List of all UTXOs
     */
    public List<UTXO> getAllUTXOs() {
        return new ArrayList<>(utxos.values());
    }
    
    /**
     * Gets total number of UTXOs
     * @return UTXO count
     */
    public int size() {
        return utxos.size();
    }
    
    /**
     * Clears all UTXOs (used for chain reorganization)
     */
    public void clear() {
        utxos.clear();
    }
    
    /**
     * Gets total value of all UTXOs in the system
     * @return Total JAVACoin in circulation
     */
    public double getTotalValue() {
        double total = 0.0;
        for (UTXO utxo : utxos.values()) {
            total += utxo.getValue();
        }
        return total;
    }
    
    /**
     * Creates a deep copy of the UTXO set
     * @return Cloned UTXO set
     */
    public UTXOSet clone() {
        UTXOSet copy = new UTXOSet();
        copy.utxos = new ConcurrentHashMap<>(this.utxos);
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("UTXOSet{size=%d, totalValue=%.2f JAC}", 
                           size(), getTotalValue());
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════════╗\n");
        sb.append("║         Global UTXO Set (Ledger)               ║\n");
        sb.append("╠════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Total UTXOs: %-34d ║\n", size()));
        sb.append(String.format("║ Total Value: %-30.2f JAC ║\n", getTotalValue()));
        sb.append("╚════════════════════════════════════════════════╝");
        return sb.toString();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.