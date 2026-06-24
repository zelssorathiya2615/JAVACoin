package com.javacoin.mining;

import com.javacoin.core.*;
import com.javacoin.crypto.Wallet;
import com.javacoin.network.Message;
import com.javacoin.network.PeerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Miner - Background thread that performs Proof-of-Work mining
 * Competes with other miners to find valid blocks
 * FIXED: Properly broadcasts blocks and transactions to network
 */
public class Miner extends Thread {
    
    private Blockchain blockchain;
    private UTXOSet utxoSet;
    private Mempool mempool;
    private Wallet minerWallet;
    private PeerManager peerManager;
    private String nodeId;
    
    private volatile boolean mining;
    private volatile boolean running;
    
    // Mining statistics
    private int blocksFound;
    private int staleBlocks;
    
    // Configuration
    private int maxTransactionsPerBlock = 10;
    private double blockReward = 50.0;
    
    public Miner(Blockchain blockchain, UTXOSet utxoSet, Mempool mempool,
                Wallet minerWallet, PeerManager peerManager, String nodeId) {
        this.blockchain = blockchain;
        this.utxoSet = utxoSet;
        this.mempool = mempool;
        this.minerWallet = minerWallet;
        this.peerManager = peerManager;
        this.nodeId = nodeId;
        
        this.mining = false;
        this.running = true;
        this.blocksFound = 0;
        this.staleBlocks = 0;
        
        this.setName("Miner-" + nodeId);
        this.setDaemon(true);
    }
    
