package com.javacoin;

import com.javacoin.crypto.CryptoUtil;
import com.javacoin.crypto.Wallet;

import java.util.Arrays;
import java.util.List;

/**
 * CryptoTest - Test harness for cryptographic components
 * Run this to verify Bouncy Castle, ECDSA, and hashing work correctly
 */
public class CryptoTest {
    
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  JAVACoin Cryptography Test Suite");
        System.out.println("═══════════════════════════════════════════════════\n");
        
        // Test 1: SHA-256 Hashing
        testSHA256Hashing();
        
        // Test 2: Wallet Generation
        testWalletGeneration();
        
        // Test 3: Digital Signatures
        testDigitalSignatures();
        
        // Test 4: Merkle Root
        testMerkleRoot();
        
        // Test 5: Difficulty Check
        testDifficultyCheck();
        
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("  ✅ All Cryptography Tests Passed!");
        System.out.println("═══════════════════════════════════════════════════");
    }
    
    /**
     * Test SHA-256 hashing functionality
     */
    private static void testSHA256Hashing() {
        System.out.println("🔐 Test 1: SHA-256 Hashing");
        System.out.println("─────────────────────────────────────────────────");
        
        String data = "Hello JAVACoin!";
        String hash = CryptoUtil.applySHA256(data);
        
        System.out.println("Input: " + data);
        System.out.println("SHA-256 Hash: " + hash);
        System.out.println("Hash Length: " + hash.length() + " characters");
        
        // Verify deterministic (same input = same output)
        String hash2 = CryptoUtil.applySHA256(data);
        assert hash.equals(hash2) : "Hashing not deterministic!";
        
        // Verify different input = different output
        String hash3 = CryptoUtil.applySHA256("Different Data");
        assert !hash.equals(hash3) : "Different inputs produced same hash!";
        
        System.out.println("✓ SHA-256 hashing works correctly\n");
    }
    
    /**
     * Test wallet generation with ECDSA keypairs
     */
    private static void testWalletGeneration() {
        System.out.println("🔐 Test 2: Wallet Generation (ECDSA secp256k1)");
        System.out.println("─────────────────────────────────────────────────");
        
        // Generate two wallets
        Wallet wallet1 = new Wallet();
        Wallet wallet2 = new Wallet();
        
        System.out.println("Wallet 1:");
        System.out.println("  Address: " + wallet1.getAddress());
        System.out.println("  Public Key: " + wallet1.getPublicKeyString().substring(0, 40) + "...");
        System.out.println();
        
        System.out.println("Wallet 2:");
        System.out.println("  Address: " + wallet2.getAddress());
        System.out.println("  Public Key: " + wallet2.getPublicKeyString().substring(0, 40) + "...");
        System.out.println();
        
        // Verify uniqueness
        assert !wallet1.getAddress().equals(wallet2.getAddress()) : "Wallets not unique!";
        assert wallet1.getPrivateKey() != null : "Private key is null!";
        assert wallet1.getPublicKey() != null : "Public key is null!";
        
        System.out.println("✓ Wallet generation works correctly\n");
    }
    
    /**
     * Test ECDSA digital signatures
     */
    private static void testDigitalSignatures() {
        System.out.println("🔐 Test 3: ECDSA Digital Signatures");
        System.out.println("─────────────────────────────────────────────────");
        
        Wallet wallet = new Wallet();
        String message = "Send 50 JAVACoin to Alice";
        
        System.out.println("Message: " + message);
        System.out.println("Signer: " + wallet.getAddress());
        System.out.println();
        
        // Sign the message
        byte[] signature = wallet.sign(message);
        System.out.println("Signature Generated: " + CryptoUtil.bytesToHex(signature).substring(0, 40) + "...");
        System.out.println("Signature Length: " + signature.length + " bytes");
        System.out.println();
        
        // Verify with correct public key
        boolean isValid = Wallet.verify(wallet.getPublicKey(), message, signature);
        System.out.println("✓ Signature verification (correct key): " + isValid);
        assert isValid : "Valid signature failed verification!";
        
        // Verify fails with wrong public key
        Wallet wrongWallet = new Wallet();
        boolean isInvalid = Wallet.verify(wrongWallet.getPublicKey(), message, signature);
        System.out.println("✗ Signature verification (wrong key): " + isInvalid);
        assert !isInvalid : "Invalid signature passed verification!";
        
        // Verify fails with tampered message
        boolean isTampered = Wallet.verify(wallet.getPublicKey(), "Tampered message", signature);
        System.out.println("✗ Signature verification (tampered data): " + isTampered);
        assert !isTampered : "Tampered message passed verification!";
        
        System.out.println("\n✓ Digital signatures work correctly\n");
    }
    
    /**
     * Test Merkle Root calculation
     */
    private static void testMerkleRoot() {
        System.out.println("🔐 Test 4: Merkle Root Calculation");
        System.out.println("─────────────────────────────────────────────────");
        
        List<String> txIds = Arrays.asList(
            "tx1_hash_abc123",
            "tx2_hash_def456",
            "tx3_hash_ghi789",
            "tx4_hash_jkl012"
        );
        
        String merkleRoot = CryptoUtil.getMerkleRoot(txIds);
        
        System.out.println("Transaction IDs:");
        for (int i = 0; i < txIds.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + txIds.get(i));
        }
        System.out.println();
        System.out.println("Merkle Root: " + merkleRoot);
        
        // Verify deterministic
        String merkleRoot2 = CryptoUtil.getMerkleRoot(txIds);
        assert merkleRoot.equals(merkleRoot2) : "Merkle root not deterministic!";
        
        // Verify different transactions = different root
        List<String> differentTxs = Arrays.asList("different_tx");
        String differentRoot = CryptoUtil.getMerkleRoot(differentTxs);
        assert !merkleRoot.equals(differentRoot) : "Different transactions produced same merkle root!";
        
        System.out.println("\n✓ Merkle root calculation works correctly\n");
    }
    
    /**
     * Test difficulty target checking
     */
    private static void testDifficultyCheck() {
        System.out.println("🔐 Test 5: Proof-of-Work Difficulty Check");
        System.out.println("─────────────────────────────────────────────────");
        
        int difficulty = 5;
        String target = CryptoUtil.getDifficultyTarget(difficulty);
        
        System.out.println("Difficulty Level: " + difficulty);
        System.out.println("Target: " + target);
        System.out.println();
        
        // Simulated hashes
        String validHash = "0000a1b2c3d4e5f6...";
        String invalidHash = "123a1b2c3d4e5f6...";
        
        boolean meetsTarget = CryptoUtil.hashMeetsDifficulty(validHash, difficulty);
        boolean failsTarget = CryptoUtil.hashMeetsDifficulty(invalidHash, difficulty);
        
        System.out.println("Valid Hash: " + validHash);
        System.out.println("  Meets difficulty? " + meetsTarget);
        assert meetsTarget : "Valid hash didn't meet difficulty!";
        
        System.out.println();
        System.out.println("Invalid Hash: " + invalidHash);
        System.out.println("  Meets difficulty? " + failsTarget);
        assert !failsTarget : "Invalid hash met difficulty!";
        
        System.out.println("\n✓ Difficulty checking works correctly\n");
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.