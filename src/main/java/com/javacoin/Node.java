package com.javacoin;


import com.javacoin.core.*;
import com.javacoin.crypto.Wallet;
import com.javacoin.mining.Miner;
import com.javacoin.network.*;
import com.javacoin.web.server.EmbeddedServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Node - Main entry point for JAVACoin nodes
 * FIXED VERSION - Proper genesis sync, network initialization, and wallet management
 */
public class Node {
    private String nodeId;
    private int p2pPort;
    private int httpPort;
    private NodeRole role;
    
    private Blockchain blockchain;
    private UTXOSet utxoSet;
    private Mempool mempool;
    private Wallet wallet;
    
    private PeerManager peerManager;
    private ServerThread serverThread;
    private MessageHandler messageHandler;
    private BlockingQueue<Message> messageQueue;
    
    private Miner miner;
    private EmbeddedServer webServer;
    private Properties config;
    
    // Shared genesis block created ONLY by first miner (port 8081)
    private ChainSynchronizer chainSynchronizer;
    private Wallet genesisWalletInstance; // NEW: Store genesis wallet if we're creator miner
    
    public enum NodeRole {
        ADMIN,
        MINER,
        USER
    }
    
    public Node(int p2pPort, int httpPort, NodeRole role) {
        this.p2pPort = p2pPort;
        this.httpPort = httpPort;
        this.role = role;
        this.nodeId = role.toString() + "-" + httpPort;
        
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║              JAVACoin Node Starting                   ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println("║ Node ID: " + String.format("%-44s", nodeId) + "║");
        System.out.println("║ Role: " + String.format("%-47s", role) + "║");
        System.out.println("║ P2P Port: " + String.format("%-43d", p2pPort) + "║");
        System.out.println("║ HTTP Port: " + String.format("%-42d", httpPort) + "║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
    }
    
    /**
     * FIXED: Main initialization with proper genesis sync
     */
    public void initialize() {
        try {
            loadConfiguration();
            
            // Generate unique wallet
            System.out.println("🔐 Generating unique wallet...");
            wallet = new Wallet();
            System.out.println("   ✅ Address: " + wallet.getAddress());
            
            System.out.println("⛓️ Initializing blockchain...");
            
            // Load genesis from file
            Block genesisBlock = loadGenesisFromFile();
            
            blockchain = new Blockchain(genesisBlock, getDifficulty(), getBlockReward());
            
            System.out.println("💰 Building UTXO set...");
            utxoSet = blockchain.rebuildUTXOSet();
            
            // Check balances
            double myBalance = utxoSet.getBalance(wallet.getPublicKey());
            System.out.println("   My Balance: " + myBalance + " JAC");
            
            if (genesisWalletInstance != null) {
                double genesisBalance = utxoSet.getBalance(genesisWalletInstance.getPublicKey());
                System.out.println("   Genesis Wallet Balance: " + genesisBalance + " JAC");
            }
            
            if (myBalance == 0 && role == NodeRole.USER) {
                System.out.println("   💡 Users start with 0 JAC - request coins from miners!");
            }
            
            mempool = new Mempool();
            System.out.println("📋 Mempool initialized");
            
            // STEP 1: Initialize network first (creates messageHandler)
            System.out.println("🌐 Starting network initialization...");
            try {
                initializeNetwork();
                System.out.println("   ✅ Network initialization complete");
            } catch (Exception e) {
                System.err.println("❌ FATAL: Network initialization failed!");
                e.printStackTrace();
                throw e;
            }
            
            // DEBUG: Verify messageHandler was created
            if (messageHandler == null) {
                throw new RuntimeException("❌ MessageHandler is NULL after initializeNetwork()!");
            }
            
            // STEP 2: Now create ChainSynchronizer and connect to messageHandler
            System.out.println("🔄 Initializing chain synchronizer...");
            chainSynchronizer = new ChainSynchronizer(blockchain, utxoSet, mempool, peerManager, nodeId,this);
            messageHandler.setChainSynchronizer(chainSynchronizer);
            
            // Wait for network to stabilize
            System.out.println("⏳ Waiting for network to stabilize...");
            Thread.sleep(3000);
            
            initializeWebServer();
            
            if (role == NodeRole.MINER) {
                initializeMiner();
            }
            
            // Sync with network if not Creator Miner
            if (role != NodeRole.MINER || httpPort != 8081) {
                chainSynchronizer.synchronizeWithNetwork();
            }
            
            System.out.println("\n✅ Node ready!\n");
            
        } catch (Exception e) {
            System.err.println("❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    /**
     * CRITICAL FIX: Wait for genesis block to be fully created
     * This ensures all nodes start with the SAME genesis block
     */
    private Block loadGenesisFromFile() {
        try {
            if (!GenerateGenesis.genesisExists()) {
                throw new RuntimeException("❌ Genesis block file not found! Run generate-genesis.bat first");
            }
            
            System.out.println("   📂 Loading genesis from file...");
            Block genesis = GenerateGenesis.loadGenesisBlock();
            System.out.println("   ✅ Genesis loaded: " + genesis.getHash().substring(0, 16) + "...");
            
            // Load genesis wallet if we're creator miner
            if (role == NodeRole.MINER && httpPort == 8081) {
                try {
                    genesisWalletInstance = GenerateGenesis.loadGenesisWallet();
                    System.out.println("   💰 Genesis wallet loaded");
                } catch (Exception e) {
                    System.err.println("   ⚠️  Could not load genesis wallet: " + e.getMessage());
                }
            }
            
            return genesis;
            
        } catch (Exception e) {
            System.err.println("❌ FATAL: Failed to load genesis block!");
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot start without genesis block", e);
        }
    }
    /**
     * CRITICAL FIX: Synchronize blockchain with network on startup
     */
    private void synchronizeWithNetwork() {
        if (role != NodeRole.MINER || httpPort != 8081) {
            chainSynchronizer.synchronizeWithNetwork();
        }   
    }
    
    private void loadConfiguration() {
        config = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/config/nodes.properties")) {
            config.load(fis);
            System.out.println("📋 Config loaded");
        } catch (IOException e) {
            setDefaultConfiguration();
        }
    }
    
    private void setDefaultConfiguration() {
        config.setProperty("mining.difficulty", "5");
        config.setProperty("mining.block.reward", "50.0");
        config.setProperty("peers", "localhost:9080,localhost:9081,localhost:9082,localhost:9083,localhost:9084,localhost:9085");
    }
    
    /**
     * FIXED: Initialize network components in correct order
     * ServerThread MUST start BEFORE PeerManager
     */
    private void initializeNetwork() {
        System.out.println("🌐 Initializing network...");
        
        String peersStr = config.getProperty("peers", "");
        List<String> peerAddresses = new ArrayList<>();
        if (!peersStr.isEmpty()) {
            for (String peer : peersStr.split(",")) {
                peerAddresses.add(peer.trim());
            }
        }
        
        System.out.println("   📋 Configured " + peerAddresses.size() + " peers");
        
        // STEP 1: Create message queue
        messageQueue = new LinkedBlockingQueue<>();
        
        // STEP 2: Start ServerThread FIRST (must be listening before peers try to connect)
        serverThread = new ServerThread(p2pPort, messageQueue);
        serverThread.start();
        System.out.println("   🌍 ServerThread listening on port " + p2pPort);
        
        // STEP 3: Give ServerThread time to bind to port
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // STEP 4: Create PeerManager (now safe)
        peerManager = new PeerManager(nodeId, p2pPort, peerAddresses);
        
        // STEP 5: Start MessageHandler to process incoming messages
        messageHandler = new MessageHandler(messageQueue, blockchain, utxoSet, mempool, peerManager);
        messageHandler.start();
        System.out.println("   📬 MessageHandler ready");
        
        System.out.println("   ✅ Network initialized");
    }
    
    private void initializeWebServer() {
        System.out.println("🌐 Web...");
        webServer = new EmbeddedServer(this, httpPort);
        webServer.start();
    }
    
    /**
     * FIXED: Initialize miner with proper wallet
     * Creator Miner uses genesis wallet (has initial 50 JAC)
     * Other miners use their own wallets
     */
    private void initializeMiner() {
        System.out.println("⛏️ Initializing Miner...");
        
        Wallet minerWallet = wallet; // Default to node's wallet
        
        // Creator Miner uses genesis wallet
        if (httpPort == 8081) {
            try {
                genesisWalletInstance = GenerateGenesis.loadGenesisWallet();
                minerWallet = genesisWalletInstance;
                this.wallet = genesisWalletInstance;
                
                System.out.println("   💰 Using Genesis Wallet (has 50.0 JAC from genesis block)");
                System.out.println("   Address: " + minerWallet.getAddress());
                System.out.println("   ✅ Can send transactions immediately!");
            } catch (Exception e) {
                System.err.println("   ⚠️  Could not load genesis wallet: " + e.getMessage());
                System.out.println("   💰 Using Node Wallet (0 JAC initially)");
                System.out.println("   Address: " + minerWallet.getAddress());
            }
        } else {
            System.out.println("   💰 Using Node Wallet (0 JAC initially)");
            System.out.println("   Address: " + minerWallet.getAddress());
            System.out.println("   ℹ️  Will earn JAC by mining blocks");
        }
        
        miner = new Miner(blockchain, utxoSet, mempool, minerWallet, peerManager, nodeId);
        miner.start();
        
        // AUTO-START mining
        miner.startMining();
        System.out.println("   ⛏️ Mining started! Racing for blocks...");
    }
    
    public void start() {
        System.out.println("▶️ Running...");
        
        String endpoint = switch (role) {
            case ADMIN -> "/admin";
            case MINER -> "/miner";
            case USER -> "/user";
        };
        System.out.println("   Web: http://localhost:" + httpPort + endpoint);
        System.out.println("   Ctrl+C to stop\n");
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        // Status reporting thread
        Thread statusThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Every 30 seconds
                    displayStatus();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statusThread.setDaemon(true);
        statusThread.start();
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            shutdown();
        }
    }
    
    private void displayStatus() {
        debugBalances();
        
        if (mempool.size() > 0) {
            debugMempool();
        }
    }
    
    private void shutdown() {
        System.out.println("\n🛑 Stopping...");
        if (webServer != null) webServer.stop();
        if (miner != null) miner.shutdown();
        if (messageHandler != null) messageHandler.shutdown();
        if (serverThread != null) serverThread.shutdown();
        System.out.println("✅ Stopped");
    }
    
    // ============================================
    // DEBUG METHODS
    // ============================================
    
    /**
     * Debug: Print detailed balance information
     */
    public void debugBalances() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║              BALANCE DEBUG REPORT                  ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║ Node: " + String.format("%-43s", nodeId) + "║");
        System.out.println("║ Role: " + String.format("%-43s", role) + "║");
        System.out.println("║ Address: " + String.format("%-38s", wallet.getAddress()) + "║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        
        // My balance
        double myBalance = utxoSet.getBalance(wallet.getPublicKey());
        System.out.println("║ My Balance: " + String.format("%33.2f JAC", myBalance) + " ║");
        
        // My UTXOs
        List<UTXO> myUtxos = utxoSet.getUTXOsForPublicKey(wallet.getPublicKey());
        System.out.println("║ My UTXOs: " + String.format("%35d", myUtxos.size()) + "     ║");
        
        if (!myUtxos.isEmpty()) {
            System.out.println("╠════════════════════════════════════════════════════╣");
            for (int i = 0; i < Math.min(5, myUtxos.size()); i++) {
                UTXO utxo = myUtxos.get(i);
                String txShort = utxo.getParentTransactionId().substring(0, 12);
                System.out.println("║   " + (i+1) + ". " + String.format("%6.2f JAC", utxo.getValue()) + 
                                 " from " + txShort + "...                ║");
            }
            if (myUtxos.size() > 5) {
                System.out.println("║   ... and " + (myUtxos.size() - 5) + " more                                  ║");
            }
        }
        
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║ GLOBAL SYSTEM STATE                                ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║ Total UTXOs: " + String.format("%34d", utxoSet.size()) + "     ║");
        System.out.println("║ Total JAC: " + String.format("%34.2f", utxoSet.getTotalValue()) + " JAC ║");
        System.out.println("║ Chain Height: " + String.format("%33d", blockchain.getHeight()) + "     ║");
        System.out.println("║ Mempool: " + String.format("%38d txs", mempool.size()) + " ║");
        
        if (miner != null) {
            System.out.println("╠════════════════════════════════════════════════════╣");
            System.out.println("║ MINING STATS                                       ║");
            System.out.println("╠════════════════════════════════════════════════════╣");
            System.out.println("║ Status: " + String.format("%-41s", miner.isMining() ? "⛏️  MINING" : "⏸️  PAUSED") + "║");
            System.out.println("║ Blocks Found: " + String.format("%33d", miner.getBlocksFound()) + "     ║");
            System.out.println("║ Blocks Lost: " + String.format("%34d", miner.getStaleBlocks()) + "     ║");
        }
        
        System.out.println("╚════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * Debug: Print all UTXOs in the system
     */
    public void debugAllUTXOs() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║              ALL UTXOS IN SYSTEM                   ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        
        List<UTXO> allUtxos = utxoSet.getAllUTXOs();
        
        if (allUtxos.isEmpty()) {
            System.out.println("No UTXOs in system!\n");
            return;
        }
        
        // Group by recipient address
        Map<String, List<UTXO>> byAddress = new HashMap<>();
        for (UTXO utxo : allUtxos) {
            String addr = utxo.getRecipientAddress();
            byAddress.computeIfAbsent(addr, k -> new ArrayList<>()).add(utxo);
        }
        
        System.out.println("Total: " + allUtxos.size() + " UTXOs across " + byAddress.size() + " addresses\n");
        
        int count = 1;
        for (Map.Entry<String, List<UTXO>> entry : byAddress.entrySet()) {
            String addr = entry.getKey();
            List<UTXO> utxos = entry.getValue();
            double total = utxos.stream().mapToDouble(UTXO::getValue).sum();
            
            System.out.println(count + ". Address: " + addr);
            System.out.println("   Total: " + String.format("%.2f JAC", total) + 
                             " (" + utxos.size() + " UTXOs)");
            
            for (UTXO utxo : utxos) {
                System.out.println("   - " + String.format("%6.2f JAC", utxo.getValue()) + 
                                 " | TX: " + utxo.getParentTransactionId().substring(0, 12) + "...");
            }
            System.out.println();
            count++;
        }
    }
    
    /**
     * Debug: Print mempool contents
     */
    public void debugMempool() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║                   MEMPOOL STATE                    ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        
        List<Transaction> txs = mempool.getAllTransactions();
        
        if (txs.isEmpty()) {
            System.out.println("Mempool is empty.\n");
            return;
        }
        
        System.out.println("Pending Transactions: " + txs.size());
        System.out.println("Total Fees: " + String.format("%.2f JAC", mempool.getTotalFees()));
        System.out.println();
        
        for (int i = 0; i < txs.size(); i++) {
            Transaction tx = txs.get(i);
            System.out.println((i+1) + ". TX: " + tx.getTransactionId().substring(0, 16) + "...");
            System.out.println("   Inputs: " + tx.getInputs().size() + 
                             " | Outputs: " + tx.getOutputs().size() + 
                             " | Fee: " + String.format("%.2f JAC", tx.getFee()));
            System.out.println("   Total Out: " + String.format("%.2f JAC", tx.getOutputValue()));
            System.out.println();
        }
    }
    
    /**
     * Debug: Trace a specific transaction
     */
    public void debugTransaction(String txId) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║              TRANSACTION TRACE                     ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("TX ID: " + txId);
        System.out.println();
        
        // Check mempool
        Transaction mempoolTx = mempool.getTransaction(txId);
        if (mempoolTx != null) {
            System.out.println("✅ Found in MEMPOOL");
            System.out.println("   Status: Pending (not yet mined)");
            System.out.println("   Fee: " + String.format("%.2f JAC", mempoolTx.getFee()));
            System.out.println();
            return;
        }
        
        // Check blockchain
        Transaction blockchainTx = blockchain.findTransaction(txId);
        if (blockchainTx != null) {
            System.out.println("✅ Found in BLOCKCHAIN");
            int confirmations = blockchain.getConfirmationCount(txId);
            System.out.println("   Confirmations: " + confirmations);
            System.out.println("   Inputs: " + blockchainTx.getInputs().size());
            System.out.println("   Outputs: " + blockchainTx.getOutputs().size());
            System.out.println("   Fee: " + String.format("%.2f JAC", blockchainTx.getFee()));
            System.out.println();
            
            System.out.println("   Outputs created:");
            for (int i = 0; i < blockchainTx.getOutputs().size(); i++) {
                TransactionOutput output = blockchainTx.getOutputs().get(i);
                String utxoId = txId + ":" + i;
                boolean exists = utxoSet.contains(utxoId);
                System.out.println("   " + (i+1) + ". " + String.format("%.2f JAC", output.getValue()) + 
                                 " | UTXO: " + (exists ? "✅ Unspent" : "❌ Spent"));
            }
            System.out.println();
            return;
        }
        
        System.out.println("❌ Transaction not found in mempool or blockchain");
        System.out.println();
    }

    /**
     * Comprehensive network health check
     */
    public void diagnoseNetwork() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║            NETWORK DIAGNOSTIC REPORT                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Node: " + String.format("%-49s", nodeId) + "║");
        System.out.println("║ Time: " + new java.util.Date() + "            ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        
        // 1. Network Connectivity
        System.out.println("║ 1. NETWORK CONNECTIVITY                                  ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ P2P Port: " + String.format("%-45d", p2pPort) + "║");
        System.out.println("║ HTTP Port: " + String.format("%-44d", httpPort) + "║");
        System.out.println("║ Peer Count: " + String.format("%-43d", peerManager.getPeerCount()) + "║");
        
        List<String> peers = peerManager.getPeerAddresses();
        System.out.println("║ Known Peers:                                             ║");
        for (String peer : peers) {
            System.out.println("║   - " + String.format("%-52s", peer) + "║");
        }
        
        // Test connectivity
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Testing peer connectivity...                             ║");
        int reachable = peerManager.pingAllPeers();
        System.out.println("║ Reachable Peers: " + String.format("%-38d", reachable) + "║");
        
        if (reachable == 0) {
            System.out.println("║ ⚠️  WARNING: NO PEERS REACHABLE!                         ║");
            System.out.println("║    Network is ISOLATED - blocks won't sync!             ║");
        } else if (reachable < peers.size()) {
            System.out.println("║ ⚠️  WARNING: Some peers unreachable                      ║");
        } else {
            System.out.println("║ ✅ All peers reachable                                   ║");
        }
        
        // 2. Blockchain Status
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 2. BLOCKCHAIN STATUS                                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Chain Height: " + String.format("%-43d", blockchain.getHeight()) + "║");
        System.out.println("║ Difficulty: " + String.format("%-45d", blockchain.getDifficulty()) + "║");
        
        boolean valid = blockchain.isChainValid();
        System.out.println("║ Chain Valid: " + String.format("%-42s", valid ? "✅ YES" : "❌ NO") + "║");
        
        // 3. UTXO Set Status
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 3. UTXO SET STATUS                                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Total UTXOs: " + String.format("%-44d", utxoSet.size()) + "║");
        System.out.println("║ Total Value: " + String.format("%-40.2f JAC", utxoSet.getTotalValue()) + "║");
        System.out.println("║ My Balance: " + String.format("%-41.2f JAC", utxoSet.getBalance(wallet.getPublicKey())) + "║");
        
        // 4. Mempool Status
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 4. MEMPOOL STATUS                                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Pending TXs: " + String.format("%-44d", mempool.size()) + "║");
        System.out.println("║ Total Fees: " + String.format("%-41.2f JAC", mempool.getTotalFees()) + "║");
        
        // 5. Mining Status (if miner)
        if (miner != null) {
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║ 5. MINING STATUS                                         ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║ Mining: " + String.format("%-49s", miner.isMining() ? "✅ ACTIVE" : "⏸️  PAUSED") + "║");
            System.out.println("║ Blocks Found: " + String.format("%-43d", miner.getBlocksFound()) + "║");
            System.out.println("║ Stale Blocks: " + String.format("%-43d", miner.getStaleBlocks()) + "║");
            
            int total = miner.getBlocksFound() + miner.getStaleBlocks();
            double winRate = total > 0 ? (miner.getBlocksFound() * 100.0 / total) : 0.0;
            System.out.println("║ Win Rate: " + String.format("%-45.1f%%", winRate) + "║");
        }
        
        // 6. Thread Status
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 6. THREAD STATUS                                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ ServerThread: " + String.format("%-43s", serverThread.isRunning() ? "✅ RUNNING" : "❌ STOPPED") + "║");
        System.out.println("║ MessageHandler: " + String.format("%-41s", messageHandler.isRunning() ? "✅ RUNNING" : "❌ STOPPED") + "║");
        if (miner != null) {
            System.out.println("║ Miner: " + String.format("%-50s", miner.isAlive() ? "✅ RUNNING" : "❌ STOPPED") + "║");
        }
        System.out.println("║ WebServer: " + String.format("%-46s", webServer.isRunning() ? "✅ RUNNING" : "❌ STOPPED") + "║");
        
        // 7. Message Queue
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ 7. MESSAGE QUEUE                                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║ Pending Messages: " + String.format("%-39d", messageQueue.size()) + "║");
        
        if (messageQueue.size() > 100) {
            System.out.println("║ ⚠️  WARNING: Message queue backup!                       ║");
        }
        
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
        
        // Print recommendations
        printRecommendations();
    }

    /**
     * Print health recommendations
     */
    private void printRecommendations() {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check for issues
        if (peerManager.getPeerCount() == 0) {
            issues.add("NO PEERS CONFIGURED - Node is isolated!");
        }
        
        if (peerManager.pingAllPeers() == 0) {
            issues.add("NO PEERS REACHABLE - Check if other nodes are running!");
        }
        
        if (!blockchain.isChainValid()) {
            issues.add("BLOCKCHAIN INVALID - Chain corruption detected!");
        }
        
        if (!serverThread.isRunning()) {
            issues.add("SERVER THREAD STOPPED - Cannot receive messages!");
        }
        
        if (!messageHandler.isRunning()) {
            issues.add("MESSAGE HANDLER STOPPED - Cannot process messages!");
        }
        
        if (messageQueue.size() > 100) {
            warnings.add("Message queue backup - Messages processing slowly");
        }
        
        if (miner != null && !miner.isMining() && role == NodeRole.MINER) {
            warnings.add("Mining is paused - Start mining to compete for blocks");
        }
        
        // Print issues
        if (!issues.isEmpty() || !warnings.isEmpty()) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                    RECOMMENDATIONS                       ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            
            if (!issues.isEmpty()) {
                System.out.println("║ ❌ CRITICAL ISSUES:                                      ║");
                for (String issue : issues) {
                    System.out.println("║   - " + String.format("%-54s", issue) + "║");
                }
            }
            
            if (!warnings.isEmpty()) {
                if (!issues.isEmpty()) {
                    System.out.println("╠══════════════════════════════════════════════════════════╣");
                }
                System.out.println("║ ⚠️  WARNINGS:                                            ║");
                for (String warning : warnings) {
                    System.out.println("║   - " + String.format("%-54s", warning) + "║");
                }
            }
            
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
        } else {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║ ✅ ALL SYSTEMS OPERATIONAL                               ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
        }
    }

