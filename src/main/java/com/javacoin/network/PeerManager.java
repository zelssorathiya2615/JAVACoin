package com.javacoin.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PeerManager - Manages connections to peer nodes
 * FIXED: Removed premature connectivity testing during initialization
 */
public class PeerManager {
    
    private String nodeId;
    private int myPort;
    private List<String> peerAddresses;
    private ConcurrentHashMap<String, Long> lastSeen;
    private ConcurrentHashMap<String, Integer> failureCount;
    
    private static final int CONNECTION_TIMEOUT = 2000; // 2 seconds
    private static final int MAX_RETRIES = 3;
    
    public PeerManager(String nodeId, int myPort, List<String> peerAddresses) {
        this.nodeId = nodeId;
        this.myPort = myPort;
        this.peerAddresses = new ArrayList<>();
        this.lastSeen = new ConcurrentHashMap<>();
        this.failureCount = new ConcurrentHashMap<>();
        
        // Filter out this node's own address
        for (String peer : peerAddresses) {
            if (!peer.contains(":" + myPort)) {
                this.peerAddresses.add(peer);
                this.failureCount.put(peer, 0);
            }
        }
        
        System.out.println("🤝 PeerManager initialized");
        System.out.println("   My Port: " + myPort);
        System.out.println("   Configured Peers: " + this.peerAddresses.size());
        for (String peer : this.peerAddresses) {
            System.out.println("   - " + peer);
        }
        
        // REMOVED: testConnectivity() - Don't test before all ServerThreads are ready!
        System.out.println("   ℹ️  Connectivity will be tested during first broadcast\n");
    }
    
    /**
     * Test connection to a single peer
     */
    private boolean testConnection(String peerAddress) {
        String[] parts = peerAddress.split(":");
        if (parts.length != 2) return false;
        
        try {
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Broadcasts a message to all peers
     * FIXED: Better error handling and reporting
     */
    public int broadcast(Message message) {
        int successCount = 0;
        List<String> failed = new ArrayList<>();
        
        System.out.println("📡 Broadcasting " + message.getType() + " to " + peerAddresses.size() + " peers...");
        
        for (String peerAddress : peerAddresses) {
            boolean sent = sendToPeer(peerAddress, message);
            if (sent) {
                successCount++;
                failureCount.put(peerAddress, 0); // Reset failure count
            } else {
                failed.add(peerAddress);
                int failures = failureCount.getOrDefault(peerAddress, 0) + 1;
                failureCount.put(peerAddress, failures);
            }
        }
        
        System.out.println("   ✅ Sent to " + successCount + " peers");
        
        if (!failed.isEmpty() && successCount == 0) {
            // Only show detailed errors if ZERO peers reached (complete isolation)
            System.err.println("   ❌ CRITICAL: Failed to reach ANY peers:");
            for (String peer : failed) {
                int failures = failureCount.get(peer);
                System.err.println("      - " + peer + " (failures: " + failures + ")");
            }
            System.err.println("   ⚠️  Check that all nodes are running and ServerThreads are listening!");
        } else if (!failed.isEmpty()) {
            // Some peers unreachable, but not all - less verbose
            System.err.println("   ⚠️  " + failed.size() + " peer(s) unreachable (still propagating to " + successCount + ")");
        }
        
        return successCount;
    }
    
    /**
     * Sends a message to a specific peer
     * FIXED: Better connection handling and timeouts
     */
    public boolean sendToPeer(String peerAddress, Message message) {
        String[] parts = peerAddress.split(":");
        if (parts.length != 2) {
            System.err.println("❌ Invalid peer address: " + peerAddress);
            return false;
        }
        
        String host = parts[0];
        int port;
        
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid port in address: " + peerAddress);
            return false;
        }
        
        Socket socket = null;
        ObjectOutputStream out = null;
        
        try {
            // Create socket with timeout
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            socket.setSoTimeout(CONNECTION_TIMEOUT);
            
            // Send message
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(message);
            out.flush();
            
            // Update last seen
            lastSeen.put(peerAddress, System.currentTimeMillis());
            
            return true;
            
        } catch (SocketTimeoutException e) {
            // Silent for timeouts - peer might not be ready yet
            return false;
        } catch (IOException e) {
            // Silent for "Connection refused" - peer not ready
            if (!e.getMessage().contains("Connection refused")) {
                // Only log unexpected errors
                System.err.println("❌ Error sending to " + peerAddress + ": " + e.getMessage());
            }
            return false;
        } finally {
            // Clean up resources
            try {
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }
    
    /**
     * Gets list of all peer addresses
     */
    public List<String> getPeerAddresses() {
        return new ArrayList<>(peerAddresses);
    }
    
    /**
     * Adds a new peer
     */
    public void addPeer(String peerAddress) {
        if (!peerAddresses.contains(peerAddress) && !peerAddress.contains(":" + myPort)) {
            peerAddresses.add(peerAddress);
            failureCount.put(peerAddress, 0);
            System.out.println("➕ Added new peer: " + peerAddress);
        }
    }
    
    /**
     * Removes a peer
     */
    public void removePeer(String peerAddress) {
        if (peerAddresses.remove(peerAddress)) {
            failureCount.remove(peerAddress);
            System.out.println("➖ Removed peer: " + peerAddress);
        }
    }
    
    /**
     * Gets number of connected peers
     */
    public int getPeerCount() {
        return peerAddresses.size();
    }
    
    /**
     * Pings all peers to check connectivity
     * FIXED: Returns actual reachable count
     */
    public int pingAllPeers() {
        int reachable = 0;
        
        for (String peer : peerAddresses) {
            if (testConnection(peer)) {
                reachable++;
            }
        }
        
        return reachable;
    }
    
    /**
     * Gets info about a specific peer
     */
    public String getPeerInfo(String peerAddress) {
        Long lastSeenTime = lastSeen.get(peerAddress);
        int failures = failureCount.getOrDefault(peerAddress, 0);
        
        StringBuilder info = new StringBuilder(peerAddress);
        
        if (lastSeenTime != null) {
            long secondsAgo = (System.currentTimeMillis() - lastSeenTime) / 1000;
            info.append(" (last seen: ").append(secondsAgo).append("s ago");
        } else {
            info.append(" (never contacted");
        }
        
        if (failures > 0) {
            info.append(", failures: ").append(failures);
        }
        
        info.append(")");
        return info.toString();
    }
    
    /**
     * Get detailed peer status - NOW SAFE TO CALL ANYTIME
     */
    public void printPeerStatus() {
        System.out.println("\n📊 Peer Status Report:");
        System.out.println("   Total peers: " + peerAddresses.size());
        
        int reachable = 0;
        for (String peer : peerAddresses) {
            boolean canReach = testConnection(peer);
            if (canReach) reachable++;
            
            String status = canReach ? "✅" : "❌";
            int failures = failureCount.getOrDefault(peer, 0);
            Long lastSeenTime = lastSeen.get(peer);  
            
            System.out.print("   " + status + " " + peer);
            if (lastSeenTime != null) {
                long ago = (System.currentTimeMillis() - lastSeenTime) / 1000;
                System.out.print(" (last: " + ago + "s ago)");
            }
            if (failures > 0) {
                System.out.print(" [failures: " + failures + "]");
            }
            System.out.println();
        }
        
        System.out.println("   Reachable: " + reachable + "/" + peerAddresses.size());
        System.out.println();
    }
    
    @Override
    public String toString() {
        return String.format("PeerManager{nodeId='%s', port=%d, peers=%d}", 
                           nodeId, myPort, peerAddresses.size());
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.