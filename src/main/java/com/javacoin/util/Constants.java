package com.javacoin.util;

/**
 * Constants - Global constants for JAVACoin
 */
public class Constants {
    
    // Network Configuration
    public static final String DEFAULT_HOST = "localhost";
    public static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Mining Configuration
    public static final int DEFAULT_DIFFICULTY = 5;
    public static final double DEFAULT_BLOCK_REWARD = 50.0;
    public static final int TARGET_BLOCK_TIME_SECONDS = 4;
    public static final int MAX_TRANSACTIONS_PER_BLOCK = 10;
    
    // Transaction Configuration
    public static final double MIN_TRANSACTION_FEE = 0.0;
    public static final double DEFAULT_TRANSACTION_FEE = 1.0;
    public static final int CONFIRMATION_THRESHOLD = 6; // Blocks for "confirmed"
    
    // Node Ports
    public static final int ADMIN_PORT = 8080;
    public static final int MINER_CREATOR_PORT = 8081;
    public static final int MINER_COMPETITOR_PORT = 8082;
    public static final int ALICE_PORT = 8083;
    public static final int BOB_PORT = 8084;
    public static final int CHARLIE_PORT = 8085;
    
    // Genesis Block
    public static final String GENESIS_MESSAGE = "JAVACoin Genesis Block - The Future of Decentralized Currency";
    public static final long GENESIS_TIMESTAMP = 1609459200000L; // Jan 1, 2021
    
    // Logging
    public static final boolean ENABLE_DEBUG_LOGGING = true;
    public static final boolean LOG_NETWORK_MESSAGES = true;
    public static final boolean LOG_MINING_PROGRESS = true;
    
    private Constants() {
        // Prevent instantiation
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.