    /**
     * Quick status check
     */
    public void quickStatus() {
        System.out.println("\n⚡ Quick Status [" + nodeId + "]");
        System.out.println("   Chain: " + blockchain.getHeight() + " blocks");
        System.out.println("   Balance: " + String.format("%.2f", utxoSet.getBalance(wallet.getPublicKey())) + " JAC");
        System.out.println("   Peers: " + peerManager.getPeerCount() + " configured, " + peerManager.pingAllPeers() + " reachable");
        System.out.println("   Mempool: " + mempool.size() + " pending TXs");
        if (miner != null) {
            System.out.println("   Mining: " + (miner.isMining() ? "⛏️  ACTIVE" : "⏸️  PAUSED") + 
                            " | Found: " + miner.getBlocksFound() + " | Lost: " + miner.getStaleBlocks());
        }
        System.out.println();
    }
    
    // ============================================
    // GETTERS
    // ============================================
    
    public String getNodeId() { 
        return nodeId; 
    }
    
    public int getP2PPort() { 
        return p2pPort; 
    }
    
    public int getHttpPort() { 
        return httpPort; 
    }
    
    public NodeRole getRole() { 
        return role; 
    }
    
    public Blockchain getBlockchain() { 
        return blockchain; 
    }
    
    public UTXOSet getUtxoSet() { 
        return utxoSet; 
    }
    
