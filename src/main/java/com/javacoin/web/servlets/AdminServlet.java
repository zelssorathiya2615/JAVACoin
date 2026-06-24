package com.javacoin.web.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.javacoin.Node;
import com.javacoin.core.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class AdminServlet extends HttpServlet {
    
    private final Node node;
    private final Gson gson;
    
    public AdminServlet(Node node) {
        this.node = node;
        this.gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            serveMainPage(resp);
        } else if (pathInfo.equals("/chain")) {
            getBlockchain(resp);
        } else if (pathInfo.equals("/validate")) {
            validateChain(resp);
        } else if (pathInfo.equals("/utxoset")) {
            getUTXOSet(resp);
        } else if (pathInfo.equals("/network")) {
            getNetworkStatus(resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null && pathInfo.equals("/difficulty")) {
            adjustDifficulty(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private void serveMainPage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <title>Admin Console</title>");
        out.println("    <link href='https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@300;400;500&display=swap' rel='stylesheet'>");
        out.println("    <style>");
        out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("        body { font-family: 'IBM Plex Mono', monospace; background: #fafafa; color: #222; font-size: 13px; line-height: 1.5; }");
        out.println("        .container { max-width: 1200px; margin: 0 auto; padding: 40px 24px; }");
        out.println("        .header { border-bottom: 1px solid #ddd; padding-bottom: 16px; margin-bottom: 32px; }");
        out.println("        h1 { font-size: 16px; font-weight: 500; margin-bottom: 4px; }");
        out.println("        .subtitle { font-size: 11px; color: #777; font-weight: 300; }");
        out.println("        .card { background: white; border: 1px solid #ddd; padding: 24px; margin-bottom: 16px; }");
        out.println("        .card-title { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: #999; margin-bottom: 16px; font-weight: 500; }");
        out.println("        .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }");
        out.println("        .info-row:last-child { border: none; }");
        out.println("        .label { font-size: 11px; color: #999; }");
        out.println("        .value { font-size: 11px; }");
        out.println("        .stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }");
        out.println("        .stat-box { background: white; border: 1px solid #ddd; padding: 16px; text-align: center; }");
        out.println("        .stat-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px; color: #999; margin-bottom: 8px; }");
        out.println("        .stat-value { font-size: 20px; font-weight: 400; }");
        out.println("        .btn { padding: 12px 24px; border: 1px solid #222; background: #222; color: white; font-size: 10px; text-transform: uppercase; letter-spacing: 1px; cursor: pointer; font-family: inherit; }");
        out.println("        .btn:hover { background: #000; }");
        out.println("        .btn-secondary { background: white; color: #222; }");
        out.println("        .btn-secondary:hover { background: #f5f5f5; }");
        out.println("        input { padding: 10px; border: 1px solid #ddd; font-family: inherit; font-size: 12px; }");
        out.println("        input:focus { outline: none; border-color: #222; }");
        out.println("        .form-group { margin-bottom: 16px; }");
        out.println("        .form-label { display: block; font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px; color: #777; margin-bottom: 6px; }");
        out.println("        .alert { padding: 12px; margin-top: 12px; font-size: 11px; border: 1px solid; }");
        out.println("        .alert-success { background: #f0f9f0; color: #2e7d32; border-color: #c8e6c9; }");
        out.println("        .alert-error { background: #fff3f3; color: #d32f2f; border-color: #ffcdd2; }");
        out.println("        .block-item { padding: 12px; border: 1px solid #f0f0f0; margin-bottom: 8px; font-size: 11px; }");
        out.println("        .block-header { display: flex; justify-content: space-between; margin-bottom: 4px; font-weight: 500; }");
        out.println("        .block-hash { color: #999; word-break: break-all; }");
        out.println("        .utxo-item { padding: 10px; border: 1px solid #f0f0f0; margin-bottom: 8px; font-size: 11px; }");
        out.println("        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }");
        out.println("        @media (max-width: 768px) { .stat-grid, .grid-2 { grid-template-columns: 1fr; } }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("    <div class='header'>");
        out.println("        <h1>Admin Console</h1>");
        out.println("        <div class='subtitle'>" + node.getNodeId() + " | Network Monitor</div>");
        out.println("    </div>");
        
        // Stats Grid
        out.println("    <div class='stat-grid'>");
        out.println("        <div class='stat-box'>");
        out.println("            <div class='stat-label'>Chain Height</div>");
        out.println("            <div class='stat-value' id='chainHeight'>0</div>");
        out.println("        </div>");
        out.println("        <div class='stat-box'>");
        out.println("            <div class='stat-label'>Total UTXOs</div>");
        out.println("            <div class='stat-value' id='utxoCount'>0</div>");
        out.println("        </div>");
        out.println("        <div class='stat-box'>");
        out.println("            <div class='stat-label'>Mempool</div>");
        out.println("            <div class='stat-value' id='mempoolSize'>0</div>");
        out.println("        </div>");
        out.println("        <div class='stat-box'>");
        out.println("            <div class='stat-label'>Peers</div>");
        out.println("            <div class='stat-value' id='peerCount'>0</div>");
        out.println("        </div>");
        out.println("    </div>");
        
        out.println("    <div class='grid-2'>");
        
        // Chain Validation
        out.println("        <div class='card'>");
        out.println("            <div class='card-title'>Chain Validation</div>");
        out.println("            <button class='btn' onclick='validateChain()' style='width: 100%;'>Validate Blockchain</button>");
        out.println("            <div id='validationResult'></div>");
        out.println("        </div>");
        
        // Network Control
        out.println("        <div class='card'>");
        out.println("            <div class='card-title'>Network Control</div>");
        out.println("            <div class='form-group'>");
        out.println("                <label class='form-label'>Mining Difficulty</label>");
        out.println("                <input type='number' id='newDifficulty' min='1' max='8' value='5'>");
        out.println("            </div>");
        out.println("            <button class='btn' onclick='adjustDifficulty()' style='width: 100%;'>Apply Difficulty</button>");
        out.println("            <div id='difficultyResult'></div>");
        out.println("        </div>");
        
        out.println("    </div>");
        
        // Blockchain Explorer
        out.println("    <div class='card'>");
        out.println("        <div class='card-title'>Blockchain Explorer</div>");
        out.println("        <div id='blockchain' style='max-height: 400px; overflow-y: auto;'>");
        out.println("            <p style='color: #999; text-align: center; padding: 16px;'>Loading...</p>");
        out.println("        </div>");
        out.println("    </div>");
        
        // UTXO Set
        out.println("    <div class='card'>");
        out.println("        <div class='card-title'>Global UTXO Set</div>");
        out.println("        <div id='utxoset' style='max-height: 400px; overflow-y: auto;'>");
        out.println("            <p style='color: #999; text-align: center; padding: 16px;'>Loading...</p>");
        out.println("        </div>");
        out.println("    </div>");
        
        out.println("</div>");
        
        out.println("<script>");
        out.println("async function validateChain() {");
        out.println("    const result = document.getElementById('validationResult');");
        out.println("    result.innerHTML = '<p style=\"color: #999; margin-top: 12px;\">Validating...</p>';");
        out.println("    try {");
        out.println("        const resp = await fetch('/admin/validate');");
        out.println("        const data = await resp.json();");
        out.println("        result.innerHTML = data.valid ? '<div class=\"alert alert-success\">Chain is valid</div>' : '<div class=\"alert alert-error\">Chain is invalid</div>';");
        out.println("    } catch (err) { result.innerHTML = '<div class=\"alert alert-error\">Error</div>'; }");
        out.println("}");
        
        out.println("async function adjustDifficulty() {");
        out.println("    const newDiff = document.getElementById('newDifficulty').value;");
        out.println("    const result = document.getElementById('difficultyResult');");
        out.println("    try {");
        out.println("        const resp = await fetch('/admin/difficulty', {");
        out.println("            method: 'POST',");
        out.println("            headers: { 'Content-Type': 'application/json' },");
        out.println("            body: JSON.stringify({ difficulty: parseInt(newDiff) })");
        out.println("        });");
        out.println("        const data = await resp.json();");
        out.println("        result.innerHTML = data.success ? '<div class=\"alert alert-success\">Updated to ' + newDiff + '</div>' : '<div class=\"alert alert-error\">Failed</div>';");
        out.println("    } catch (err) { result.innerHTML = '<div class=\"alert alert-error\">Error</div>'; }");
        out.println("}");
        
        out.println("async function updateStats() {");
        out.println("    try {");
        out.println("        const resp = await fetch('/admin/network');");
        out.println("        const data = await resp.json();");
        out.println("        document.getElementById('chainHeight').textContent = data.chainHeight;");
        out.println("        document.getElementById('utxoCount').textContent = data.utxoCount;");
        out.println("        document.getElementById('mempoolSize').textContent = data.mempoolSize;");
        out.println("        document.getElementById('peerCount').textContent = data.peerCount;");
        out.println("    } catch (err) { console.error(err); }");
        out.println("}");
        
        out.println("async function updateBlockchain() {");
        out.println("    try {");
        out.println("        const resp = await fetch('/admin/chain');");
        out.println("        const data = await resp.json();");
        out.println("        const container = document.getElementById('blockchain');");
        out.println("        if (data.blocks.length === 0) {");
        out.println("            container.innerHTML = '<p style=\"color: #999; text-align: center; padding: 16px;\">No blocks</p>';");
        out.println("            return;");
        out.println("        }");
        out.println("        container.innerHTML = data.blocks.map(b => `");
        out.println("            <div class='block-item'>");
        out.println("                <div class='block-header'>");
        out.println("                    <span>Block #${b.index}</span>");
        out.println("                    <span>${b.transactions} TXs</span>");
        out.println("                </div>");
        out.println("                <div class='block-hash'>Hash: ${b.hash.substring(0, 40)}...</div>");
        out.println("                <div class='block-hash'>Prev: ${b.previousHash === '0' ? 'GENESIS' : b.previousHash.substring(0, 40) + '...'}</div>");
        out.println("                <div style='margin-top: 8px; color: #999;'>Nonce: ${b.nonce}</div>");
        out.println("            </div>");
        out.println("        `).join('');");
        out.println("    } catch (err) { console.error(err); }");
        out.println("}");
        
        out.println("async function updateUTXOSet() {");
        out.println("    try {");
        out.println("        const resp = await fetch('/admin/utxoset');");
        out.println("        const data = await resp.json();");
        out.println("        const container = document.getElementById('utxoset');");
        out.println("        if (data.utxos.length === 0) {");
        out.println("            container.innerHTML = '<p style=\"color: #999; text-align: center; padding: 16px;\">No UTXOs</p>';");
        out.println("            return;");
        out.println("        }");
        out.println("        container.innerHTML = data.utxos.map(u => `");
        out.println("            <div class='utxo-item'>");
        out.println("                <div style='display: flex; justify-content: space-between;'>");
        out.println("                    <span><strong>${u.amount.toFixed(2)} JAC</strong></span>");
        out.println("                    <span style='color: #999;'>${u.address.substring(0, 20)}...</span>");
        out.println("                </div>");
        out.println("                <div style='color: #999; margin-top: 4px;'>Tx: ${u.txHash.substring(0, 16)}... [${u.index}]</div>");
        out.println("            </div>");
        out.println("        `).join('');");
        out.println("    } catch (err) { console.error(err); }");
        out.println("}");
        
        out.println("updateStats(); updateBlockchain(); updateUTXOSet();");
        out.println("setInterval(() => { updateStats(); updateBlockchain(); updateUTXOSet(); }, 5000);");
        out.println("</script>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private void getBlockchain(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            List<Block> blocks = node.getBlockchain().getChain();
            List<Map<String, Object>> blockList = new ArrayList<>();
            
            for (Block block : blocks) {
                Map<String, Object> blockData = new HashMap<>();
                blockData.put("index", block.getIndex());
                blockData.put("hash", block.getHash());
                blockData.put("previousHash", block.getPreviousHash());
                blockData.put("nonce", block.getNonce());
                blockData.put("timestamp", block.getTimestamp());
                blockData.put("transactions", block.getTransactions().size());
                blockList.add(blockData);
            }
            
            JsonObject json = new JsonObject();
            json.add("blocks", gson.toJsonTree(blockList));
            
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Failed: " + e.getMessage());
        }
    }
    
    private void validateChain(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            boolean valid = node.getBlockchain().isChainValid();
            JsonObject json = new JsonObject();
            json.addProperty("valid", valid);
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Error: " + e.getMessage());
        }
    }
    
    private void getUTXOSet(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            List<UTXO> allUTXOs = node.getUtxoSet().getAllUTXOs();
            List<Map<String, Object>> utxoList = new ArrayList<>();
            
            for (UTXO utxo : allUTXOs) {
                Map<String, Object> utxoData = new HashMap<>();
                utxoData.put("txHash", utxo.getParentTransactionId());
                utxoData.put("index", utxo.getOutputIndex());
                utxoData.put("amount", utxo.getValue());
                utxoData.put("address", utxo.getRecipientAddress());
                utxoList.add(utxoData);
            }
            
            JsonObject json = new JsonObject();
            json.add("utxos", gson.toJsonTree(utxoList));
            
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Failed: " + e.getMessage());
        }
    }
    
    private void getNetworkStatus(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            JsonObject json = new JsonObject();
            json.addProperty("chainHeight", node.getBlockchain().getHeight());
            json.addProperty("utxoCount", node.getUtxoSet().getAllUTXOs().size());
            json.addProperty("mempoolSize", node.getMempool().size());
            json.addProperty("peerCount", node.getPeerManager().getPeerCount());
            
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Failed: " + e.getMessage());
        }
    }
    
    private void adjustDifficulty(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            JsonObject reqData = gson.fromJson(req.getReader(), JsonObject.class);
            int newDifficulty = reqData.get("difficulty").getAsInt();
            
            if (newDifficulty < 1 || newDifficulty > 8) {
                sendError(out, "Difficulty must be 1-8");
                return;
            }
            
            node.getBlockchain().setDifficulty(newDifficulty);
            
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("message", "Difficulty set to " + newDifficulty);
            
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Failed: " + e.getMessage());
        }
    }
    
    private void sendError(PrintWriter out, String message) {
        JsonObject json = new JsonObject();     
        json.addProperty("success", false);
        json.addProperty("error", message);
        out.println(gson.toJson(json));
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.