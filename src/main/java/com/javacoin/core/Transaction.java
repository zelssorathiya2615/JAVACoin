package com.javacoin.core;

import com.javacoin.crypto.CryptoUtil;
import com.javacoin.crypto.Wallet;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction - Represents a JAVACoin transaction using UTXO model
 * Inputs reference previous outputs (coins to spend)
 * Outputs create new UTXOs (coins for recipients)
 */
public class Transaction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String transactionId;              // Unique hash identifier
    private PublicKey sender;                  // Sender's public key
    private List<TransactionInput> inputs;     // UTXOs being spent
    private List<TransactionOutput> outputs;   // New UTXOs being created
    private byte[] signature;                  // Digital signature
    private long timestamp;                    // When transaction was created
    private boolean isCoinbase;                // Is this a mining reward?
    
    // Transient fields (not serialized)
    private transient double cachedInputValue = -1;
    private transient double cachedOutputValue = -1;
    
    /**
     * Creates a new transaction
     * @param sender Sender's public key
     * @param inputs UTXOs to spend
     * @param outputs New UTXOs to create
     */
    public Transaction(PublicKey sender, List<TransactionInput> inputs, List<TransactionOutput> outputs) {
        this.sender = sender;
        this.inputs = inputs != null ? inputs : new ArrayList<>();
        this.outputs = outputs != null ? outputs : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.isCoinbase = false;
        
        // Calculate hash FIRST to get transaction ID
        calculateHash();
        
        // THEN update all outputs with the transaction ID
        for (int i = 0; i < this.outputs.size(); i++) {
            TransactionOutput output = this.outputs.get(i);
            output.setParentTransactionId(this.transactionId);
            output.setId(this.transactionId + ":" + i);
        }
    }        
    /**
     * Creates a Coinbase transaction (mining reward)
     * @param minerPublicKey Miner's public key
     * @param blockReward Block subsidy amount
     * @param totalFees Sum of all transaction fees in block
     */
    public static Transaction createCoinbase(PublicKey minerPublicKey, double blockReward, double totalFees) {
        Transaction coinbase = new Transaction(minerPublicKey, new ArrayList<>(), new ArrayList<>());
        coinbase.isCoinbase = true;
        
        // Calculate hash first to get transaction ID
        String tempData = minerPublicKey.toString() + System.currentTimeMillis() + "COINBASE" + (blockReward + totalFees);
        coinbase.transactionId = CryptoUtil.applySHA256(tempData);
        
        // Coinbase has no inputs (new coins created from nothing)
        // Output pays miner the block reward + fees
        double totalReward = blockReward + totalFees;
        TransactionOutput output = new TransactionOutput(minerPublicKey, totalReward, coinbase.transactionId);
        coinbase.outputs.add(output);
        output.setId(coinbase.transactionId + ":0");
        output.setParentTransactionId(coinbase.transactionId);  // ADD THIS
        
        return coinbase;
    }
    
    /**
     * Calculates SHA-256 hash of transaction data
     * This becomes the transaction ID
     */
    public void calculateHash() {
        String data = sender.toString() + 
                    timestamp + 
                    getInputsData() + 
                    getOutputsData() +
                    isCoinbase;
        this.transactionId = CryptoUtil.applySHA256(data);
        
        // CRITICAL: Update parent transaction ID for all outputs
        for (int i = 0; i < outputs.size(); i++) {
            TransactionOutput output = outputs.get(i);
            output.setId(transactionId + ":" + i);
            
            // ALSO set the parent transaction ID
            // Check if TransactionOutput has this setter, if not, we need to add it
        }
    }
    
    /**
     * Gets concatenated string of all inputs
     * @return Input data string
     */
    private String getInputsData() {
        StringBuilder sb = new StringBuilder();
        for (TransactionInput input : inputs) {
            sb.append(input.getUtxoId());
        }
        return sb.toString();
    }
    
    /**
     * Gets concatenated string of all outputs
     * @return Output data string
     */
    private String getOutputsData() {
        StringBuilder sb = new StringBuilder();
        for (TransactionOutput output : outputs) {
            sb.append(output.getRecipient().toString()).append(output.getValue());
        }
        return sb.toString();
    }
    
    /**
     * Signs the transaction with sender's private key
     * @param privateKey Sender's private key
     */
    public void signTransaction(PrivateKey privateKey) {
        String data = transactionId;
        this.signature = CryptoUtil.applyECDSASignature(privateKey, data);
    }
    
    /**
     * Verifies the transaction signature
     * @return true if signature is valid
     */
    public boolean verifySignature() {
        if (isCoinbase) {
            return true; // Coinbase transactions don't need signatures
        }
        
        if (signature == null || sender == null) {
            return false;
        }
        
        String data = transactionId;
        return Wallet.verify(sender, data, signature);
    }
    
    /**
     * Calculates total input value
     * @return Sum of all input values
     */
    public double getInputValue() {
        if (cachedInputValue >= 0) {
            return cachedInputValue;
        }
        
        double total = 0.0;
        for (TransactionInput input : inputs) {
            if (input.getUtxo() != null) {
                total += input.getUtxo().getValue();
            }
        }
        cachedInputValue = total;
        return total;
    }
    
    /**
     * Calculates total output value
     * @return Sum of all output values
     */
    public double getOutputValue() {
        if (cachedOutputValue >= 0) {
            return cachedOutputValue;
        }
        
        double total = 0.0;
        for (TransactionOutput output : outputs) {
            total += output.getValue();
        }
        cachedOutputValue = total;
        return total;
    }
    
    /**
     * Calculates transaction fee (implicit)
     * Fee = Total Inputs - Total Outputs
     * @return Transaction fee
     */
    public double getFee() {
        if (isCoinbase) {
            return 0.0; // Coinbase transactions have no fee
        }
        return getInputValue() - getOutputValue();
    }
    
    /**
     * Validates transaction logic
     * @param utxoSet Current UTXO set for verification
     * @return true if transaction is valid
     */
    public boolean isValid(UTXOSet utxoSet) {
        // Coinbase validation
        if (isCoinbase) {
            return outputs.size() == 1 && inputs.isEmpty();
        }
        
        // Verify signature
        if (!verifySignature()) {
            System.err.println("Transaction signature invalid: " + transactionId);
            return false;
        }
        
        // Check if inputs exist and are unspent
        for (TransactionInput input : inputs) {
            UTXO utxo = utxoSet.getUTXO(input.getUtxoId());
            if (utxo == null) {
                System.err.println("Input UTXO not found or already spent: " + input.getUtxoId());
                return false;
            }
            
            // Link UTXO to input
            input.setUtxo(utxo);
            
            // Verify ownership
            if (!utxo.getRecipient().equals(sender)) {
                System.err.println("Sender doesn't own input UTXO: " + input.getUtxoId());
                return false;
            }
        }
        
        // Check that inputs >= outputs (no negative fee)
        if (getInputValue() < getOutputValue()) {
            System.err.println("Transaction outputs exceed inputs (negative fee)");
            return false;
        }
        
        return true;
    }
    
    /**
     * Processes transaction: removes spent UTXOs, adds new ones
     * @param utxoSet UTXO set to update
     */
    public void processTransaction(UTXOSet utxoSet) {
        if (isCoinbase) {
            // Coinbase only adds outputs (no inputs to remove)
            System.out.println("      [UTXO] Processing Coinbase:");
            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                UTXO utxo = new UTXO(this.transactionId, i, output.getRecipient(), output.getValue());
                utxoSet.addUTXO(utxo);
                System.out.println("         + Added UTXO: " + utxo.getId() + " = " + utxo.getValue() + " JAC");
            }
            return;
        }
        
        // Remove spent UTXOs
        System.out.println("      [UTXO] Removing spent inputs:");
        for (TransactionInput input : inputs) {
            String utxoId = input.getUtxoId();
            UTXO removed = utxoSet.removeUTXO(utxoId);
            if (removed != null) {
                System.out.println("         - Removed UTXO: " + utxoId + " = " + removed.getValue() + " JAC");
            } else {
                System.out.println("         ⚠️  UTXO not found: " + utxoId);
            }
        }
        
        // Add new UTXOs (INCLUDING CHANGE!)
        System.out.println("      [UTXO] Adding new outputs:");
        for (int i = 0; i < outputs.size(); i++) {
            TransactionOutput output = outputs.get(i);
            
            // CRITICAL FIX: Create UTXO directly with correct transaction ID
            UTXO utxo = new UTXO(this.transactionId, i, output.getRecipient(), output.getValue());
            
            utxoSet.addUTXO(utxo);
            System.out.println("         + Added UTXO: " + utxo.getId() + " = " + utxo.getValue() + " JAC");
        }
        
        System.out.println("      [UTXO] Transaction processing complete");
        System.out.println("         Total UTXOs now: " + utxoSet.size());
        System.out.println("         Total value now: " + utxoSet.getTotalValue() + " JAC");
    }
    
    // Getters
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public PublicKey getSender() {
        return sender;
    }
    
    public List<TransactionInput> getInputs() {
        return inputs;
    }
    
    public List<TransactionOutput> getOutputs() {
        return outputs;
    }
    
    public byte[] getSignature() {
        return signature;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isCoinbase() {
        return isCoinbase;
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id='%s', inputs=%d, outputs=%d, fee=%.2f, coinbase=%b}", 
                           transactionId.substring(0, Math.min(8, transactionId.length())),
                           inputs.size(),
                           outputs.size(),
                           getFee(),
                           isCoinbase);
    }
    
    /**
     * Display-friendly representation
     * @return Formatted string
     */
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════╗\n");
        sb.append("║           JAVACoin Transaction                ║\n");
        sb.append("╠═══════════════════════════════════════════════╣\n");
        sb.append(String.format("║ ID: %s...║\n", transactionId.substring(0, 20)));
        sb.append(String.format("║ Type: %-38s ║\n", isCoinbase ? "COINBASE (Mining Reward)" : "Standard"));
        sb.append(String.format("║ Inputs: %-36d ║\n", inputs.size()));
        sb.append(String.format("║ Outputs: %-35d ║\n", outputs.size()));
        sb.append(String.format("║ Fee: %-39.2f ║\n", getFee()));
        sb.append("╚═══════════════════════════════════════════════╝");
        return sb.toString();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.