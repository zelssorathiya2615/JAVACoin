package com.javacoin.network;

import com.javacoin.core.*;

import java.util.concurrent.BlockingQueue;

/**
 * MessageHandler - Processes incoming network messages
 * UPDATED: Added chain synchronization support and fork resolution
 */
public class MessageHandler extends Thread {
    
    private BlockingQueue<Message> messageQueue;
    private Blockchain blockchain;
    private UTXOSet utxoSet;
    private Mempool mempool;
    private PeerManager peerManager;
    private ChainSynchronizer chainSynchronizer;  // NEW: Chain sync handler
    private volatile boolean running;
    
    /**
     * Creates message handler
     * @param messageQueue Queue of incoming messages
     * @param blockchain Local blockchain
     * @param utxoSet Local UTXO set
     * @param mempool Local mempool
     * @param peerManager Peer manager for forwarding
     */
    public MessageHandler(BlockingQueue<Message> messageQueue,
                         Blockchain blockchain,
                         UTXOSet utxoSet,
                         Mempool mempool,
                         PeerManager peerManager) {
        this.messageQueue = messageQueue;
        this.blockchain = blockchain;
        this.utxoSet = utxoSet;
        this.mempool = mempool;
        this.peerManager = peerManager;
        this.running = true;
        this.setName("MessageHandler");
        this.setDaemon(true);
    }
    
    /**
     * Set chain synchronizer (called after MessageHandler creation)
     */
    public void setChainSynchronizer(ChainSynchronizer chainSynchronizer) {
        this.chainSynchronizer = chainSynchronizer;
    }
    
    @Override
    public void run() {
        System.out.println("📬 MessageHandler started");
        
        while (running) {
            try {
                // Block until message arrives
                Message message = messageQueue.take();
                
                // Process message based on type
                handleMessage(message);
                
            } catch (InterruptedException e) {
                if (!running) {
                    break; // Normal shutdown
                }
                System.err.println("⚠️ MessageHandler interrupted: " + e.getMessage());
            }
        }
        
        System.out.println("🛑 MessageHandler stopped");
    }
    
    /**
     * Processes a single message
     * @param message Message to handle
     */
    private void handleMessage(Message message) {
        System.out.println("📥 Processing: " + message.getDisplayInfo());
        
        switch (message.getType()) {
            case NEW_TRANSACTION:
                handleNewTransaction(message);
                break;
                
            case NEW_BLOCK:
                handleNewBlock(message);
                break;
                
            case REQUEST_CHAIN:
                handleChainRequest(message);
                break;
                
            case RESPONSE_CHAIN:
                handleChainResponse(message);
                break;
                
            case DIFFICULTY_UPDATE:
                handleDifficultyUpdate(message);
                break;
                
            case PING:
                handlePing(message);
                break;
                
            case PONG:
                System.out.println("🏓 PONG received from " + message.getSenderId());
                break;
                
            default:
                System.out.println("⚠️ Unhandled message type: " + message.getType());
        }
    }
    
    /**
     * Handles incoming transaction
     * @param message Transaction message
     */
    private void handleNewTransaction(Message message) {
        Transaction tx = message.getTransaction();
        if (tx == null) {
            System.err.println("❌ Invalid transaction message");
            return;
        }
        
        // Check if already in mempool
        if (mempool.contains(tx.getTransactionId())) {
            System.out.println("ℹ️ Transaction already in mempool");
            return;
        }
        
        // Validate transaction
        if (!tx.isValid(utxoSet)) {
            System.err.println("❌ Invalid transaction received: " + tx.getTransactionId());
            return;
        }
        
        // Add to mempool
        mempool.addTransaction(tx);
        System.out.println("✅ Transaction added to mempool: " + 
                         tx.getTransactionId().substring(0, 10) + "...");
    }
    
