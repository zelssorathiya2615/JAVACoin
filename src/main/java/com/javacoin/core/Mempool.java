package com.javacoin.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Mempool - Memory pool of unconfirmed transactions
 * Thread-safe queue that miners pull from to build blocks
 */
public class Mempool {
    
    private ConcurrentLinkedQueue<Transaction> transactions;
    
    /**
     * Creates empty mempool
     */
    public Mempool() {
        this.transactions = new ConcurrentLinkedQueue<>();
    }
    
    /**
     * Adds a transaction to the mempool
     * @param transaction Transaction to add
     * @return true if added successfully
     */
    public boolean addTransaction(Transaction transaction) {
        // Check if transaction already exists
        if (contains(transaction.getTransactionId())) {
            System.out.println("Transaction already in mempool: " + transaction.getTransactionId());
            return false;
        }
        
        transactions.add(transaction);
        System.out.println("✅ Transaction added to mempool: " + transaction.getTransactionId().substring(0, 10) + "...");
        return true;
    }
    
    /**
     * Removes a transaction from the mempool
     * @param transactionId Transaction ID
     * @return true if removed
     */
    public boolean removeTransaction(String transactionId) {
        return transactions.removeIf(tx -> tx.getTransactionId().equals(transactionId));
    }
    
    /**
     * Removes multiple transactions (after block confirmation)
     * @param transactionIds List of transaction IDs to remove
     */
    public void removeTransactions(List<String> transactionIds) {
        for (String txId : transactionIds) {
            removeTransaction(txId);
        }
    }
    
    /**
     * Checks if mempool contains a transaction
     * @param transactionId Transaction ID
     * @return true if exists
     */
    public boolean contains(String transactionId) {
        return transactions.stream()
            .anyMatch(tx -> tx.getTransactionId().equals(transactionId));
    }
    
    /**
     * Gets a transaction by ID
     * @param transactionId Transaction ID
     * @return Transaction, or null if not found
     */
    public Transaction getTransaction(String transactionId) {
        return transactions.stream()
            .filter(tx -> tx.getTransactionId().equals(transactionId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets all transactions sorted by fee (highest first)
     * Miners use this to prioritize high-fee transactions
     * @return Sorted list of transactions
     */
    public List<Transaction> getTransactionsSortedByFee() {
        return transactions.stream()
            .sorted(Comparator.comparingDouble(Transaction::getFee).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Gets top N transactions by fee
     * @param maxTransactions Maximum number of transactions
     * @return List of highest-fee transactions
     */
    public List<Transaction> getTopTransactions(int maxTransactions) {
        return getTransactionsSortedByFee().stream()
            .limit(maxTransactions)
            .collect(Collectors.toList());
    }
    
    /**
     * Validates all transactions against current UTXO set
     * Removes invalid transactions
     * @param utxoSet Current UTXO set
     * @return Number of invalid transactions removed
     */
    public int validateAndClean(UTXOSet utxoSet) {
        int removed = 0;
        List<Transaction> toRemove = new ArrayList<>();
        
        for (Transaction tx : transactions) {
            if (!tx.isValid(utxoSet)) {
                toRemove.add(tx);
                removed++;
            }
        }
        
        transactions.removeAll(toRemove);
        
        if (removed > 0) {
            System.out.println("🗑️  Removed " + removed + " invalid transactions from mempool");
        }
        
        return removed;
    }
    
    /**
     * Gets total fees for all transactions in mempool
     * @return Total fees
     */
    public double getTotalFees() {
        return transactions.stream()
            .mapToDouble(Transaction::getFee)
            .sum();
    }
    
    /**
     * Gets mempool size
     * @return Number of transactions
     */
    public int size() {
        return transactions.size();
    }
    
    /**
     * Checks if mempool is empty
     * @return true if no transactions
     */
    public boolean isEmpty() {
        return transactions.isEmpty();
    }
    
    /**
     * Clears all transactions
     */
    public void clear() {
        transactions.clear();
    }
    
    /**
     * Gets all transactions as a list
     * @return List of all transactions
     */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }
    
    @Override
    public String toString() {
        return String.format("Mempool{size=%d, totalFees=%.2f JAC}", 
                           size(), getTotalFees());
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════════╗\n");
        sb.append("║              Mempool (Pending Transactions)           ║\n");
        sb.append("╠═══════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Pending Transactions: %-31d ║\n", size()));
        sb.append(String.format("║ Total Fees: %-37.2f JAC ║\n", getTotalFees()));
        sb.append("╠═══════════════════════════════════════════════════════╣\n");
        
        if (isEmpty()) {
            sb.append("║                    (empty)                            ║\n");
        } else {
            List<Transaction> sorted = getTransactionsSortedByFee();
            int displayCount = Math.min(5, sorted.size());
            sb.append("║ Top Transactions by Fee:                              ║\n");
            
            for (int i = 0; i < displayCount; i++) {
                Transaction tx = sorted.get(i);
                String txIdShort = tx.getTransactionId().substring(0, 8);
                sb.append(String.format("║ %d. %s... | Fee: %-8.2f JAC                  ║\n", 
                                      i + 1, txIdShort, tx.getFee()));
            }
            
            if (sorted.size() > displayCount) {
                sb.append(String.format("║ ... and %d more                                       ║\n", 
                                      sorted.size() - displayCount));
            }
        }
        
        sb.append("╚═══════════════════════════════════════════════════════╝");
        return sb.toString();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.