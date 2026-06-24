package com.javacoin.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

/**
 * ServerThread - Listens for incoming P2P connections
 * Runs continuously on each node, accepting messages from peers
 */
public class ServerThread extends Thread {
    
    private int port;
    private ServerSocket serverSocket;
    private BlockingQueue<Message> incomingMessages;
    private volatile boolean running;
    
    /**
     * Creates a server thread
     * @param port Port to listen on
     * @param incomingMessages Queue to put received messages
     */
    public ServerThread(int port, BlockingQueue<Message> incomingMessages) {
        this.port = port;
        this.incomingMessages = incomingMessages;
        this.running = true;
        this.setName("ServerThread-" + port);
        this.setDaemon(true);
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("🌐 Server listening on port " + port);
            
            while (running) {
                try {
                    // Accept incoming connection
                    Socket clientSocket = serverSocket.accept();
                    
                    // Handle connection in separate thread
                    new Thread(() -> handleConnection(clientSocket)).start();
                    
                } catch (SocketException e) {
                    if (!running) {
                        // Normal shutdown
                        break;
                    }
                    System.err.println("❌ Socket error on port " + port + ": " + e.getMessage());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("❌ Error accepting connection: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Failed to start server on port " + port + ": " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    /**
     * Handles an incoming connection
     * @param socket Client socket
     */
    private void handleConnection(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            // Read message object
            Object obj = in.readObject();
            
            if (obj instanceof Message) {
                Message message = (Message) obj;
                
                // Add to incoming message queue for processing
                incomingMessages.offer(message);
                
                System.out.println("📨 Received: " + message.getType() + 
                                 " from " + message.getSenderId());
            }
            
        } catch (IOException e) {
            // Connection closed or error - normal in P2P
            // Only log if it's not a common disconnection
            if (!e.getMessage().contains("Connection reset") && 
                !e.getMessage().contains("Stream closed")) {
                System.err.println("⚠️  Connection error: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Unknown message type received: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }
    
    /**
     * Stops the server thread
     */
    public void shutdown() {
        running = false;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("🛑 Server stopped on port " + port);
            } catch (IOException e) {
                System.err.println("❌ Error closing server: " + e.getMessage());
            }
        }
    }
    
    /**
     * Checks if server is running
     * @return true if running
     */
    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }
    
    public int getPort() {
        return port;
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.