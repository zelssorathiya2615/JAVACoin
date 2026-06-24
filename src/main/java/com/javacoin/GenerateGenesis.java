package com.javacoin;

import com.javacoin.core.Block;
import com.javacoin.crypto.Wallet;

import java.io.*;

/**
 * GenerateGenesis - Standalone utility to create genesis block
 * MUST be run BEFORE starting any nodes
 * Creates genesis.block file that all nodes will load
 */
public class GenerateGenesis {
    
    private static final String GENESIS_FILE = "genesis.block";
    private static final String GENESIS_WALLET_FILE = "genesis-wallet.dat";
    private static final int DIFFICULTY = 5; // Genesis uses easier difficulty
    private static final double BLOCK_REWARD = 50.0;
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║         JAVACoin Genesis Block Generator             ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
        
        // Check if genesis already exists
        File genesisFile = new File(GENESIS_FILE);
        if (genesisFile.exists()) {
            System.out.println("⚠️  Genesis block already exists!");
            System.out.println("   File: " + genesisFile.getAbsolutePath());
            System.out.print("\n   Delete and regenerate? (yes/no): ");
            
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String response = reader.readLine().trim().toLowerCase();
                
                if (!response.equals("yes") && !response.equals("y")) {
                    System.out.println("\n✅ Using existing genesis block");
                    displayGenesisInfo();
                    return;
                }
                
                System.out.println("\n🗑️  Deleting old genesis...");
                genesisFile.delete();
                new File(GENESIS_WALLET_FILE).delete();
                
            } catch (IOException e) {
                System.err.println("❌ Error reading input: " + e.getMessage());
                return;
            }
        }
        
        System.out.println("🏗️  Creating Genesis Block...");
        System.out.println("   Difficulty: " + DIFFICULTY);
        System.out.println("   Block Reward: " + BLOCK_REWARD + " JAC");
        System.out.println("   ⏳ Mining (this takes 1-5 seconds)...\n");
        
        long startTime = System.currentTimeMillis();
        
        // Create genesis wallet
        Wallet genesisWallet = new Wallet();
        System.out.println("💰 Genesis Wallet Created:");
        System.out.println("   Address: " + genesisWallet.getAddress());
        System.out.println("   Public Key: " + genesisWallet.getPublicKeyString().substring(0, 40) + "...");
        System.out.println();
        
        // Create and mine genesis block
        Block genesis = Block.createGenesisBlock(genesisWallet.getPublicKey(), BLOCK_REWARD);
        
        long miningTime = System.currentTimeMillis() - startTime;
        
        System.out.println("✅ Genesis Block Mined!");
        System.out.println("   Hash: " + genesis.getHash());
        System.out.println("   Nonce: " + genesis.getNonce());
        System.out.println("   Mining Time: " + miningTime + " ms");
        System.out.println();
        
        // Save genesis block to file
        try {
            saveGenesisBlock(genesis);
            System.out.println("💾 Genesis block saved to: " + GENESIS_FILE);
        } catch (IOException e) {
            System.err.println("❌ Failed to save genesis block: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // Save genesis wallet to file
        try {
            saveGenesisWallet(genesisWallet);
            System.out.println("💾 Genesis wallet saved to: " + GENESIS_WALLET_FILE);
        } catch (IOException e) {
            System.err.println("❌ Failed to save genesis wallet: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║              Genesis Generation Complete             ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println("║  ✅ All nodes can now start with identical genesis    ║");
        System.out.println("║  📄 Genesis file: " + String.format("%-35s", GENESIS_FILE) + "║");
        System.out.println("║  💰 Initial supply: " + String.format("%-31.2f JAC", BLOCK_REWARD) + "║");
        System.out.println("║  🔐 Genesis wallet has exclusive control initially   ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("📌 IMPORTANT: Creator Miner (port 8081) will use this wallet");
        System.out.println("              to send initial transactions to users.");
        System.out.println();
        System.out.println("▶️  Ready to launch network with: run-nodes-web.bat");
    }
    
    /**
     * Save genesis block to file
     */
    private static void saveGenesisBlock(Block genesis) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(GENESIS_FILE))) {
            out.writeObject(genesis);
        }
    }
    
    /**
     * Save genesis wallet to file
     */
    private static void saveGenesisWallet(Wallet wallet) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(GENESIS_WALLET_FILE))) {
            out.writeObject(wallet);
        }
    }
    
    /**
     * Display info about existing genesis
     */
    private static void displayGenesisInfo() {
        try {
            Block genesis = loadGenesisBlock();
            System.out.println("\n📊 Genesis Block Info:");
            System.out.println("   Hash: " + genesis.getHash());
            System.out.println("   Timestamp: " + new java.util.Date(genesis.getTimestamp()));
            System.out.println("   Transactions: " + genesis.getTransactionCount());
            System.out.println("   Difficulty: " + genesis.getDifficulty());
            
            if (new File(GENESIS_WALLET_FILE).exists()) {
                Wallet wallet = loadGenesisWallet();
                System.out.println("\n💰 Genesis Wallet:");
                System.out.println("   Address: " + wallet.getAddress());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Could not load genesis: " + e.getMessage());
        }
    }
    
    /**
     * Load genesis block from file
     */
    public static Block loadGenesisBlock() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(GENESIS_FILE))) {
            return (Block) in.readObject();
        }
    }
    
    /**
     * Load genesis wallet from file
     */
    public static Wallet loadGenesisWallet() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(GENESIS_WALLET_FILE))) {
            return (Wallet) in.readObject();
        }
    }
    
    /**
     * Check if genesis exists
     */
    public static boolean genesisExists() {
        return new File(GENESIS_FILE).exists();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.