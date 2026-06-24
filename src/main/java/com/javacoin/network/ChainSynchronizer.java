package com.javacoin.network;

import com.javacoin.core.*;
import com.javacoin.Node;

import java.util.List;

/**
 * ChainSynchronizer - Handles blockchain synchronization between nodes
 * Implements catch-up protocol for nodes that join late or restart
 */
public class ChainSynchronizer {
    
    private Blockchain blockchain;
    private UTXOSet utxoSet;
    private Mempool mempool;
    private PeerManager peerManager;
    private String nodeId;
    private Node node;
    
    public ChainSynchronizer(Blockchain blockchain, UTXOSet utxoSet, 
                           Mempool mempool, PeerManager peerManager, String nodeId,Node node) {
        this.blockchain = blockchain;
        this.utxoSet = utxoSet;
        this.mempool = mempool;
        this.peerManager = peerManager;
        this.nodeId = nodeId;
        this.node=node;
    }
    
    /**
     * Request blockchain from all peers and sync with longest valid chain
     * Called on startup and when we detect we're behind
     */
    public void synchronizeWithNetwork() {
        System.out.println("🔄 [" + nodeId + "] Starting blockchain synchronization...");
        System.out.println("   Current height: " + blockchain.getHeight());
        
        // Request chain from all peers
        Message requestMsg = new Message(Message.MessageType.REQUEST_CHAIN, null, nodeId);
        int requestsSent = peerManager.broadcast(requestMsg);
        
        if (requestsSent == 0) {
            System.out.println("   ⚠️  No peers available for sync");
            return;
        }
        
        System.out.println("   📡 Chain requests sent to " + requestsSent + " peers");
        System.out.println("   ⏳ Waiting for responses (3 seconds)...");
        
        // Give peers time to respond
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✅ Synchronization complete");
        System.out.println("   Final height: " + blockchain.getHeight());
    }
    
    /**
     * Handle incoming blockchain from peer
     * Compare with our chain and adopt if longer and valid
     */
    public void handleChainResponse(Blockchain peerChain, String peerId) {
        System.out.println("📊 [" + nodeId + "] Received chain from " + peerId);
        System.out.println("   Our height: " + blockchain.getHeight());
        System.out.println("   Peer height: " + peerChain.getHeight());
        
        // Ignore if peer's chain is shorter or equal
        if (peerChain.getHeight() <= blockchain.getHeight()) {
            System.out.println("   ℹ️  Peer chain not longer, keeping our chain");
            return;
        }
        
        // Validate peer's chain
        if (!peerChain.isChainValid()) {
            System.err.println("   ❌ Peer chain validation failed, rejecting");
            return;
        }
        
        // Check if genesis matches
        Block ourGenesis = blockchain.getChain().get(0);
        Block peerGenesis = peerChain.getChain().get(0);
        
        if (!ourGenesis.getHash().equals(peerGenesis.getHash())) {
            System.err.println("   ❌ Genesis mismatch! Cannot sync with this peer");
            System.err.println("      Our genesis: " + ourGenesis.getHash().substring(0, 16) + "...");
            System.err.println("      Peer genesis: " + peerGenesis.getHash().substring(0, 16) + "...");
            return;
        }
        
        // Accept longer chain (fork resolution)
        System.out.println("   ✅ Peer chain is longer and valid!");
        System.out.println("   🔄 Replacing our chain...");
        
        // Replace blockchain
        synchronized (blockchain) {
            blockchain.replaceChain(peerChain);
        }
        
        // FIXED: Rebuild UTXO set from new chain
        System.out.println("   💰 Rebuilding UTXO set from new chain...");
        UTXOSet newUtxoSet = blockchain.rebuildUTXOSet();
        
        // CRITICAL FIX: Use Node's method to safely replace UTXO set
        if (node != null) {
            node.replaceUTXOSet(newUtxoSet);
        } else {
            // Fallback if node reference not available
            System.err.println("   ⚠️  Node reference null, using unsafe UTXO replacement");
            synchronized (utxoSet) {
                utxoSet.clear();
                for (UTXO utxo : newUtxoSet.getAllUTXOs()) {
                    utxoSet.addUTXO(utxo);
                }
            }
        }
        
        // Clean mempool of now-confirmed transactions
        System.out.println("   🧹 Cleaning mempool...");
        cleanMempoolAfterSync();
        
        System.out.println("   ✅ Chain synchronization complete!");
        System.out.println("   📊 New height: " + blockchain.getHeight());
        System.out.println("   💰 Total value: " + utxoSet.getTotalValue() + " JAC");
        
        // CALL DEBUG METHOD
        if (node != null) {
            node.debugUTXOState("After Chain Sync from " + peerId);
        }
    }    
    /**
     * Handle request for our blockchain from peer
     */
    public void handleChainRequest(String requesterId) {
        System.out.println("📤 [" + nodeId + "] Chain requested by " + requesterId);
        System.out.println("   Sending chain (height: " + blockchain.getHeight() + ")");
        
        // Send our blockchain to requester
        Message responseMsg = new Message(
            Message.MessageType.RESPONSE_CHAIN, 
            blockchain, 
            nodeId
        );
        
        // Find requester's address and send directly
        List<String> peers = peerManager.getPeerAddresses();
        for (String peer : peers) {
            if (peer.contains(requesterId) || requesterId.contains(peer.split(":")[1])) {
                boolean sent = peerManager.sendToPeer(peer, responseMsg);
                if (sent) {
                    System.out.println("   ✅ Chain sent to " + requesterId);
                } else {
                    System.err.println("   ❌ Failed to send chain to " + requesterId);
                }
                return;
            }
        }
        
        // If not found, broadcast (less efficient but ensures delivery)
        peerManager.broadcast(responseMsg);
    }
    
