package com.javacoin.web.server;

import com.javacoin.Node;
import com.javacoin.web.servlets.AdminServlet;
import com.javacoin.web.servlets.MinerServlet;
import com.javacoin.web.servlets.UserServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import java.net.URL;

/**
 * EmbeddedServer - Jetty HTTP server for web interface
 * Serves role-specific servlets on each node's HTTP port
 */
public class EmbeddedServer {
    
    private final Server server;
    private final Node node;
    private final int httpPort;
    private final Node.NodeRole role;
    
    /**
     * Creates embedded Jetty server
     * @param node Parent node instance
     * @param httpPort Port for HTTP server
     */
    public EmbeddedServer(Node node, int httpPort) {
        this.node = node;
        this.httpPort = httpPort;
        this.role = node.getRole();
        this.server = new Server(httpPort);
        
        configureServer();
    }
    
    /**
     * Configures Jetty server with servlets and static resources
     */
    private void configureServer() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Serve static resources (CSS, JS)
        try {
            URL resourceUrl = getClass().getClassLoader().getResource("static");
            if (resourceUrl != null) {
                context.setBaseResource(Resource.newResource(resourceUrl));
                ServletHolder staticHolder = new ServletHolder("static", DefaultServlet.class);
                staticHolder.setInitParameter("dirAllowed", "false");
                context.addServlet(staticHolder, "/static/*");
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not load static resources: " + e.getMessage());
        }
        
        // Register role-specific servlets
        switch (role) {
            case ADMIN:
                context.addServlet(new ServletHolder(new AdminServlet(node)), "/admin/*");
                context.addServlet(new ServletHolder(new AdminServlet(node)), "/");
                System.out.println("   📊 Admin servlet registered at /admin");
                break;
                
            case MINER:
                context.addServlet(new ServletHolder(new MinerServlet(node)), "/miner/*");
                context.addServlet(new ServletHolder(new MinerServlet(node)), "/");
                System.out.println("   ⛏️  Miner servlet registered at /miner");
                break;
                
            case USER:
                context.addServlet(new ServletHolder(new UserServlet(node)), "/user/*");
                context.addServlet(new ServletHolder(new UserServlet(node)), "/");
                System.out.println("   👤 User servlet registered at /user");
                break;
        }
        
        server.setHandler(context);
    }
    
    /**
     * Starts the HTTP server
     */
    public void start() {
        try {
            server.start();
            String endpoint = switch (role) {
                case ADMIN -> "/admin";
                case MINER -> "/miner";
                case USER -> "/user";
            };
            System.out.println("   🌐 Web interface: http://localhost:" + httpPort + endpoint);
        } catch (Exception e) {
            System.err.println("❌ Failed to start HTTP server on port " + httpPort);
            e.printStackTrace();
        }
    }
    
    /**
     * Stops the HTTP server gracefully
     */
    public void stop() {
        try {
            if (server != null && !server.isStopped()) {
                server.stop();
                System.out.println("   ✅ HTTP server stopped");
            }
        } catch (Exception e) {
            System.err.println("⚠️  Error stopping HTTP server: " + e.getMessage());
        }
    }
    
    /**
     * Checks if server is running
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
    
    /**
     * Gets the HTTP port
     */
    public int getPort() {
        return httpPort;
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.