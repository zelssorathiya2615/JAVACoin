#!/bin/bash
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║          JAVACoin Network Launcher (Web Enabled)              ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Check if JAR exists
JAR="target/javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR" ]; then
    echo "❌ JAR file not found. Building project..."
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "❌ Build failed. Please fix compilation errors."
        exit 1
    fi
fi

# Check if genesis block exists
if [ ! -f "genesis.block" ]; then
    echo "❌ Genesis block not found. Run ./generate-genesis.sh first."
    exit 1
fi

echo "✅ JAR file found. Starting nodes..."
echo ""

# Function to start a node in a new terminal
start_node() {
    local title=$1
    local p2p_port=$2
    local http_port=$3
    local role=$4
    
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal --title="$title" -- bash -c "java -cp $JAR com.javacoin.Node $p2p_port $http_port $role; read -p 'Press Enter to close...'" &
    elif command -v xterm &> /dev/null; then
        xterm -T "$title" -e "java -cp $JAR com.javacoin.Node $p2p_port $http_port $role; read -p 'Press Enter to close...'" &
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        osascript -e "tell app \"Terminal\" to do script \"cd $(pwd) && java -cp $JAR com.javacoin.Node $p2p_port $http_port $role\"" &
    else
        # Fallback: run in background
        java -cp "$JAR" com.javacoin.Node $p2p_port $http_port $role > "logs/node-$http_port.log" 2>&1 &
        echo "  (running in background, logs: logs/node-$http_port.log)"
    fi
}

# Create logs directory
mkdir -p logs

# Start Admin Node
start_node "Admin-8080" 9080 8080 ADMIN
echo "✅ Started Admin node on ports 9080 (P2P) / 8080 (HTTP)"
sleep 2

# Start First Miner
start_node "Miner1-8081" 9081 8081 MINER
echo "✅ Started Miner #1 on ports 9081 (P2P) / 8081 (HTTP)"
sleep 1

# Start Second Miner
start_node "Miner2-8082" 9082 8082 MINER
echo "✅ Started Miner #2 on ports 9082 (P2P) / 8082 (HTTP)"
sleep 1

# Start User Nodes
start_node "Alice-8083" 9083 8083 USER
echo "✅ Started Alice on ports 9083 (P2P) / 8083 (HTTP)"
sleep 1

start_node "Bob-8084" 9084 8084 USER
echo "✅ Started Bob on ports 9084 (P2P) / 8084 (HTTP)"
sleep 1

start_node "Charlie-8085" 9085 8085 USER
echo "✅ Started Charlie on ports 9085 (P2P) / 8085 (HTTP)"

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                   All Nodes Running!                          ║"
echo "╠═══════════════════════════════════════════════════════════════╣"
echo "║  🌐 Web Interfaces Available:                                 ║"
echo "║                                                               ║"
echo "║  Admin Console:   http://localhost:8080/admin                 ║"
echo "║  Miner #1:        http://localhost:8081/miner                 ║"
echo "║  Miner #2:        http://localhost:8082/miner                 ║"
echo "║  Alice (User):    http://localhost:8083/user                  ║"
echo "║  Bob (User):      http://localhost:8084/user                  ║"
echo "║  Charlie (User):  http://localhost:8085/user                  ║"
echo "║                                                               ║"
echo "║  📡 P2P Network: Ports 9080-9085                              ║"
echo "║  🌐 HTTP Access: Ports 8080-8085                              ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "💡 TIP: Open these URLs in your browser to interact with nodes"
echo ""
echo "⏹️  To stop all nodes: kill all java processes with 'pkill -f javacoin'"
