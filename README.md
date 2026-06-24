<div align="center">

# 🪙 JAVACoin

### A Decentralized Cryptocurrency Built in Java

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)
[![Blockchain](https://img.shields.io/badge/Blockchain-PoW-blue?style=for-the-badge)](#-proof-of-work-mining)
[![Crypto](https://img.shields.io/badge/Crypto-secp256k1-orange?style=for-the-badge)](#-cryptography)

**A fully functional 6-node decentralized cryptocurrency simulation featuring Proof-of-Work mining, UTXO transactions, ECDSA digital signatures, P2P networking, and a web interface — all running on your local machine.**

[Quick Start](#-quick-start) · [Architecture](#-architecture) · [How It Works](#-how-it-works) · [Bitcoin Comparison](#-bitcoin-comparison) · [Documentation](#-documentation)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Quick Start](#-quick-start)
- [Architecture](#-architecture)
- [How It Works](#-how-it-works)
- [Network Topology](#-network-topology)
- [OOP Concepts Used](#-oop-concepts-used)
- [Bitcoin Comparison](#-bitcoin-comparison)
- [Project Structure](#-project-structure)
- [Configuration](#%EF%B8%8F-configuration)
- [Documentation](#-documentation)
- [Tech Stack](#-tech-stack)
- [License](#-license)

---

## 🌟 Overview

JAVACoin is a **complete cryptocurrency implementation** built entirely in Java as a deep exploration of blockchain technology, cryptography, distributed systems, and object-oriented programming.

Unlike simplified blockchain tutorials that only implement hashing and linked lists, JAVACoin implements the **real mechanisms** that power Bitcoin:

- ✅ **UTXO Transaction Model** — Same model Bitcoin uses (not account-based)
- ✅ **ECDSA secp256k1 Signatures** — The exact same elliptic curve as Bitcoin
- ✅ **SHA-256 Proof-of-Work** — Real mining with adjustable difficulty
- ✅ **Merkle Trees** — Efficient transaction verification
- ✅ **P2P Network** — 6 nodes communicating over TCP sockets
- ✅ **Fork Resolution** — Longest valid chain wins (Nakamoto Consensus)
- ✅ **Web Interface** — Role-specific dashboards for Admin, Miner, and User nodes
- ✅ **Chain Synchronization** — Nodes that join late catch up automatically

---

## ✨ Features

### 🔐 Cryptographic Foundation
- **SHA-256 hashing** for block headers, transaction IDs, and Merkle roots
- **ECDSA digital signatures** using Bitcoin's secp256k1 curve (via Bouncy Castle)
- **Wallet generation** with unique addresses derived from public key hashes

### ⛏️ Proof-of-Work Mining
- Background miner threads that race to find valid blocks
- Configurable difficulty (leading zeros in hash)
- Mining statistics: hash rate, attempts, block discovery time
- Stale block detection when competitors find blocks first

### 💰 UTXO Transaction Model
- Inputs reference previous unspent outputs (coins to spend)
- Outputs create new UTXOs (coins for recipients)
- Change outputs return unspent value to sender
- Implicit transaction fees (inputs - outputs = fee)
- Coinbase transactions for mining rewards

### 🌐 P2P Networking
- 6 nodes running on localhost with separate P2P and HTTP ports
- TCP socket-based message passing (Java Object Serialization)
- Message types: NEW_BLOCK, NEW_TRANSACTION, REQUEST_CHAIN, PING/PONG
- Automatic chain synchronization for late-joining nodes
- Fork resolution using longest valid chain rule

### 🖥️ Web Interface
- **Admin Dashboard** — Network overview, all node statuses
- **Miner Dashboard** — Mining controls, block discovery stats, balance
- **User Dashboard** — Send transactions, check balance, view transaction history

### 📋 Mempool & Fee Market
- Unconfirmed transactions queued in memory pool
- Miners prioritize highest-fee transactions
- Fee-sorted transaction selection for block building

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| **Java JDK** | 17 or higher | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/) |
| **Git** | Any | [Download Git](https://git-scm.com/downloads) |

### Step-by-Step Setup

```bash
# 1. Clone the repository
git clone https://github.com/zelssorathiya2615/JAVACoin.git
cd JAVACoin

# 2. Build the project (downloads dependencies automatically via Maven Wrapper)
# Windows:
.\mvnw.cmd clean package
# Linux/Mac:
./mvnw clean package

# 3. Generate the Genesis Block (run once, before first launch)
# Windows:
.\generate-genesis.bat
# Linux/Mac:
chmod +x generate-genesis.sh && ./generate-genesis.sh

# 4. Launch the 6-node network
# Windows:
.\run-nodes-web.bat
# Linux/Mac:
chmod +x run-nodes.sh && ./run-nodes.sh
```

### 🌐 Access the Web Interface

Once all nodes are running, open these URLs in your browser:

| Node | Role | URL |
|------|------|-----|
| Admin Console | Monitor | [http://localhost:8080/admin](http://localhost:8080/admin) |
| Miner #1 | Creator Miner | [http://localhost:8081/miner](http://localhost:8081/miner) |
| Miner #2 | Competing Miner | [http://localhost:8082/miner](http://localhost:8082/miner) |
| Alice | User | [http://localhost:8083/user](http://localhost:8083/user) |
| Bob | User | [http://localhost:8084/user](http://localhost:8084/user) |
| Charlie | User | [http://localhost:8085/user](http://localhost:8085/user) |

---

## 🏗️ Architecture

```
                        ┌─────────────────────────────────┐
                        │          JAVACoin Node          │
                        │                                 │
                        │  ┌──────────┐  ┌──────────────┐ │
                        │  │  Wallet  │  │  Blockchain  │ │
                        │  │ (ECDSA)  │  │  (Chain of   │ │
                        │  │          │  │   Blocks)    │ │
                        │  └──────────┘  └──────────────┘ │
                        │                                 │
                        │  ┌──────────┐  ┌──────────────┐ │
                        │  │  Miner   │  │  UTXO Set    │ │
                        │  │ (PoW     │  │  (Ledger)    │ │
                        │  │  Thread) │  │              │ │
                        │  └──────────┘  └──────────────┘ │
                        │                                 │
                        │  ┌──────────┐  ┌──────────────┐ │
                        │  │ Mempool  │  │ Web Server   │ │
                        │  │ (Pending │  │ (Jetty HTTP) │ │
                        │  │  TXs)    │  │              │ │
                        │  └──────────┘  └──────────────┘ │
                        │                                 │
                        │  ┌─────────────────────────────┐│
                        │  │     P2P Network Layer       ││
                        │  │  ServerThread + PeerManager ││
                        │  │  MessageHandler + ChainSync ││
                        │  └─────────────────────────────┘│
                        └─────────────────────────────────┘
                                      │
                    TCP Socket Communication (Ports 9080-9085)
                                      │
              ┌───────────┬───────────┼───────────┬───────────┐
              │           │           │           │           │
         Node 8080   Node 8081   Node 8082   Node 8083   Node 8084
         (Admin)     (Miner 1)   (Miner 2)   (Alice)     (Bob)
```

### Package Organization

| Package | Responsibility |
|---------|---------------|
| `com.javacoin.core` | Blockchain data structures — Block, Transaction, UTXO, UTXOSet, Mempool |
| `com.javacoin.crypto` | Cryptographic operations — SHA-256, ECDSA, Wallet |
| `com.javacoin.mining` | Proof-of-Work mining engine |
| `com.javacoin.network` | P2P communication — ServerThread, PeerManager, MessageHandler, ChainSynchronizer |
| `com.javacoin.web` | Embedded Jetty HTTP server + role-specific servlets |
| `com.javacoin.util` | Constants, Logger |

---

## 🔄 How It Works

### 1. Genesis Block Creation
```
GenerateGenesis.java
    ├── Creates ECDSA wallet (secp256k1 keypair)
    ├── Creates coinbase transaction (50 JAC reward)
    ├── Mines genesis block (PoW with difficulty 5)
    ├── Saves genesis.block (serialized)
    └── Saves genesis-wallet.dat (serialized)
```

### 2. Node Startup
```
Node.main(p2pPort, httpPort, role)
    ├── Load config (nodes.properties)
    ├── Generate unique wallet
    ├── Load genesis block from file
    ├── Initialize blockchain + UTXO set
    ├── Start ServerThread (TCP listener)
    ├── Start PeerManager
    ├── Start MessageHandler
    ├── Start ChainSynchronizer
    ├── Start EmbeddedServer (Jetty HTTP)
    ├── Start Miner (if MINER role)
    └── Sync with network
```

### 3. Transaction Flow
```
User creates transaction
    ├── Select UTXOs to spend (inputs)
    ├── Define recipients + amounts (outputs)
    ├── Calculate change output
    ├── Sign with private key (ECDSA)
    ├── Broadcast to network (P2P)
    ├── Nodes validate & add to mempool
    ├── Miner selects from mempool (by fee)
    ├── Miner builds block + mines (PoW)
    ├── Miner broadcasts block to network
    ├── All nodes validate & add block
    └── UTXO set updated (spent removed, new added)
```

### 4. Mining Process
```
Miner Thread (continuous loop)
    ├── Get latest block hash
    ├── Select highest-fee TXs from mempool
    ├── Create coinbase TX (reward + fees)
    ├── Build candidate block
    ├── Increment nonce until hash meets difficulty
    │   └── SHA256(header + nonce) starts with "00000..."
    ├── If valid: add to local chain
    ├── Broadcast to all peers
    └── If stale (competitor found first): discard
```

---

## 🌐 Network Topology

```
                     ┌──────────┐
                     │  Admin   │
                     │  :8080   │
                     │  :9080   │
                     └────┬─────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    ┌────┴─────┐    ┌─────┴────┐    ┌──────┴───┐
    │ Miner #1 │    │ Miner #2 │    │  Alice   │
    │  :8081   │────│  :8082   │────│  :8083   │
    │  :9081   │    │  :9082   │    │  :9083   │
    └────┬─────┘    └─────┬────┘    └──────┬───┘
         │                │                │
         └────────┬───────┘                │
                  │                        │
            ┌─────┴────┐            ┌──────┴───┐
            │   Bob    │────────────│ Charlie  │
            │  :8084   │            │  :8085   │
            │  :9084   │            │  :9085   │
            └──────────┘            └──────────┘

    All nodes are fully connected (mesh topology)
    P2P ports: 9080-9085  |  HTTP ports: 8080-8085
```

---

## 🎓 OOP Concepts Used

This project demonstrates **15+ Java OOP concepts**:

| Concept | Where Used |
|---------|-----------|
| **Encapsulation** | All classes use private fields + public getters |
| **Inheritance** | `Miner extends Thread`, `MessageHandler extends Thread` |
| **Interfaces** | `Block`, `Transaction`, `Wallet` implement `Serializable` |
| **Polymorphism** | `MessageType` enum in switch statements |
| **Abstraction** | `CryptoUtil` hides complex crypto behind simple methods |
| **Composition** | `Node` HAS-A `Blockchain`, `Wallet`, `Miner`, `PeerManager` |
| **Enums** | `MessageType`, `NodeRole` |
| **Generics** | `List<Transaction>`, `ConcurrentHashMap<String, UTXO>` |
| **Static Factory** | `Transaction.createCoinbase()`, `Block.createGenesisBlock()` |
| **Stream API** | Mempool fee sorting with lambdas |
| **Concurrency** | `synchronized`, `volatile`, `ConcurrentHashMap`, `BlockingQueue` |
| **Serialization** | `Serializable` + `transient` for network transfer |
| **Switch Expressions** | Java 17 arrow-case syntax |
| **Method Overloading** | Multiple constructors in `TransactionInput`, `Wallet` |
| **Override** | `toString()`, `equals()`, `hashCode()` |

---

## ⚖️ Bitcoin Comparison

| Feature | Bitcoin | JAVACoin |
|---------|---------|----------|
| Hash Algorithm | SHA-256 (double) | SHA-256 |
| Signature Curve | secp256k1 ECDSA | secp256k1 ECDSA ✅ |
| Transaction Model | UTXO | UTXO ✅ |
| Consensus | Proof-of-Work | Proof-of-Work ✅ |
| Merkle Trees | Yes | Yes ✅ |
| Fork Resolution | Longest chain wins | Longest chain wins ✅ |
| Fee Market | Yes | Yes ✅ |
| Block Time | ~10 minutes | ~3-5 seconds |
| Difficulty Adj. | Every 2016 blocks | Fixed |
| Block Reward | 50 → halves | Fixed 50 JAC |
| Supply Cap | 21M BTC | Unlimited |
| Network | Global P2P | localhost (6 nodes) |
| Script System | Bitcoin Script | None (simplified) |
| Persistence | LevelDB | In-memory |

---

## 📁 Project Structure

```
JAVACoin/
├── 📄 pom.xml                          # Maven build configuration
├── 📄 generate-genesis.bat             # Genesis block generator (Windows)
├── 📄 generate-genesis.sh              # Genesis block generator (Linux/Mac)
├── 📄 run-nodes-web.bat                # Network launcher (Windows)
├── 📄 run-nodes.sh                     # Network launcher (Linux/Mac)
├── 📄 LICENSE                          # MIT License
├── 📄 README.md                        # This file
│
├── 📂 src/main/java/com/javacoin/
│   ├── 📄 Node.java                    # Main entry point & orchestrator
│   ├── 📄 GenerateGenesis.java         # Genesis block creation utility
│   ├── 📄 CryptoTest.java             # Cryptography test suite
│   ├── 📄 BlockchainTest.java          # Blockchain integration tests
│   │
│   ├── 📂 core/                        # Blockchain data structures
│   │   ├── 📄 Block.java              # Block with PoW mining
│   │   ├── 📄 Blockchain.java         # Chain management & validation
│   │   ├── 📄 Transaction.java        # UTXO-based transactions
│   │   ├── 📄 TransactionInput.java   # References UTXOs to spend
│   │   ├── 📄 TransactionOutput.java  # Creates new UTXOs
│   │   ├── 📄 UTXO.java              # Unspent Transaction Output
│   │   ├── 📄 UTXOSet.java           # Thread-safe UTXO ledger
│   │   └── 📄 Mempool.java           # Pending transaction pool
│   │
│   ├── 📂 crypto/                      # Cryptographic layer
│   │   ├── 📄 CryptoUtil.java         # SHA-256, ECDSA, Merkle trees
│   │   └── 📄 Wallet.java            # Key generation & signing
│   │
│   ├── 📂 mining/                      # Mining engine
│   │   └── 📄 Miner.java             # PoW miner thread
│   │
│   ├── 📂 network/                     # P2P networking
│   │   ├── 📄 ServerThread.java       # TCP connection listener
│   │   ├── 📄 PeerManager.java        # Peer discovery & messaging
│   │   ├── 📄 MessageHandler.java     # Incoming message processor
│   │   ├── 📄 ChainSynchronizer.java  # Chain sync & fork resolution
│   │   └── 📄 Message.java           # Network message protocol
│   │
│   ├── 📂 web/                         # HTTP web interface
│   │   ├── 📂 server/
│   │   │   └── 📄 EmbeddedServer.java # Jetty HTTP server
│   │   └── 📂 servlets/
│   │       ├── 📄 AdminServlet.java   # Admin dashboard
│   │       ├── 📄 MinerServlet.java   # Miner dashboard
│   │       └── 📄 UserServlet.java    # User dashboard
│   │
│   └── 📂 util/                        # Utilities
│       ├── 📄 Constants.java          # Global configuration
│       └── 📄 Logger.java            # File-based logging
│
├── 📂 src/main/resources/
│   ├── 📂 config/
│   │   └── 📄 nodes.properties        # Network configuration
│   └── 📂 static/                      # CSS & JS assets
│
├── 📂 src/test/java/                   # Test sources
│
└── 📂 docs/                            # Documentation
    ├── 📄 CONCEPTS.md                  # Blockchain & OOP theory
    ├── 📄 ARCHITECTURE.md              # System design deep-dive
    └── 📄 SETUP.md                     # Detailed setup guide
```

---

## ⚙️ Configuration

The network is configured via [`src/main/resources/config/nodes.properties`](src/main/resources/config/nodes.properties):

```properties
# Mining settings
mining.difficulty=5                    # Leading zeros required in block hash
mining.block.reward=50.0               # JAC reward per mined block
mining.target.block.time.seconds=5     # Target time between blocks

# Network topology
peers=localhost:9080,localhost:9081,localhost:9082,localhost:9083,localhost:9084,localhost:9085

# Transaction settings
transaction.min.fee=0.0
transaction.default.fee=1.0
utxo.confirmation.threshold=6          # Blocks for "confirmed" status
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Project overview and quick start guide |
| [docs/CONCEPTS.md](docs/CONCEPTS.md) | In-depth blockchain, crypto, and OOP concepts |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, threading, and data flow |
| [docs/SETUP.md](docs/SETUP.md) | Detailed setup instructions for all platforms |
| [LICENSE](LICENSE) | MIT License |

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Java** | 17+ | Core language |
| **Maven** | 3.8+ | Build tool & dependency management |
| **Bouncy Castle** | 1.78 | ECDSA secp256k1 cryptography |
| **Eclipse Jetty** | 11.0.20 | Embedded HTTP server |
| **Apache Tomcat Jasper** | 10.1.24 | JSP support |
| **Gson** | 2.10.1 | JSON serialization |
| **JUnit 5** | 5.10.2 | Unit testing |

---

## 👤 Author

**Zels Sorathiya**

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**⭐ Star this repo if you found it helpful!**

*Built with ☕ Java and a love for blockchain technology*

</div>
