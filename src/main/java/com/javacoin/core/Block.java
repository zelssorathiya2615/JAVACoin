package com.javacoin.core;

import com.javacoin.crypto.CryptoUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Block - Contains transactions and Proof-of-Work
 * Each block is cryptographically linked to the previous block
 */
public class Block implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Block Header
    private int index;                          // Block height in chain
    private String hash;                        // This block's hash
    private String previousHash;                // Previous block's hash
    private String merkleRoot;                  // Root of transaction merkle tree
    private long timestamp;                     // When block was mined
    private int nonce;                          // Proof-of-Work nonce
    private int difficulty;                     // Mining difficulty (leading zeros)
    
    // Block Body
    private List<Transaction> transactions;     // All transactions in block
    
    // Mining metadata (transient - not serialized)
    private transient long miningStartTime;
    private transient int hashAttempts;
    
    /**
     * Creates a new block
     * @param index Block height
     * @param previousHash Hash of previous block
     * @param transactions Transactions to include
     * @param difficulty Mining difficulty
     */
    public Block(int index, String previousHash, List<Transaction> transactions, int difficulty) {
        this.index = index;
        this.previousHash = previousHash;
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        this.difficulty = difficulty;
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
        
        // Calculate merkle root from transaction IDs
        List<String> txIds = this.transactions.stream()
            .map(Transaction::getTransactionId)
            .collect(Collectors.toList());
        this.merkleRoot = CryptoUtil.getMerkleRoot(txIds);
        
        // Hash is calculated during mining
        this.hash = "";
    }
    
    /**
     * Creates the Genesis Block (first block in chain)
     * @param minerPublicKey Miner's public key for reward
     * @param blockReward Initial block reward
     * @return Genesis block
     */
    public static Block createGenesisBlock(java.security.PublicKey minerPublicKey, double blockReward) {
        List<Transaction> transactions = new ArrayList<>();
        
        // Genesis block has one coinbase transaction
        Transaction coinbase = Transaction.createCoinbase(minerPublicKey, blockReward, 0.0);
        transactions.add(coinbase);
        
        Block genesis = new Block(0, "0", transactions, 4);
        genesis.mineBlock(); // Mine the genesis block
        
        return genesis;
    }
    
    /**
     * Calculates hash of block header data
     * @return SHA-256 hash
     */
    public String calculateHash() {
        String data = index +
                     previousHash +
                     merkleRoot +
                     timestamp +
                     nonce +
                     difficulty;
        return CryptoUtil.applySHA256(data);
    }
    
    /**
     * Mines the block using Proof-of-Work
     * Increments nonce until hash meets difficulty target
     */
    public void mineBlock() {
        miningStartTime = System.currentTimeMillis();
        hashAttempts = 0;
        
        String target = CryptoUtil.getDifficultyTarget(difficulty);
        
        System.out.println("⛏️  Mining block " + index + " with difficulty " + difficulty + "...");
        System.out.println("   Target: " + target + "...");
        
        do {
            nonce++;
            hash = calculateHash();
            hashAttempts++;
            
            // Progress indicator every 100,000 attempts
            if (hashAttempts % 100000 == 0) {
                System.out.println("   Attempts: " + hashAttempts + " | Current hash: " + hash.substring(0, 10) + "...");
            }
            
        } while (!CryptoUtil.hashMeetsDifficulty(hash, difficulty));
        
        long miningTime = System.currentTimeMillis() - miningStartTime;
        double hashRate = hashAttempts / (miningTime / 1000.0);
        
        System.out.println("✅ Block mined successfully!");
        System.out.println("   Hash: " + hash);
        System.out.println("   Nonce: " + nonce);
        System.out.println("   Attempts: " + hashAttempts);
        System.out.println("   Time: " + miningTime + " ms");
        System.out.println("   Hash Rate: " + String.format("%.2f", hashRate) + " H/s");
        System.out.println();
    }
    
    /**
     * Validates block structure and proof-of-work
     * @return true if block is valid
     */
    public boolean isValid() {
        // Check if hash matches calculated hash
        if (!hash.equals(calculateHash())) {
            System.err.println("Block hash doesn't match calculated hash");
            return false;
        }
        
        // Check if hash meets difficulty
        if (!CryptoUtil.hashMeetsDifficulty(hash, difficulty)) {
            System.err.println("Block hash doesn't meet difficulty target");
            return false;
        }
        
        // Check merkle root
        List<String> txIds = transactions.stream()
            .map(Transaction::getTransactionId)
            .collect(Collectors.toList());
        String calculatedMerkleRoot = CryptoUtil.getMerkleRoot(txIds);
        
        if (!merkleRoot.equals(calculatedMerkleRoot)) {
            System.err.println("Merkle root mismatch");
            return false;
        }
        
        // Validate all transactions
        for (Transaction tx : transactions) {
            if (!tx.verifySignature() && !tx.isCoinbase()) {
                System.err.println("Invalid transaction signature: " + tx.getTransactionId());
                return false;
            }
        }
        
        // Check that first transaction is coinbase
        if (!transactions.isEmpty() && !transactions.get(0).isCoinbase()) {
            System.err.println("First transaction must be coinbase");
            return false;
        }
        
        // Check that only first transaction is coinbase
        for (int i = 1; i < transactions.size(); i++) {
            if (transactions.get(i).isCoinbase()) {
                System.err.println("Non-first transaction is coinbase");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets total fees from all transactions in block
     * @return Total transaction fees
     */
    public double getTotalFees() {
        double total = 0.0;
        for (Transaction tx : transactions) {
            if (!tx.isCoinbase()) {
                total += tx.getFee();
            }
        }
        return total;
    }
    
    /**
     * Gets coinbase transaction (mining reward)
     * @return Coinbase transaction, or null if not found
     */
    public Transaction getCoinbaseTransaction() {
        if (!transactions.isEmpty() && transactions.get(0).isCoinbase()) {
            return transactions.get(0);
        }
        return null;
    }
    
    // Getters
    
    public int getIndex() {
        return index;
    }
    
    public String getHash() {
        return hash;
    }
    
    public String getPreviousHash() {
        return previousHash;
    }
    
    public String getMerkleRoot() {
        return merkleRoot;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getNonce() {
        return nonce;
    }
    
    public void setNonce(int nonce) {
        this.nonce = nonce;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public int getTransactionCount() {
        return transactions.size();
    }
    
    @Override
    public String toString() {
        return String.format("Block{index=%d, hash='%s...', txCount=%d, difficulty=%d}", 
                           index, 
                           hash.substring(0, Math.min(10, hash.length())),
                           transactions.size(),
                           difficulty);
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════════╗\n");
        sb.append(String.format("║               Block #%-6d                           ║\n", index));
        sb.append("╠═══════════════════════════════════════════════════════╣\n");
        
        // Safe substring for hash
        String hashDisplay = hash.length() >= 30 ? hash.substring(0, 30) : hash;
        sb.append(String.format("║ Hash: %-47s║\n", hashDisplay + "..."));
        
        // Safe substring for previous hash (handle genesis "0")
        String prevHashDisplay;
        if (previousHash.equals("0")) {
            prevHashDisplay = "GENESIS (no previous block)    ";
        } else if (previousHash.length() >= 27) {
            prevHashDisplay = previousHash.substring(0, 27) + "...";
        } else {
            prevHashDisplay = previousHash;
        }
        sb.append(String.format("║ Previous: %-43s║\n", prevHashDisplay));
        
        // Safe substring for merkle root
        String merkleDisplay = merkleRoot.length() >= 24 ? merkleRoot.substring(0, 24) : merkleRoot;
        sb.append(String.format("║ Merkle Root: %-40s║\n", merkleDisplay + "..."));
        
        sb.append(String.format("║ Timestamp: %-42d ║\n", timestamp));
        sb.append(String.format("║ Nonce: %-46d ║\n", nonce));
        sb.append(String.format("║ Difficulty: %-42d ║\n", difficulty));
        sb.append(String.format("║ Transactions: %-39d ║\n", transactions.size()));
        sb.append(String.format("║ Total Fees: %-37.2f JAC ║\n", getTotalFees()));
        sb.append("╚═══════════════════════════════════════════════════════╝");
        return sb.toString();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.