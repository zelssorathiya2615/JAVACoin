# 🛠️ JAVACoin — Setup Guide

Detailed setup instructions for running JAVACoin on any operating system.

---

## Prerequisites

### Java JDK 17+

**Check if installed:**
```bash
java -version
```

**Install:**
- **Windows:** Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
- **macOS:** `brew install openjdk@17`
- **Linux (Ubuntu):** `sudo apt install openjdk-17-jdk`
- **Linux (Fedora):** `sudo dnf install java-17-openjdk-devel`

### Git

**Check if installed:**
```bash
git --version
```

**Install:** [git-scm.com/downloads](https://git-scm.com/downloads)

---

## Installation

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/JAVACoin.git
cd JAVACoin
```

### 2. Build the Project
We use the **Maven Wrapper** (`mvnw`), so you don't even need to install Maven manually! It will download everything automatically.

**Windows:**
```cmd
mvnw.cmd clean package
```

**Linux/Mac:**
```bash
./mvnw clean package
```

This will:
- Download all dependencies (Bouncy Castle, Jetty, Gson, JUnit)
- Compile all Java source files
- Run tests
- Create `target/javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar` (~15 MB)

### 3. Generate the Genesis Block

This creates the shared genesis block that all nodes will use. **Run this only once.**

**Windows:**
```cmd
generate-genesis.bat
```

**Linux/Mac:**
```bash
chmod +x generate-genesis.sh
./generate-genesis.sh
```

This creates:
- `genesis.block` — The serialized genesis block
- `genesis-wallet.dat` — The wallet that owns the initial 50 JAC

### 4. Launch the Network

**Windows:**
```cmd
run-nodes-web.bat
```

**Linux/Mac:**
```bash
chmod +x run-nodes.sh
./run-nodes.sh
```

This opens 6 terminal windows, each running a JAVACoin node.

### 5. Access Web Interfaces

Open your browser and navigate to:

| Node | URL |
|------|-----|
| Admin | http://localhost:8080/admin |
| Miner #1 | http://localhost:8081/miner |
| Miner #2 | http://localhost:8082/miner |
| Alice | http://localhost:8083/user |
| Bob | http://localhost:8084/user |
| Charlie | http://localhost:8085/user |

---

## Running Individual Nodes

You can start nodes individually for debugging:

```bash
java -cp target/javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node <p2pPort> <httpPort> <ROLE>
```

Examples:
```bash
# Start Admin node
java -cp target/javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9080 8080 ADMIN

# Start Miner node
java -cp target/javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9081 8081 MINER

# Start User node
java -cp target/javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9083 8083 USER
```

---

## Running Tests

### Cryptography Tests
```bash
mvn exec:java -Dexec.mainClass="com.javacoin.CryptoTest"
```

### Blockchain Integration Tests
```bash
mvn exec:java -Dexec.mainClass="com.javacoin.BlockchainTest"
```

### JUnit Tests
```bash
mvn test
```

---

## Troubleshooting

### "Port already in use"
Another process is using the port. Kill it:
```bash
# Windows
netstat -aon | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

### "Genesis block file not found"
Run `generate-genesis.bat` (or `.sh`) before starting nodes.

### Build fails
Make sure Java 17+ and Maven 3.8+ are installed and on your PATH.

### Nodes can't connect to each other
Ensure all 6 nodes are running. The P2P network uses ports 9080-9085 — check no firewall is blocking them.
