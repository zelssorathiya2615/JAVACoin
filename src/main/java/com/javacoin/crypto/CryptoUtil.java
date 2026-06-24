package com.javacoin.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * CryptoUtil - Core cryptographic utilities for JAVACoin
 * Handles SHA-256 hashing, ECDSA signing/verification using secp256k1 curve
 */
public class CryptoUtil {
    
    // Static block to register Bouncy Castle provider once
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    // Elliptic Curve parameter (Bitcoin's secp256k1)
    public static final String ELLIPTIC_CURVE = "secp256k1";
    
    // Signature algorithm
    public static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    
    /**
     * Generates SHA-256 hash of input string
     * @param input String to hash
     * @return Hex-encoded hash
     */
    public static String applySHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Generates SHA-256 hash of byte array
     * @param input Bytes to hash
     * @return Hex-encoded hash
     */
    public static String applySHA256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Applies SHA-256 twice (double hashing, Bitcoin-style)
     * Used for block hashing and transaction IDs
     * @param input String to hash
     * @return Double-hashed hex string
     */
    public static String applyDoubleSHA256(String input) {
        String firstHash = applySHA256(input);
        return applySHA256(firstHash);
    }
    
    /**
     * Generates ECDSA keypair using secp256k1 curve
     * @return KeyPair containing private and public keys
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(ELLIPTIC_CURVE);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(ecSpec, random);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate keypair", e);
        }
    }
    
    /**
     * Signs data with private key using ECDSA
     * @param privateKey Signer's private key
     * @param data Data to sign
     * @return Digital signature as byte array
     */
    public static byte[] applyECDSASignature(PrivateKey privateKey, String data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, "BC");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }
    
    /**
     * Signs byte array with private key using ECDSA
     * @param privateKey Signer's private key
     * @param data Data to sign
     * @return Digital signature as byte array
     */
    public static byte[] applyECDSASignature(PrivateKey privateKey, byte[] data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, "BC");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }
    
    /**
     * Verifies ECDSA signature
     * @param publicKey Signer's public key
     * @param data Original data
     * @param signature Signature to verify
     * @return true if signature is valid, false otherwise
     */
    public static boolean verifyECDSASignature(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, "BC");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifies ECDSA signature on byte array
     * @param publicKey Signer's public key
     * @param data Original data
     * @param signature Signature to verify
     * @return true if signature is valid, false otherwise
     */
    public static boolean verifyECDSASignature(PublicKey publicKey, byte[] data, byte[] signature) {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, "BC");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Converts byte array to hexadecimal string
     * @param bytes Byte array
     * @return Hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Converts hexadecimal string to byte array
     * @param hex Hex string
     * @return Byte array
     */
    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * Encodes byte array to Base64 string
     * @param bytes Byte array
     * @return Base64 string
     */
    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * Decodes Base64 string to byte array
     * @param base64 Base64 string
     * @return Byte array
     */
    public static byte[] decodeBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }
    
    /**
     * Generates Merkle Root from list of transaction IDs
     * Used in Block header to represent all transactions
     * @param transactionIds List of transaction hashes
     * @return Merkle root hash
     */
    public static String getMerkleRoot(List<String> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return applySHA256("");
        }
        
        List<String> previousTreeLayer = new ArrayList<>(transactionIds);
        
        while (previousTreeLayer.size() > 1) {
            List<String> treeLayer = new ArrayList<>();
            
            for (int i = 0; i < previousTreeLayer.size(); i += 2) {
                if (i + 1 < previousTreeLayer.size()) {
                    // Combine two adjacent hashes
                    String combined = previousTreeLayer.get(i) + previousTreeLayer.get(i + 1);
                    treeLayer.add(applySHA256(combined));
                } else {
                    // Odd number of elements, duplicate last one
                    String combined = previousTreeLayer.get(i) + previousTreeLayer.get(i);
                    treeLayer.add(applySHA256(combined));
                }
            }
            
            previousTreeLayer = treeLayer;
        }
        
        return previousTreeLayer.get(0);
    }
    
    /**
     * Checks if hash meets difficulty target (has required leading zeros)
     * @param hash Hash string to check
     * @param difficulty Number of leading zeros required
     * @return true if hash meets difficulty, false otherwise
     */
    public static boolean hashMeetsDifficulty(String hash, int difficulty) {
        String target = "0".repeat(difficulty);
        return hash.startsWith(target);
    }
    
    /**
     * Gets difficulty target string (e.g., "0000" for difficulty 4)
     * @param difficulty Number of leading zeros
     * @return Target string
     */
    public static String getDifficultyTarget(int difficulty) {
        return "0".repeat(difficulty);
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.