# 🏗️ JAVACoin — Architecture Guide

## System Architecture

JAVACoin runs as a network of 6 independent Java processes on a single machine. Each process is a `Node` that contains all the components needed to participate in the cryptocurrency network.

## Node Components

### 1. Blockchain (`core/Blockchain.java`)
- Maintains the chain of blocks as an `ArrayList<Block>`
- Validates chain integrity (hash linking, PoW, sequential indices)
- Supports fork resolution via `replaceChain()` (longest valid chain wins)
- Can rebuild the UTXO set from scratch by replaying all blocks

### 2. UTXO Set (`core/UTXOSet.java`)
- Thread-safe ledger using `ConcurrentHashMap<String, UTXO>`
- Tracks all unspent coins in the system
- Balance = sum of UTXOs owned by a public key
- Updated atomically when blocks are processed

### 3. Mempool (`core/Mempool.java`)
- Thread-safe queue of unconfirmed transactions (`ConcurrentLinkedQueue`)
- Miners select highest-fee transactions first
- Cleaned after block confirmation (confirmed TXs removed)
- Validated against current UTXO set

### 4. Miner (`mining/Miner.java`)
- Daemon thread running continuous PoW computation
- Builds candidate blocks from mempool transactions
- Uses temp UTXO set clone to validate TX selection without modifying real state
- Detects stale blocks (competitor mined same height first)
- Broadcasts discovered blocks to network

### 5. Network Layer (`network/`)
- **ServerThread:** TCP `ServerSocket` listening for incoming connections
- **PeerManager:** Maintains peer list, sends messages via TCP sockets
- **MessageHandler:** Processes messages from a `BlockingQueue` (producer-consumer)
- **ChainSynchronizer:** Handles catch-up sync and fork resolution
- **Message:** Serializable message protocol with type-specific payloads

### 6. Web Layer (`web/`)
- **EmbeddedServer:** Jetty HTTP server with role-specific servlet routing
- **AdminServlet:** Network monitoring dashboard
- **MinerServlet:** Mining controls and balance display
- **UserServlet:** Transaction creation and history

## Thread Model

Each node runs 5+ concurrent threads:

| Thread | Class | Lifecycle |
|--------|-------|-----------|
| Main | `Node` | Init → status loop → join (blocks forever) |
| P2P Server | `ServerThread` | Daemon, listens on TCP port |
| Message Handler | `MessageHandler` | Daemon, blocks on queue.take() |
| Miner | `Miner` | Daemon, continuous PoW loop |
| Status Reporter | Anonymous | Daemon, prints status every 30s |
| Connection Handlers | Anonymous | Per-connection, short-lived |

## Startup Sequence (Critical Order)

1. Load configuration
2. Generate wallet
3. Load genesis block from file
4. Initialize blockchain with genesis
5. Build UTXO set from chain
6. **Start ServerThread** (MUST be first — peers need a listener)
7. Wait 500ms for port binding
8. Create PeerManager
9. Start MessageHandler
10. Create ChainSynchronizer and link to MessageHandler
11. Wait 3s for network stabilization
12. Start web server
13. Start miner (if MINER role)
14. Sync with network (if not creator miner)

## Data Flow

```
Transaction Creation:
  User → Wallet.sign() → PeerManager.broadcast() → TCP → 
  Other nodes' ServerThread → MessageQueue → MessageHandler → 
  Validate → Mempool.addTransaction()

Block Mining:
  Miner → Mempool.getTopTransactions() → Block.mineBlock() →
  Blockchain.addBlockToChain() → UTXOSet update → 
  PeerManager.broadcast() → All peers receive and validate

Chain Sync:
  New node → broadcast(REQUEST_CHAIN) → Peer responds with chain →
  ChainSynchronizer validates → replaceChain() if longer → 
  rebuildUTXOSet() → cleanMempool()
```