    /**
     * Request specific blocks from a peer
     * Used for gap-filling when we're missing blocks
     */
    public void requestBlocks(int startHeight, int endHeight, String peerId) {
        System.out.println("📥 [" + nodeId + "] Requesting blocks " + 
                         startHeight + "-" + endHeight + " from " + peerId);
        
        // Create request message with height range
        int[] range = new int[]{startHeight, endHeight};
        Message requestMsg = new Message(
            Message.MessageType.REQUEST_BLOCKS,
            range,
            nodeId
        );
        
        // Send to specific peer
        List<String> peers = peerManager.getPeerAddresses();
        for (String peer : peers) {
            if (peer.contains(peerId) || peerId.contains(peer.split(":")[1])) {
                peerManager.sendToPeer(peer, requestMsg);
                return;
            }
        }
    }
    
    /**
     * Clean mempool after chain replacement
     * Remove transactions that are now confirmed in new chain
     */
    private void cleanMempoolAfterSync() {
        List<Transaction> allChainTxs = blockchain.getAllTransactions();
        int removed = 0;
        
        for (Transaction chainTx : allChainTxs) {
            if (mempool.removeTransaction(chainTx.getTransactionId())) {
                removed++;
            }
        }
        
        if (removed > 0) {
            System.out.println("   🗑️  Removed " + removed + " confirmed transactions from mempool");
        }
        
        // Also validate remaining transactions against new UTXO set
        int invalid = mempool.validateAndClean(utxoSet);
        if (invalid > 0) {
            System.out.println("   🗑️  Removed " + invalid + " invalid transactions from mempool");
        }
    }
    
    /**
     * Check if we need to sync (are we behind?)
     * Called periodically or when we receive a block we can't add
     */
    public boolean needsSync() {
        // In production, would ping peers for their heights
        // For now, assume we need sync if we have just genesis
        return blockchain.getHeight() == 1;
    }
    
    /**
     * Attempt to add a block that arrived out of order
     * If we're missing previous blocks, request them
     */
    public void handleOutOfOrderBlock(Block block, String senderId) {
        int ourHeight = blockchain.getHeight();
        int blockIndex = block.getIndex();
        
        System.out.println("📦 [" + nodeId + "] Received out-of-order block");
        System.out.println("   Our height: " + ourHeight);
        System.out.println("   Block index: " + blockIndex);
        System.out.println("   Gap size: " + (blockIndex - ourHeight) + " blocks");
        
        if (blockIndex - ourHeight > 100) {
            // Large gap, request full chain
            System.out.println("   ⚠️  Large gap detected, requesting full chain");
            synchronizeWithNetwork();
        } else {
            // Small gap, request missing blocks
            System.out.println("   📥 Requesting missing blocks from " + senderId);
            requestBlocks(ourHeight, blockIndex - 1, senderId);
        }
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.