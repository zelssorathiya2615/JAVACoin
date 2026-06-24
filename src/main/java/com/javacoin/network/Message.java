package com.javacoin.network;

import com.javacoin.core.Block;
import com.javacoin.core.Blockchain;
import com.javacoin.core.Transaction;

import java.io.Serializable;

/**
 * Message - Serializable network message for P2P communication
 * UPDATED: Added chain synchronization message types
 */
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Message types
    public enum MessageType {
        NEW_TRANSACTION,      // Broadcasting a new transaction
        NEW_BLOCK,            // Broadcasting a newly mined block
        REQUEST_CHAIN,        // Requesting full blockchain from peer (NEW)
        RESPONSE_CHAIN,       // Sending blockchain to requesting peer (NEW)
        REQUEST_BLOCKS,       // Requesting specific block range (NEW)
        RESPONSE_BLOCKS,      // Sending requested blocks (NEW)
        REQUEST_UTXO_SET,     // Requesting current UTXO set
        RESPONSE_UTXO_SET,    // Sending UTXO set
        PING,                 // Check if peer is alive
        PONG,                 // Response to ping
        PEER_LIST,            // Sharing known peers
        DIFFICULTY_UPDATE     // Broadcasting new difficulty setting
    }
    
    private MessageType type;
    private Object payload;           // Transaction, Block, Blockchain, etc.
    private String senderId;          // Node address that sent this message
    private long timestamp;
    
    /**
     * Creates a new message
     * @param type Message type
     * @param payload Data to send
     * @param senderId Sender's node ID
     */
    public Message(MessageType type, Object payload, String senderId) {
        this.type = type;
        this.payload = payload;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a transaction broadcast message
     * @param transaction Transaction to broadcast
     * @param senderId Sender node ID
     * @return Message object
     */
    public static Message newTransaction(Transaction transaction, String senderId) {
        return new Message(MessageType.NEW_TRANSACTION, transaction, senderId);
    }
    
    /**
     * Creates a block broadcast message
     * @param block Block to broadcast
     * @param senderId Sender node ID
     * @return Message object
     */
    public static Message newBlock(Block block, String senderId) {
        return new Message(MessageType.NEW_BLOCK, block, senderId);
    }
    
    /**
     * Creates a ping message
     * @param senderId Sender node ID
     * @return Message object
     */
    public static Message ping(String senderId) {
        return new Message(MessageType.PING, null, senderId);
    }
    
    /**
     * Creates a pong response message
     * @param senderId Sender node ID
     * @return Message object
     */
    public static Message pong(String senderId) {
        return new Message(MessageType.PONG, null, senderId);
    }
    
    /**
     * Creates a difficulty update message
     * @param newDifficulty New difficulty value
     * @param senderId Sender node ID (usually admin)
     * @return Message object
     */
    public static Message difficultyUpdate(int newDifficulty, String senderId) {
        return new Message(MessageType.DIFFICULTY_UPDATE, newDifficulty, senderId);
    }
    
    /**
     * Gets the transaction from message payload
     * @return Transaction, or null if not a transaction message
     */
    public Transaction getTransaction() {
        if (type == MessageType.NEW_TRANSACTION && payload instanceof Transaction) {
            return (Transaction) payload;
        }
        return null;
    }
    
    /**
     * Gets the block from message payload
     * @return Block, or null if not a block message
     */
    public Block getBlock() {
        if (type == MessageType.NEW_BLOCK && payload instanceof Block) {
            return (Block) payload;
        }
        return null;
    }
    
    /**
     * Gets the blockchain from message payload
     * @return Blockchain, or null if not a blockchain message
     */
    public Blockchain getBlockchain() {
        if ((type == MessageType.RESPONSE_CHAIN) && payload instanceof Blockchain) {
            return (Blockchain) payload;
        }
        return null;
    }
    
    /**
     * Gets integer payload (e.g., difficulty)
     * @return Integer value
     */
    public Integer getIntegerPayload() {
        if (payload instanceof Integer) {
            return (Integer) payload;
        }
        return null;
    }
    
    // Getters
    
    public MessageType getType() {
        return type;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("Message{type=%s, sender='%s', time=%d}", 
                           type, senderId, timestamp);
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        String payloadInfo = "";
        if (payload instanceof Transaction) {
            Transaction tx = (Transaction) payload;
            payloadInfo = "TX: " + tx.getTransactionId().substring(0, 8) + "...";
        } else if (payload instanceof Block) {
            Block block = (Block) payload;
            payloadInfo = "Block #" + block.getIndex();
        } else if (payload instanceof Blockchain) {
            Blockchain chain = (Blockchain) payload;
            payloadInfo = "Chain (height: " + chain.getHeight() + ")";
        } else if (payload instanceof Integer) {
            payloadInfo = "Value: " + payload;
        }
        
        return String.format("[%s] %s from %s | %s", 
                           type, 
                           payloadInfo,
                           senderId.substring(0, Math.min(10, senderId.length())),
                           new java.util.Date(timestamp));
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.