    @Override
    public void run() {
        System.out.println("⛏️ Miner thread started for " + nodeId);
        
        while (running) {
            if (mining) {
                try {
                    mineNextBlock();
                } catch (Exception e) {
                    System.err.println("❌ Mining error: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Sleep when not mining
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
        }
        
        System.out.println("🛑 Miner thread stopped");
    }
    
    /**
     * Mines the next block - FIXED VERSION
     */
    private void mineNextBlock() {
        // Build candidate block
        Block candidateBlock = buildCandidateBlock();
        if (candidateBlock == null) {
            // Nothing to mine, wait
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                if (!running) return;
            }
            return;
        }
        
        System.out.println("⛏️ [" + nodeId + "] Mining Block #" + candidateBlock.getIndex() + 
                         " with " + candidateBlock.getTransactionCount() + " transactions");
        
        // Mine the block (this is CPU-intensive and takes 3-5 seconds)
        candidateBlock.mineBlock();
        
        // Check if still valid (another miner might have found a block first)
        synchronized (blockchain) {
            if (candidateBlock.getIndex() != blockchain.getHeight()) {
                System.out.println("❌ [" + nodeId + "] Block #" + candidateBlock.getIndex() + 
                                 " already mined by competitor (stale)");
                staleBlocks++;
                return;
            }
            
            // Try to add to local chain
            boolean added = blockchain.addBlockToChain(candidateBlock);
            
            if (added) {
                blocksFound++;
                
                System.out.println("✅ [" + nodeId + "] BLOCK FOUND!");
                System.out.println("   Reward: " + (blockReward + candidateBlock.getTotalFees()) + " JAC");
                
                // CRITICAL FIX: Process transactions BEFORE broadcasting
                System.out.println("🔄 [" + nodeId + "] Processing " + candidateBlock.getTransactions().size() + " transactions locally...");
                for (Transaction tx : candidateBlock.getTransactions()) {
                    tx.processTransaction(utxoSet);
                    mempool.removeTransaction(tx.getTransactionId());
                    
                    if (tx.isCoinbase()) {
                        System.out.println("   🪙 Coinbase: " + tx.getOutputValue() + " JAC");
                    } else {
                        System.out.println("   💸 TX: " + tx.getTransactionId().substring(0, 10) + "... (fee: " + tx.getFee() + " JAC)");
                    }
                }
                
                System.out.println("💰 [" + nodeId + "] UTXO set updated. Total: " + utxoSet.getTotalValue() + " JAC");
                
                // CRITICAL FIX: Broadcast block to ALL peers
                Message blockMessage = Message.newBlock(candidateBlock, nodeId);
                int sent = peerManager.broadcast(blockMessage);
                System.out.println("📡 [" + nodeId + "] Block #" + candidateBlock.getIndex() + " broadcast to " + sent + " peers");
                
                // Give network time to propagate
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Ignore
                }
                
            } else {
                System.err.println("❌ [" + nodeId + "] Failed to add own block to chain");
                staleBlocks++;
            }
        }
    }
    
    /**
     * Builds a candidate block from mempool transactions
     */
    private Block buildCandidateBlock() {
        synchronized (blockchain) {
            // Get latest block
            Block previousBlock = blockchain.getLatestBlock();
            int newIndex = previousBlock.getIndex() + 1;
            String previousHash = previousBlock.getHash();
            
            // Get highest-fee transactions from mempool
            List<Transaction> selectedTransactions = new ArrayList<>();
            
            // Validate and select transactions
            List<Transaction> sortedTx = mempool.getTransactionsSortedByFee();
            UTXOSet tempUtxoSet = utxoSet.clone(); // Clone to avoid modifying real set
            
            double totalFees = 0.0;
            for (Transaction tx : sortedTx) {
                if (selectedTransactions.size() >= maxTransactionsPerBlock) {
                    break;
                }
                
                // Re-validate against temp UTXO set
                if (tx.isValid(tempUtxoSet)) {
                    selectedTransactions.add(tx);
                    totalFees += tx.getFee();
                    
                    // Update temp UTXO set
                    tx.processTransaction(tempUtxoSet);
                }
            }
            
            // Create coinbase transaction
            Transaction coinbase = Transaction.createCoinbase(
                minerWallet.getPublicKey(), 
                blockReward, 
                totalFees
            );

            // DEBUG: Verify coinbase is correct
            System.out.println("   💰 Coinbase created: " + coinbase.getOutputValue() + " JAC");

            // Build block with coinbase first
            List<Transaction> blockTransactions = new ArrayList<>();
            blockTransactions.add(coinbase);
            blockTransactions.addAll(selectedTransactions);
            
            // Create block
            Block block = new Block(newIndex, previousHash, blockTransactions, blockchain.getDifficulty());
            
            return block;
        }
    }
    
    /**
     * Starts mining
     */
    public void startMining() {
        if (!mining) {
            mining = true;
            System.out.println("▶️ [" + nodeId + "] Mining started");
        }
    }
    
    /**
     * Stops mining
     */
    public void stopMining() {
        if (mining) {
            mining = false;
            System.out.println("⏸️ [" + nodeId + "] Mining paused");
        }
    }
    
    /**
     * Shuts down miner thread
     */
    public void shutdown() {
        running = false;
        mining = false;
        this.interrupt();
    }
    
    // Getters
    
    public boolean isMining() {
        return mining;
    }
    
    public int getBlocksFound() {
        return blocksFound;
    }
    
    public int getStaleBlocks() {
        return staleBlocks;
    }
    
    public double getEstimatedReward() {
        return blockReward + mempool.getTotalFees();
    }
    
    /**
     * Gets mining statistics
     */
    public String getStatistics() {
        return String.format(
            "⛏️ Mining Stats [%s]:\n" +
            "   Status: %s\n" +
            "   Blocks Found: %d\n" +
            "   Stale Blocks: %d\n" +
            "   Win Rate: %.1f%%\n" +
            "   Estimated Reward: %.2f JAC",
            nodeId,
            mining ? "ACTIVE" : "PAUSED",
            blocksFound,
            staleBlocks,
            (blocksFound + staleBlocks) > 0 ? (blocksFound * 100.0 / (blocksFound + staleBlocks)) : 0.0,
            getEstimatedReward()
        );
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.