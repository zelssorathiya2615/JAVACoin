package com.javacoin.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Blockchain - The complete chain of blocks
 * Maintains consensus rules and validates chain integrity
 */
public class Blockchain {
    
    private List<Block> chain;
    private int difficulty;
    private double blockReward;
    
    /**
     * Creates new blockchain with genesis block
     * @param genesisBlock First block in chain
     * @param difficulty Mining difficulty
     * @param blockReward Block subsidy amount
     */
    public Blockchain(Block genesisBlock, int difficulty, double blockReward) {
        this.chain = new ArrayList<>();
        this.difficulty = difficulty;
        this.blockReward = blockReward;
        this.chain.add(genesisBlock);
    }
    
    /**
     * Adds a new block to the chain (synchronized for thread safety)
     * This is the critical consensus method - only one thread can add at a time
     * @param block Block to add
     * @return true if block was added successfully
     */
    public synchronized boolean addBlockToChain(Block block) {
        // Verify block index is sequential
        if (block.getIndex() != chain.size()) {
            System.err.println("Block index mismatch. Expected: " + chain.size() + ", Got: " + block.getIndex());
            return false;
        }
        
        // Verify previous hash matches
        Block lastBlock = getLatestBlock();
        if (!block.getPreviousHash().equals(lastBlock.getHash())) {
            System.err.println("Previous hash mismatch");
            return false;
        }
        
        // Validate block structure and PoW
        if (!block.isValid()) {
            System.err.println("Block validation failed");
            return false;
        }
        
        // Add block to chain
        chain.add(block);
        System.out.println("✅ Block " + block.getIndex() + " added to chain");
        return true;
    }
    
    /**
     * Gets the latest block in the chain
     * @return Latest block
     */
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }
    
    /**
     * Gets a block by index
     * @param index Block height
     * @return Block, or null if not found
     */
    public Block getBlock(int index) {
        if (index >= 0 && index < chain.size()) {
            return chain.get(index);
        }
        return null;
    }
    
    /**
     * Gets current chain height
     * @return Number of blocks in chain
     */
    public int getHeight() {
        return chain.size();
    }
    
    /**
     * Validates entire blockchain integrity
     * @return true if chain is valid
     */
    public boolean isChainValid() {
        System.out.println("🔍 Validating blockchain...");
        
        // Check genesis block
        Block genesis = chain.get(0);
        if (genesis.getIndex() != 0) {
            System.err.println("Genesis block has invalid index");
            return false;
        }
        
        // Validate each block and its link to previous
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);
            
            // Validate block itself
            if (!currentBlock.isValid()) {
                System.err.println("Block " + i + " is invalid");
                return false;
            }
            
            // Check hash chain continuity
            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                System.err.println("Block " + i + " has invalid previous hash");
                return false;
            }
            
            // Check sequential index
            if (currentBlock.getIndex() != i) {
                System.err.println("Block " + i + " has invalid index: " + currentBlock.getIndex());
                return false;
            }
            
            // Verify hash matches calculated hash
            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                System.err.println("Block " + i + " hash has been tampered with");
                return false;
            }
        }
        
        System.out.println("✅ Blockchain is valid!");
        return true;
    }
    
    /**
     * Rebuilds UTXO set from scratch by replaying all blocks
     * Used after chain reorganization or node restart
     * @return Reconstructed UTXO set
     */
    public UTXOSet rebuildUTXOSet() {
        UTXOSet utxoSet = new UTXOSet();
        
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                tx.processTransaction(utxoSet);
            }
        }
        
        return utxoSet;
    }
    
    /**
     * Gets all transactions from the entire blockchain
     * @return List of all transactions
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> allTx = new ArrayList<>();
        for (Block block : chain) {
            allTx.addAll(block.getTransactions());
        }
        return allTx;
    }
    
    /**
     * Finds a transaction by ID across all blocks
     * @param txId Transaction ID
     * @return Transaction, or null if not found
     */
    public Transaction findTransaction(String txId) {
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (tx.getTransactionId().equals(txId)) {
                    return tx;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets confirmation count for a transaction
     * @param txId Transaction ID
     * @return Number of blocks built on top of the block containing this tx
     */
    public int getConfirmationCount(String txId) {
        for (int i = 0; i < chain.size(); i++) {
            Block block = chain.get(i);
            for (Transaction tx : block.getTransactions()) {
                if (tx.getTransactionId().equals(txId)) {
                    // Confirmations = blocks mined after this block
                    return chain.size() - i;
                }
            }
        }
        return 0; // Transaction not found
    }
    
    /**
     * Compares this chain with another (for fork resolution)
     * @param otherChain Another blockchain
     * @return true if this chain is longer
     */
    public boolean isLongerThan(Blockchain otherChain) {
        return this.chain.size() > otherChain.chain.size();
    }
    
    /**
     * Replaces this chain with another (used in fork resolution)
     * @param otherChain Chain to adopt
     */
    public synchronized void replaceChain(Blockchain otherChain) {
        if (!otherChain.isChainValid()) {
            System.err.println("❌ Cannot replace with invalid chain");
            return;
        }
        
        if (otherChain.getHeight() <= this.getHeight()) {
            System.out.println("ℹ️  Other chain not longer, keeping current chain");
            return;
        }
        
        System.out.println("⚠️  FORK RESOLUTION: Replacing chain!");
        System.out.println("   Old height: " + this.getHeight());
        System.out.println("   New height: " + otherChain.getHeight());
        
        this.chain = new ArrayList<>(otherChain.getChain());
        
        System.out.println("✅ Chain replaced (longest chain wins)");
    }
    
    /**
     * Gets total mining rewards issued (sum of all coinbase outputs)
     * @return Total issued rewards
     */
    public double getTotalRewardsIssued() {
        double total = 0.0;
        for (Block block : chain) {
            Transaction coinbase = block.getCoinbaseTransaction();
            if (coinbase != null) {
                total += coinbase.getOutputValue();
            }
        }
        return total;
    }
    
    // Getters
    
    public List<Block> getChain() {
        return new ArrayList<>(chain); // Return copy for safety
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
    
    public double getBlockReward() {
        return blockReward;
    }
    
    @Override
    public String toString() {
        return String.format("Blockchain{height=%d, difficulty=%d, latestHash='%s...'}", 
                           chain.size(), 
                           difficulty,
                           getLatestBlock().getHash().substring(0, 10));
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════════╗\n");
        sb.append("║                  JAVACoin Blockchain                  ║\n");
        sb.append("╠═══════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Height: %-45d ║\n", chain.size()));
        sb.append(String.format("║ Difficulty: %-41d ║\n", difficulty));
        sb.append(String.format("║ Block Reward: %-35.2f JAC ║\n", blockReward));
        sb.append(String.format("║ Total Rewards Issued: %-27.2f JAC ║\n", getTotalRewardsIssued()));
        sb.append(String.format("║ Latest Block Hash: %s...║\n", getLatestBlock().getHash().substring(0, 20)));
        sb.append("╚═══════════════════════════════════════════════════════╝");
        return sb.toString();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.