    public Mempool getMempool() { 
        return mempool; 
    }
    
    public Wallet getWallet() { 
        return wallet; 
    }
    
    public PeerManager getPeerManager() { 
        return peerManager; 
    }
    
    public Miner getMiner() { 
        return miner; 
    }
    
    // public static Wallet getGenesisWallet() {
    //     return genesisWallet;
    // }
    public Wallet getGenesisWalletInstance() {
    return genesisWalletInstance;
}
    private int getDifficulty() {
        return Integer.parseInt(config.getProperty("mining.difficulty", "5"));
    }
    
    private double getBlockReward() {
        return Double.parseDouble(config.getProperty("mining.block.reward", "50.0"));
    }
    
    // ============================================
    // MAIN METHOD
    // ============================================
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java Node <p2pPort> <httpPort> <role>");
            System.err.println("Example: java Node 9081 8081 MINER");
            System.err.println("");
            System.err.println("Roles: ADMIN, MINER, USER");
            System.exit(1);
        }
        
        try {
            int p2pPort = Integer.parseInt(args[0]);
            int httpPort = Integer.parseInt(args[1]);
            NodeRole role = NodeRole.valueOf(args[2].toUpperCase());
            
            Node node = new Node(p2pPort, httpPort, role);
            node.initialize();
            node.start();
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Error: Invalid port number");
            System.err.println("   Ports must be integers (e.g., 9081, 8081)");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error: Invalid role '" + args[2] + "'");
            System.err.println("   Valid roles: ADMIN, MINER, USER");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    /**
     * Safely replace UTXO set contents
     * Used during chain synchronization to maintain consistency
     */
    public void replaceUTXOSet(UTXOSet newUtxoSet) {
        synchronized (utxoSet) {
            System.out.println("   🔄 Replacing UTXO set...");
            System.out.println("      Old: " + utxoSet.size() + " UTXOs, " + 
                            String.format("%.2f JAC", utxoSet.getTotalValue()));
            
            // Clear existing UTXOs
            utxoSet.clear();
            
            // Add new UTXOs
            for (UTXO utxo : newUtxoSet.getAllUTXOs()) {
                utxoSet.addUTXO(utxo);
            }
            
            System.out.println("      New: " + utxoSet.size() + " UTXOs, " + 
                            String.format("%.2f JAC", utxoSet.getTotalValue()));
            System.out.println("   ✅ UTXO set replaced");
        }
    }
    /**
     * Debug: Print current UTXO state with all balances
     * Call this after any UTXO modification to track what's happening
     */
    public void debugUTXOState(String context) {
        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║  🔍 UTXO DEBUG: " + String.format("%-38s", context) + "║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        System.out.println("║  Total UTXOs: " + String.format("%-39d", utxoSet.size()) + "║");
        System.out.println("║  Total Value: " + String.format("%-35.2f JAC", utxoSet.getTotalValue()) + "║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        
        // Group UTXOs by recipient address
        Map<String, Double> balances = new HashMap<>();
        Map<String, Integer> utxoCounts = new HashMap<>();
        
        for (UTXO utxo : utxoSet.getAllUTXOs()) {
            String addr = utxo.getRecipientAddress();
            balances.put(addr, balances.getOrDefault(addr, 0.0) + utxo.getValue());
            utxoCounts.put(addr, utxoCounts.getOrDefault(addr, 0) + 1);
        }
        
        System.out.println("║  Balances by Address:                                 ║");
        System.out.println("╠═══════════════════════════════════════════════════════╣");
        
        if (balances.isEmpty()) {
            System.out.println("║  (No UTXOs in system)                                 ║");
        } else {
            int count = 1;
            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                String addr = entry.getKey();
                double balance = entry.getValue();
                int utxos = utxoCounts.get(addr);
                
                System.out.println("║  " + count + ". " + 
                                String.format("%-20s", addr.substring(0, Math.min(20, addr.length()))) + 
                                String.format("%8.2f JAC", balance) + 
                                " (" + utxos + " UTXOs)" + 
                                String.format("%" + (19 - String.valueOf(utxos).length()) + "s", "") + "║");
                count++;
            }
        }
        
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");
    }

    /**
     * Debug: Print detailed transaction being processed
     */
    public void debugTransaction(Transaction tx, String context) {
        System.out.println("\n🔍 [" + context + "] Transaction Debug:");
        System.out.println("   TX ID: " + tx.getTransactionId().substring(0, 16) + "...");
        System.out.println("   Coinbase: " + tx.isCoinbase());
        System.out.println("   Inputs: " + tx.getInputs().size());
        System.out.println("   Outputs: " + tx.getOutputs().size());
        System.out.println("   Input Value: " + tx.getInputValue() + " JAC");
        System.out.println("   Output Value: " + tx.getOutputValue() + " JAC");
        System.out.println("   Fee: " + tx.getFee() + " JAC");
        
        System.out.println("   📥 Inputs (UTXOs being spent):");
        for (TransactionInput input : tx.getInputs()) {
            UTXO utxo = input.getUtxo();
            if (utxo != null) {
                System.out.println("      - " + utxo.getId() + " | " + 
                                utxo.getValue() + " JAC | " + 
                                utxo.getRecipientAddress().substring(0, 16) + "...");
            } else {
                System.out.println("      - " + input.getUtxoId() + " | UTXO not loaded");
            }
        }
        
        System.out.println("   📤 Outputs (UTXOs being created):");
        for (TransactionOutput output : tx.getOutputs()) {
            String recipientAddr = output.getRecipient().toString();
            // Get simplified address
            String addr = java.util.Base64.getEncoder()
                            .encodeToString(output.getRecipient().getEncoded())
                            .substring(0, 16);
            System.out.println("      + " + output.getValue() + " JAC → " + addr + "...");
        }
        System.out.println();
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.