    /**
     * Handles incoming block - WITH FORK RESOLUTION
     * @param message Block message
     */
    private void handleNewBlock(Message message) {
        Block block = message.getBlock();
        if (block == null) {
            System.err.println("❌ Invalid block message");
            return;
        }
        
        System.out.println("📦 Received Block #" + block.getIndex() + 
                        " from " + message.getSenderId());
        
        // Check if block is next in sequence
        if (block.getIndex() != blockchain.getHeight()) {
            if (block.getIndex() > blockchain.getHeight()) {
                System.out.println("   ⚠️  We're behind! Our height: " + blockchain.getHeight() + 
                                ", received: " + block.getIndex());
                if (chainSynchronizer != null) {
                    chainSynchronizer.handleOutOfOrderBlock(block, message.getSenderId());
                }
            } else {
                System.out.println("   ℹ️  Old block (already have), ignoring");
            }
            return;
        }
        
        // CRITICAL: Process block atomically with UTXO updates
        synchronized (blockchain) {
            synchronized (utxoSet) {
                
                // BEFORE processing - debug current state
                System.out.println("\n⏸️  BEFORE processing block #" + block.getIndex());
                // Get Node reference through some means - you'll need to add this
                // For now, just show UTXO count
                System.out.println("   Current UTXOs: " + utxoSet.size());
                System.out.println("   Current Value: " + utxoSet.getTotalValue() + " JAC");
                
                // Try to add block to chain
                if (blockchain.addBlockToChain(block)) {
                    System.out.println("✅ Block #" + block.getIndex() + " accepted");
                    
                    // Process all transactions
                    System.out.println("🔄 Processing " + block.getTransactions().size() + " transactions...");
                    
                    for (Transaction tx : block.getTransactions()) {
                        // DEBUG: Show transaction before processing
                        System.out.println("\n   Processing TX: " + tx.getTransactionId().substring(0, 10) + "...");
                        System.out.println("      Type: " + (tx.isCoinbase() ? "COINBASE" : "REGULAR"));
                        System.out.println("      Inputs: " + tx.getInputs().size() + 
                                        " | Outputs: " + tx.getOutputs().size());
                        
                        // Process transaction (updates UTXO set)
                        tx.processTransaction(utxoSet);
                        
                        // Remove from mempool if present
                        mempool.removeTransaction(tx.getTransactionId());
                        
                        // Debug output
                        if (tx.isCoinbase()) {
                            System.out.println("   🪙 Coinbase: " + tx.getOutputValue() + " JAC");
                        } else {
                            System.out.println("   💸 TX: " + tx.getTransactionId().substring(0, 10) + 
                                            "... (fee: " + tx.getFee() + " JAC)");
                            
                            // Show where money is going
                            for (TransactionOutput output : tx.getOutputs()) {
                                System.out.println("      → " + output.getValue() + " JAC");
                            }
                        }
                    }
                    
                    // AFTER processing - show final state
                    System.out.println("\n▶️  AFTER processing block #" + block.getIndex());
                    System.out.println("   Final UTXOs: " + utxoSet.size());
                    System.out.println("   Final Value: " + utxoSet.getTotalValue() + " JAC");
                    
                    System.out.println("\n💰 UTXO Set updated. Total: " + 
                                    utxoSet.getTotalValue() + " JAC (" + 
                                    utxoSet.size() + " UTXOs)");
                    
                } else {
                    System.err.println("❌ Block #" + block.getIndex() + " rejected");
                }
            }
        }
    }
    // Add this debug method to MessageHandler
    private void debugAllBalances() {
        System.out.println("   📊 Current Balances:");
        // You'll need to track all known public keys
        // For now, just show UTXO count
        System.out.println("      Total UTXOs in system: " + utxoSet.size());
        System.out.println("      Total value: " + utxoSet.getTotalValue() + " JAC");
    }   
    /**
     * Handle request for our blockchain
     */
    private void handleChainRequest(Message message) {
        System.out.println("📤 Chain request from " + message.getSenderId());
        
        if (chainSynchronizer != null) {
            chainSynchronizer.handleChainRequest(message.getSenderId());
        } else {
            System.err.println("   ❌ ChainSynchronizer not initialized!");
        }
    }
    
    /**
     * Handle incoming blockchain from peer
     */
    private void handleChainResponse(Message message) {
        Blockchain peerChain = message.getBlockchain();
        
        if (peerChain == null) {
            System.err.println("❌ Invalid chain response");
            return;
        }
        
        if (chainSynchronizer != null) {
            chainSynchronizer.handleChainResponse(peerChain, message.getSenderId());
        } else {
            System.err.println("   ❌ ChainSynchronizer not initialized!");
        }
    }
    
    /**
     * Handles difficulty adjustment
     * @param message Difficulty update message
     */
    private void handleDifficultyUpdate(Message message) {
        Integer newDifficulty = message.getIntegerPayload();
        if (newDifficulty == null) {
            System.err.println("❌ Invalid difficulty update message");
            return;
        }
        
        System.out.println("⚙️ Difficulty update received: " + 
                         blockchain.getDifficulty() + " → " + newDifficulty);
        
        blockchain.setDifficulty(newDifficulty);
        System.out.println("✅ Mining difficulty updated to " + newDifficulty);
    }
    
    /**
     * Handles ping request
     * @param message Ping message
     */
    private void handlePing(Message message) {
        System.out.println("🏓 PING received from " + message.getSenderId());
            
        // In a full implementation, would send pong back to sender
        // For now, just acknowledge the ping
        System.out.println("🏓 PONG acknowledged");
    }
    
    /**
     * Stops the message handler
     */
    public void shutdown() {
        running = false;
        this.interrupt();
    }
    
    /**
     * Checks if handler is running
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.