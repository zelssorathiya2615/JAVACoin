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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class MinerServlet extends HttpServlet {
    
    private final Node node;
    private final Gson gson;
    
    public MinerServlet(Node node) {
        this.node = node;
        this.gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            serveMainPage(resp);
        } else if (pathInfo.equals("/stats")) {
            getMiningStats(resp);
        } else if (pathInfo.equals("/mempool")) {
            getMempool(resp);
        } else if (pathInfo.equals("/balance")) {
            getBalance(resp);
        } else if (pathInfo.equals("/utxos")) {
            getUTXOs(resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null) {
            if (pathInfo.equals("/start")) {
                startMining(resp);
            } else if (pathInfo.equals("/stop")) {
                stopMining(resp);
            } else if (pathInfo.equals("/send")) {
                sendTransaction(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
    
    private void serveMainPage(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <title>Miner Console</title>");
        out.println("    <link href='https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@300;400;500&display=swap' rel='stylesheet'>");
        out.println("    <style>");
        out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("        body { font-family: 'IBM Plex Mono', monospace; background: #fafafa; color: #222; font-size: 13px; line-height: 1.5; }");
        out.println("        .container { max-width: 1000px; margin: 0 auto; padding: 40px 24px; }");
        out.println("        .header { border-bottom: 1px solid #ddd; padding-bottom: 16px; margin-bottom: 32px; }");
        out.println("        h1 { font-size: 16px; font-weight: 500; margin-bottom: 4px; }");
        out.println("        .subtitle { font-size: 11px; color: #777; font-weight: 300; }");
        out.println("        .card { background: white; border: 1px solid #ddd; padding: 24px; margin-bottom: 16px; }");
        out.println("        .card-title { font-size: 10px; text-transform: uppercase; letter-spacing: 1px; color: #999; margin-bottom: 16px; font-weight: 500; }");
        out.println("        .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }");
        out.println("        .info-row:last-child { border: none; }");
        out.println("        .label { font-size: 11px; color: #999; }");
        out.println("        .value { font-size: 11px; }");
        out.println("        .status { font-size: 24px; font-weight: 300; margin: 16px 0; }");
        out.println("        .status.mining { color: #2e7d32; }");
        out.println("        .status.idle { color: #999; }");
        out.println("        .form-group { margin-bottom: 16px; }");
        out.println("        .form-label { display: block; font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px; color: #777; margin-bottom: 6px; }");
        out.println("        input, textarea { width: 100%; padding: 10px; border: 1px solid #ddd; font-family: inherit; font-size: 12px; }");
        out.println("        input:focus, textarea:focus { outline: none; border-color: #222; }");
        out.println("        textarea { resize: vertical; min-height: 80px; }");
        out.println("        .btn { padding: 12px 24px; border: 1px solid #222; background: #222; color: white; font-size: 10px; text-transform: uppercase; letter-spacing: 1px; cursor: pointer; font-family: inherit; margin-right: 8px; }");
        out.println("        .btn:hover { background: #000; }");
        out.println("        .btn:disabled { opacity: 0.3; cursor: not-allowed; }");
        out.println("        .btn-secondary { background: white; color: #222; }");
        out.println("        .btn-secondary:hover { background: #f5f5f5; }");
        out.println("        .alert { padding: 12px; margin-top: 12px; font-size: 11px; border: 1px solid; }");
        out.println("        .alert-success { background: #f0f9f0; color: #2e7d32; border-color: #c8e6c9; }");
        out.println("        .alert-error { background: #fff3f3; color: #d32f2f; border-color: #ffcdd2; }");
        out.println("        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }");
        out.println("        .stat-value { font-size: 20px; font-weight: 400; margin: 8px 0; }");
        out.println("        .mempool-tx { padding: 10px; border: 1px solid #f0f0f0; margin-bottom: 8px; font-size: 11px; }");
        out.println("        .copy-btn { padding: 8px 16px; background: white; border: 1px solid #ddd; cursor: pointer; font-size: 10px; font-family: inherit; text-transform: uppercase; letter-spacing: 0.5px; }");
        out.println("        .copy-btn:hover { background: #f5f5f5; }");
        out.println("        @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("    <div class='header'>");
        out.println("        <h1>Miner Console</h1>");
        out.println("        <div class='subtitle'>" + node.getNodeId() + "</div>");
        out.println("    </div>");
        
        // Public Key Display
        out.println("    <div class='card'>");
        out.println("        <div class='card-title'>Your Public Key</div>");
        out.println("        <div class='info-row'>");
        out.println("            <span class='label'>Address</span>");
        out.println("            <span class='value'>" + node.getWallet().getAddress() + "</span>");
        out.println("        </div>");
        out.println("        <div style='margin-top: 16px;'>");
        out.println("            <label class='form-label'>Full Public Key (Share for receiving)</label>");
        out.println("            <textarea readonly id='publicKey' style='font-size: 10px;'>" + node.getWallet().getPublicKeyString() + "</textarea>");
        out.println("            <button class='copy-btn' onclick='copyPublicKey()'>Copy to Clipboard</button>");
        out.println("        </div>");
        out.println("    </div>");
        
        // Mining Status
        out.println("    <div class='card'>");
        out.println("        <div class='card-title'>Mining Status</div>");
        out.println("        <div class='status' id='miningStatus'>IDLE</div>");
        out.println("        <button id='startBtn' class='btn' onclick='startMining()'>Start Mining</button>");
        out.println("        <button id='stopBtn' class='btn btn-secondary' onclick='stopMining()' disabled>Stop Mining</button>");
        out.println("        <div id='controlResult'></div>");
        out.println("    </div>");
        
        out.println("    <div class='grid'>");
        
        // Balance Card
        out.println("        <div class='card'>");
        out.println("            <div class='card-title'>Balance</div>");
        out.println("            <div class='stat-value' id='balance'>0.00 JAC</div>");
        out.println("            <div style='margin-top: 12px; padding-top: 12px; border-top: 1px solid #f5f5f5;'>");
        out.println("                <div class='info-row'><span class='label'>Blocks Won</span><span class='value' id='blocksMined'>0</span></div>");
        out.println("                <div class='info-row'><span class='label'>Blocks Lost</span><span class='value' id='staleBlocks'>0</span></div>");
        out.println("                <div class='info-row'><span class='label'>Chain Height</span><span class='value' id='height'>0</span></div>");
        out.println("            </div>");
        out.println("        </div>");
        
        // Send Transaction Card
        out.println("        <div class='card'>");
        out.println("            <div class='card-title'>Send JAC</div>");
        out.println("            <form id='sendForm'>");
        out.println("                <div class='form-group'>");
        out.println("                    <label class='form-label'>Recipient Public Key</label>");
        out.println("                    <textarea id='recipient' placeholder='Paste recipient public key here' required></textarea>");
        out.println("                </div>");
        out.println("                <div class='form-group'>");
        out.println("                    <label class='form-label'>Amount (JAC)</label>");
        out.println("                    <input type='number' id='amount' step='0.01' min='0.01' required>");
        out.println("                </div>");
        out.println("                <div class='form-group'>");
        out.println("                    <label class='form-label'>Fee (JAC)</label>");
        out.println("                    <input type='number' id='fee' value='1' step='0.01' min='0.01' required>");
        out.println("                </div>");
        out.println("                <button type='submit' class='btn' style='width: 100%;'>Send</button>");
        out.println("            </form>");
        out.println("            <div id='sendResult'></div>");
        out.println("        </div>");
        
        out.println("    </div>");
        
        // Mempool
        out.println("    <div class='card'>");
        out.println("        <div class='card-title'>Mempool</div>");
        out.println("        <div id='mempool' style='max-height: 300px; overflow-y: auto;'>");
        out.println("            <div style='color: #999; text-align: center; padding: 16px;'>Loading...</div>");
        out.println("        </div>");
        out.println("    </div>");
        
        out.println("</div>");
        
        out.println("<script>");
        out.println("function copyPublicKey() {");
        out.println("    const key = document.getElementById('publicKey');");
        out.println("    key.select();");
        out.println("    document.execCommand('copy');");
        out.println("    alert('Public key copied to clipboard!');");
        out.println("}");
        
        out.println("async function startMining() {");
        out.println("    try {");
        out.println("        const resp = await fetch('/miner/start', { method: 'POST' });");
        out.println("        const data = await resp.json();");
        out.println("        if (data.success) {");
        out.println("            document.getElementById('controlResult').innerHTML = '<div class=\"alert alert-success\">Mining started</div>';");
        out.println("            updateStats();");
        out.println("        }");
        out.println("    } catch (err) { console.error(err); }");
        out.println("}");
        
        out.println("async function stopMining() {");
        out.println("    try {");
        out.println("        const resp = await fetch('/miner/stop', { method: 'POST' });");
        out.println("        const data = await resp.json();");
        out.println("        if (data.success) {");
        out.println("            document.getElementById('controlResult').innerHTML = '<div class=\"alert alert-success\">Mining stopped</div>';");
        out.println("            updateStats();");
        out.println("        }");
        out.println("    } catch (err) { console.error(err); }");
        out.println("}");
        
        out.println("async function updateStats() {");
        out.println("    try {");
        out.println("        const [stats, bal] = await Promise.all([");
        out.println("            fetch('/miner/stats').then(r => r.json()),");
        out.println("            fetch('/miner/balance').then(r => r.json())");
        out.println("        ]);");
        out.println("        const status = document.getElementById('miningStatus');");
        out.println("        status.textContent = stats.isMining ? 'MINING' : 'IDLE';");
        out.println("        status.className = stats.isMining ? 'status mining' : 'status idle';");
        out.println("        document.getElementById('balance').textContent = bal.balance.toFixed(2) + ' JAC';");
        out.println("        document.getElementById('blocksMined').textContent = stats.blocksMined;");
        out.println("        document.getElementById('staleBlocks').textContent = stats.staleBlocks;");
        out.println("        document.getElementById('height').textContent = stats.chainHeight;");
        out.println("        document.getElementById('startBtn').disabled = stats.isMining;");
        out.println("        document.getElementById('stopBtn').disabled = !stats.isMining;");
        out.println("    } catch(e) { console.error(e); }");
        out.println("}");
        
        out.println("async function updateMempool() {");
        out.println("    try {");
        out.println("        const resp = await fetch('/miner/mempool');");
        out.println("        const data = await resp.json();");
        out.println("        const container = document.getElementById('mempool');");
        out.println("        if (data.transactions.length === 0) {");
        out.println("            container.innerHTML = '<div style=\"color: #999; text-align: center; padding: 16px;\">Mempool empty</div>';");
        out.println("            return;");
        out.println("        }");
        out.println("        container.innerHTML = data.transactions.map((tx, i) => `");
        out.println("            <div class='mempool-tx'>");
        out.println("                <div><strong>#${i+1}</strong> ${tx.hash.substring(0, 16)}...</div>");
        out.println("                <div style='color: #999;'>Fee: ${tx.fee.toFixed(2)} JAC</div>");
        out.println("            </div>");
        out.println("        `).join('');");
        out.println("    } catch(e) { console.error(e); }");
        out.println("}");
        
        out.println("document.getElementById('sendForm').addEventListener('submit', async (e) => {");
        out.println("    e.preventDefault();");
        out.println("    const result = document.getElementById('sendResult');");
        out.println("    result.innerHTML = '';");
        out.println("    try {");
        out.println("        const resp = await fetch('/miner/send', {");
        out.println("            method: 'POST',");
        out.println("            headers: { 'Content-Type': 'application/json' },");
        out.println("            body: JSON.stringify({");
        out.println("                recipient: document.getElementById('recipient').value.trim(),");
        out.println("                amount: parseFloat(document.getElementById('amount').value),");
        out.println("                fee: parseFloat(document.getElementById('fee').value)");
        out.println("            })");
        out.println("        });");
        out.println("        const data = await resp.json();");
        out.println("        if (data.success) {");
        out.println("            result.innerHTML = '<div class=\"alert alert-success\">Sent successfully</div>';");
        out.println("            e.target.reset();");
        out.println("            document.getElementById('fee').value = '1';");
        out.println("            setTimeout(() => { updateStats(); updateMempool(); }, 2000);");
        out.println("        } else {");
        out.println("            result.innerHTML = '<div class=\"alert alert-error\">' + data.error + '</div>';");
        out.println("        }");
        out.println("    } catch(err) {");
        out.println("        result.innerHTML = '<div class=\"alert alert-error\">Error: ' + err.message + '</div>';");
        out.println("    }");
        out.println("});");
        
        out.println("updateStats();");
        out.println("updateMempool();");
        out.println("setInterval(() => { updateStats(); updateMempool(); }, 5000);");
        out.println("</script>");
        out.println("</body>");
        out.println("</html>");
    }
    
    private void getMiningStats(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            JsonObject json = new JsonObject();
            
            if (node.getMiner() != null) {
                json.addProperty("isMining", node.getMiner().isMining());
                json.addProperty("blocksMined", node.getMiner().getBlocksFound());
                json.addProperty("staleBlocks", node.getMiner().getStaleBlocks());
            } else {
                json.addProperty("isMining", false);
                json.addProperty("blocksMined", 0);
                json.addProperty("staleBlocks", 0);
            }
            
            json.addProperty("chainHeight", node.getBlockchain().getHeight());
            
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Failed to get stats: " + e.getMessage());
        }
    }
    
    private void getBalance(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            PublicKey pubKey = node.getWallet().getPublicKey();
            JsonObject json = new JsonObject();
            json.addProperty("balance", node.getUtxoSet().getBalance(pubKey));
            json.addProperty("utxoCount", node.getUtxoSet().getUTXOsForPublicKey(pubKey).size());
            json.addProperty("height", node.getBlockchain().getHeight());
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, e.getMessage());
        }
    }
    
    private void getUTXOs(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            List<UTXO> utxos = node.getUtxoSet().getUTXOsForPublicKey(node.getWallet().getPublicKey());
            List<Map<String, Object>> list = new ArrayList<>();
            
            for (UTXO u : utxos) {
                Map<String, Object> m = new HashMap<>();
                m.put("txHash", u.getParentTransactionId());
                m.put("index", u.getOutputIndex());
                m.put("amount", u.getValue());
                list.add(m);
            }
            
            JsonObject json = new JsonObject();
            json.add("utxos", gson.toJsonTree(list));
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, e.getMessage());
        }
    }
    
    private void getMempool(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            List<Transaction> transactions = node.getMempool().getTransactionsSortedByFee();
            
            List<Map<String, Object>> txList = new ArrayList<>();
            double totalFees = 0;
            
            for (Transaction tx : transactions) {
                double fee = tx.getFee();
                totalFees += fee;
                
                Map<String, Object> txData = new HashMap<>();
                txData.put("hash", tx.getTransactionId());
                txData.put("fee", fee);
                txData.put("inputs", tx.getInputs().size());
                txData.put("outputs", tx.getOutputs().size());
                txList.add(txData);
            }
            
            JsonObject json = new JsonObject();
            json.add("transactions", gson.toJsonTree(txList));
            json.addProperty("totalFees", totalFees);
            json.addProperty("count", transactions.size());
            
            out.println(gson.toJson(json));
        } catch (Exception e) {
            sendError(out, "Failed to get mempool: " + e.getMessage());
        }
    }
    
    private void sendTransaction(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            JsonObject reqData = gson.fromJson(req.getReader(), JsonObject.class);
            String recipientKey = reqData.get("recipient").getAsString().trim();
            double amount = reqData.get("amount").getAsDouble();
            double fee = reqData.get("fee").getAsDouble();
            
            if (recipientKey.isEmpty()) {
                sendError(out, "Recipient public key required");
                return;
            }
            
            // Parse Base64 public key
            PublicKey recipientPubKey;
            try {
                byte[] keyBytes = Base64.getDecoder().decode(recipientKey);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
                recipientPubKey = kf.generatePublic(spec);
            } catch (Exception e) {
                sendError(out, "Invalid public key format: " + e.getMessage());
                return;
            }
            
            PublicKey senderPubKey = node.getWallet().getPublicKey();
            double balance = node.getUtxoSet().getBalance(senderPubKey);
            
            if (balance < amount + fee) {
                sendError(out, "Insufficient balance. Have: " + String.format("%.2f", balance) + " JAC, need: " + String.format("%.2f", amount + fee) + " JAC");
                return;
            }
            
            List<UTXO> senderUTXOs = node.getUtxoSet().getUTXOsForPublicKey(senderPubKey);
            if (senderUTXOs.isEmpty()) {
                sendError(out, "No UTXOs available");
                return;
            }
            
            // Select UTXOs for inputs
            List<TransactionInput> inputs = new ArrayList<>();
            double inputTotal = 0;
            
            for (UTXO utxo : senderUTXOs) {
                inputs.add(new TransactionInput(utxo));
                inputTotal += utxo.getValue();
                if (inputTotal >= amount + fee) break;
            }
            
            if (inputTotal < amount + fee) {
                sendError(out, "Could not gather enough UTXOs. Have: " + String.format("%.2f", inputTotal) + " JAC");
                return;
            }
            
            // Create outputs
            List<TransactionOutput> outputs = new ArrayList<>();
            outputs.add(new TransactionOutput(recipientPubKey, amount, "temp"));
            
            double change = inputTotal - amount - fee;
            if (change > 0.001) {
                outputs.add(new TransactionOutput(senderPubKey, change, "temp"));
            }
            
            // Create and sign transaction
            Transaction tx = new Transaction(senderPubKey, inputs, outputs);
            tx.signTransaction(node.getWallet().getPrivateKey());
            
            // Validate before adding
            if (!tx.isValid(node.getUtxoSet())) {
                sendError(out, "Transaction validation failed");
                return;
            }
            
            // Add to local mempool
            boolean added = node.getMempool().addTransaction(tx);
            if (!added) {
                sendError(out, "Failed to add transaction to mempool (duplicate?)");
                return;
            }
            
            System.out.println("📤 [" + node.getNodeId() + "] Creating transaction:");
            System.out.println("   From: " + node.getWallet().getAddress().substring(0, 16) + "...");
            System.out.println("   Amount: " + amount + " JAC");
            System.out.println("   Fee: " + fee + " JAC");
            System.out.println("   TX ID: " + tx.getTransactionId().substring(0, 16) + "...");
            
            // CRITICAL: Broadcast to ALL peers
            com.javacoin.network.Message msg = com.javacoin.network.Message.newTransaction(tx, node.getNodeId());
            int sent = node.getPeerManager().broadcast(msg);
            
            System.out.println("📡 [" + node.getNodeId() + "] Transaction broadcast to " + sent + " peers");
            System.out.println("   Waiting for miners to include in next block...");
            
            // Success response
            JsonObject json = new JsonObject();
            json.addProperty("success", true);
            json.addProperty("txHash", tx.getTransactionId());
            json.addProperty("broadcasted", sent);
            json.addProperty("message", "Transaction sent to " + sent + " nodes. Waiting for mining...");
            out.println(gson.toJson(json));
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError(out, "Failed: " + e.getMessage());
        }
    }
    
    private void startMining(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            if (node.getMiner() != null) {
                node.getMiner().startMining();
                
                JsonObject json = new JsonObject();
                json.addProperty("success", true);
                json.addProperty("message", "Mining started");
                out.println(gson.toJson(json));
            } else {
                sendError(out, "Miner not initialized");
            }
        } catch (Exception e) {
            sendError(out, "Failed to start mining: " + e.getMessage());
        }
    }
    
    private void stopMining(HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        
        try {
            if (node.getMiner() != null) {
                node.getMiner().stopMining();
                
                JsonObject json = new JsonObject();
                json.addProperty("success", true);
                json.addProperty("message", "Mining stopped");
                out.println(gson.toJson(json));
            } else {
                sendError(out, "Miner not initialized");
            }
        } catch (Exception e) {
            sendError(out, "Failed to stop mining: " + e.getMessage());
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