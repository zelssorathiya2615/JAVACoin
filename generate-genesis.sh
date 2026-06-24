#!/bin/bash
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║               JAVACoin Genesis Block Generator                ║"
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

echo "✅ JAR found. Running genesis generator..."
echo ""

java -cp "$JAR" com.javacoin.GenerateGenesis

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "Genesis generation complete!"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Next step: Run ./run-nodes.sh to start the network"
