# 📖 JAVACoin — Blockchain & OOP Concepts Guide

A comprehensive guide to every concept used in the JAVACoin project, from basic OOP to advanced blockchain theory.

---

## Table of Contents

1. [Object-Oriented Programming Concepts](#1-object-oriented-programming-concepts)
2. [Blockchain Fundamentals](#2-blockchain-fundamentals)
3. [Cryptography](#3-cryptography)
4. [Transaction Model (UTXO)](#4-transaction-model-utxo)
5. [Proof-of-Work Mining](#5-proof-of-work-mining)
6. [P2P Networking](#6-p2p-networking)
7. [Consensus & Fork Resolution](#7-consensus--fork-resolution)
8. [Concurrency & Thread Safety](#8-concurrency--thread-safety)

---

## 1. Object-Oriented Programming Concepts

### 1.1 Encapsulation
**What:** Hiding internal state and requiring access through methods.

**In JAVACoin:** Every class uses `private` fields with public getters. For example, `UTXO.java` stores `private double value` — you can read it via `getValue()` but never modify it after creation. This prevents external code from corrupting blockchain state.

### 1.2 Inheritance
**What:** A class inherits behavior from a parent class (IS-A relationship).

**In JAVACoin:**
- `Miner extends Thread` — The miner IS-A thread that runs in the background
- `MessageHandler extends Thread` — The message handler IS-A thread
- `ServerThread extends Thread` — The TCP server IS-A thread

### 1.3 Interfaces
**What:** A contract that classes must implement.

**In JAVACoin:** `Block`, `Transaction`, `UTXO`, `Wallet`, and `Message` all implement `java.io.Serializable`. This interface guarantees they can be converted to bytes for network transfer and file storage.

### 1.4 Composition
**What:** A class contains instances of other classes (HAS-A relationship).

**In JAVACoin:** The `Node` class HAS-A `Blockchain`, `UTXOSet`, `Mempool`, `Wallet`, `Miner`, `PeerManager`, `MessageHandler`, and `EmbeddedServer`. The node doesn't inherit from any of these — it *contains* them.

### 1.5 Enums
**What:** A fixed set of named constants.

**In JAVACoin:**
- `Message.MessageType` — `NEW_TRANSACTION`, `NEW_BLOCK`, `REQUEST_CHAIN`, `PING`, `PONG`, etc.
- `Node.NodeRole` — `ADMIN`, `MINER`, `USER`

### 1.6 Static Factory Methods
**What:** Static methods that create objects (alternative to constructors).

**In JAVACoin:**
- `Transaction.createCoinbase()` — Creates a special mining reward transaction
- `Block.createGenesisBlock()` — Creates the first block in the chain
- `Message.newBlock()`, `Message.newTransaction()` — Create network messages

### 1.7 Generics
**What:** Type parameters that provide compile-time type safety.

**In JAVACoin:**
- `List<Transaction>` — A list that can only hold Transaction objects
- `ConcurrentHashMap<String, UTXO>` — Thread-safe map from ID → UTXO
- `BlockingQueue<Message>` — Thread-safe producer-consumer queue

### 1.8 Stream API & Lambdas
**What:** Functional-style operations on collections.

**In JAVACoin:** Mempool sorts transactions by fee:
```java
transactions.stream()
    .sorted(Comparator.comparingDouble(Transaction::getFee).reversed())
    .collect(Collectors.toList());
```

### 1.9 Serialization & `transient`
**What:** Converting objects to bytes. `transient` fields are excluded.

**In JAVACoin:** Blocks are serialized for network transmission. Mining metadata like `miningStartTime` and `hashAttempts` are marked `transient` — they don't need to travel with the block.

### 1.10 Method Overloading
**What:** Multiple methods with the same name but different parameters.

**In JAVACoin:** `TransactionInput` has two constructors: one takes a `String` UTXO ID, the other takes a `UTXO` object.

---

## 2. Blockchain Fundamentals

### 2.1 What Is a Blockchain?
A blockchain is a **linked list of blocks** where each block contains a hash of the previous block. This creates a tamper-evident chain — changing any block invalidates all subsequent blocks.

### 2.2 The Genesis Block
The first block in the chain. It has `previousHash = "0"` (no predecessor). In JAVACoin, it's generated once by `GenerateGenesis.java` and saved to a file so all nodes start with identical state.

### 2.3 Block Structure
Each block has a **header** (index, hash, previousHash, merkleRoot, timestamp, nonce, difficulty) and a **body** (list of transactions). The hash is computed from the header data.

### 2.4 Hash Chain
Block N's `previousHash` = Block (N-1)'s `hash`. If you change Block 3, its hash changes, which makes Block 4's previousHash invalid, breaking the entire chain from that point forward.

---

## 3. Cryptography

### 3.1 SHA-256
A one-way hash function that takes any input and produces a fixed 256-bit (64 hex character) output. Properties: deterministic, fast, avalanche effect (small input change → huge output change), collision-resistant.

### 3.2 ECDSA (Elliptic Curve Digital Signature Algorithm)
Used for digital signatures. A private key (secret number) generates a public key (point on the secp256k1 elliptic curve). Only the private key holder can sign messages, but anyone with the public key can verify the signature.

### 3.3 secp256k1
The specific elliptic curve used by both Bitcoin and JAVACoin. It's defined by the equation `y² = x³ + 7` over a specific prime field.

### 3.4 Merkle Trees
A binary hash tree where each leaf is a transaction hash. Pairs of hashes are combined and hashed again, building up to a single "Merkle Root" that represents all transactions. Used in the block header.

---

## 4. Transaction Model (UTXO)

### 4.1 What Is UTXO?
UTXO = **Unspent Transaction Output**. There are no "accounts" — your balance is the sum of all UTXOs you own. Think of UTXOs as physical coins/bills.

### 4.2 How Transactions Work
- **Inputs:** Reference existing UTXOs to spend (they get destroyed)
- **Outputs:** Create new UTXOs for recipients
- **Change:** Unspent input value returned as a new UTXO to the sender
- **Fee:** `Total Inputs - Total Outputs` (implicit, goes to the miner)

### 4.3 Coinbase Transaction
A special transaction in every block with NO inputs. It creates new coins (the block reward) and is the miner's payment.

---

## 5. Proof-of-Work Mining

### 5.1 The Puzzle
Find a `nonce` value such that `SHA256(blockHeader + nonce)` starts with N zeros (where N = difficulty). This requires brute-force computation.

### 5.2 Difficulty
JAVACoin uses difficulty 5, meaning the hash must start with `"00000"`. This requires ~1 million hash attempts on average.

### 5.3 Why It Matters
PoW makes it computationally expensive to create blocks, preventing spam and Sybil attacks. It's easy to verify (just hash once and check) but hard to produce.

---

## 6. P2P Networking

### 6.1 Architecture
Each node has a TCP server (ServerThread) listening for connections and a PeerManager that sends messages to known peers. Messages are serialized Java objects.

### 6.2 Message Types
`NEW_TRANSACTION`, `NEW_BLOCK`, `REQUEST_CHAIN`, `RESPONSE_CHAIN`, `PING`, `PONG`, `DIFFICULTY_UPDATE`

### 6.3 Message Flow
Sender → TCP Socket → ServerThread → BlockingQueue → MessageHandler → Process

---

## 7. Consensus & Fork Resolution

### 7.1 The Problem
Two miners can find valid blocks at the same time, creating a temporary fork (two valid chains).

### 7.2 The Solution
**Longest valid chain wins** (Nakamoto Consensus). When a node receives a longer valid chain from a peer, it replaces its own chain and rebuilds the UTXO set from scratch.

---

## 8. Concurrency & Thread Safety

### 8.1 `synchronized`
The `addBlockToChain()` method is synchronized — only one thread can add a block at a time.

### 8.2 `volatile`
The miner's `mining` and `running` flags are volatile, ensuring changes made by one thread are immediately visible to others.

### 8.3 `ConcurrentHashMap`
The UTXO set uses a ConcurrentHashMap for thread-safe access from multiple threads (miner, message handler, web servlets).

### 8.4 `BlockingQueue`
The message queue uses a LinkedBlockingQueue — the MessageHandler blocks on `take()` until a message arrives, avoiding busy-waiting.
