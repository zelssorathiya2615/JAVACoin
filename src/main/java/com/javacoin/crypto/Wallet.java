package com.javacoin.crypto;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class Wallet implements Serializable{
    private static final long serialVersionUID = 1L;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String address;
    
    public Wallet() {
        generateKeyPair();
    }
    
    public Wallet(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.address = generateAddress(publicKey);
    }
    
    private void generateKeyPair() {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        this.address = generateAddress(publicKey);
    }
    
    /**
     * Generates UNIQUE wallet address from public key
     * Uses SHA-256 hash of the full public key for uniqueness
     */
    private String generateAddress(PublicKey publicKey) {
        // Get full public key bytes
        byte[] pubKeyBytes = publicKey.getEncoded();
        
        // Hash the public key for uniqueness
        String pubKeyHash = CryptoUtil.applySHA256(pubKeyBytes);
        
        // Return first 32 characters (enough for visual distinction)
        return pubKeyHash.substring(0, 32);
    }
    
    public byte[] sign(String data) {
        return CryptoUtil.applyECDSASignature(privateKey, data);
    }
    
    public byte[] sign(byte[] data) {
        return CryptoUtil.applyECDSASignature(privateKey, data);
    }
    
    public boolean verify(String data, byte[] signature) {
        return CryptoUtil.verifyECDSASignature(publicKey, data, signature);
    }
    
    public static boolean verify(PublicKey publicKey, String data, byte[] signature) {
        return CryptoUtil.verifyECDSASignature(publicKey, data, signature);
    }
    
    public static boolean verify(PublicKey publicKey, byte[] data, byte[] signature) {
        return CryptoUtil.verifyECDSASignature(publicKey, data, signature);
    }
    
    public PrivateKey getPrivateKey() {
        return privateKey;
    }
    
    public PublicKey getPublicKey() {
        return publicKey;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getPublicKeyString() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    public String getPrivateKeyString() {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    @Override
    public String toString() {
        return "Wallet{address='" + address + "'}";
    }
    
    public String getDisplayInfo() {
        return String.format(
            "═══════════════════════════════════════\n" +
            "  JAVACoin Wallet\n" +
            "═══════════════════════════════════════\n" +
            "  Address: %s\n" +
            "  Public Key: %s...\n" +
            "═══════════════════════════════════════",
            address,
            getPublicKeyString().substring(0, 40)
        );
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.