@echo off
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║          JAVACoin Network Launcher (Web Enabled)              ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM:: Check if JAR exists
set JAR="target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar"
if not exist %JAR% (
    echo [X] JAR file not found. Building project...
    call mvnw.cmd clean package
    if %ERRORLEVEL% neq 0 (
        echo [X] Build failed. Please fix compilation errors.
        pause
        exit /b %ERRORLEVEL%
    )
)

echo ✅ JAR file found. Starting nodes...
echo.

REM Start Admin Node (Monitor)
start "Admin-8080" cmd /k "java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9080 8080 ADMIN"
echo ✅ Started Admin node on ports 9080 (P2P) / 8080 (HTTP)

REM Wait 2 seconds for genesis block creation
timeout /t 2 /nobreak > nul

REM Start First Miner (Your Miner)
start "Miner1-8081" cmd /k "java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9081 8081 MINER"
echo ✅ Started Miner #1 on ports 9081 (P2P) / 8081 (HTTP)

REM Wait 1 second
timeout /t 1 /nobreak > nul

REM Start Second Miner (Competing Miner)
start "Miner2-8082" cmd /k "java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9082 8082 MINER"
echo ✅ Started Miner #2 on ports 9082 (P2P) / 8082 (HTTP)

REM Wait 1 second
timeout /t 1 /nobreak > nul

REM Start User Nodes (Alice, Bob, Charlie)
start "Alice-8083" cmd /k "java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9083 8083 USER"
echo ✅ Started Alice on ports 9083 (P2P) / 8083 (HTTP)

timeout /t 1 /nobreak > nul

start "Bob-8084" cmd /k "java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9084 8084 USER"
echo ✅ Started Bob on ports 9084 (P2P) / 8084 (HTTP)

timeout /t 1 /nobreak > nul

start "Charlie-8085" cmd /k "java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.Node 9085 8085 USER"
echo ✅ Started Charlie on ports 9085 (P2P) / 8085 (HTTP)

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║                   All Nodes Running!                          ║
echo ╠═══════════════════════════════════════════════════════════════╣
echo ║  🌐 Web Interfaces Available:                                 ║
echo ║                                                               ║
echo ║  Admin Console:   http://localhost:8080/admin                 ║
echo ║  Miner #1:        http://localhost:8081/miner                 ║
echo ║  Miner #2:        http://localhost:8082/miner                 ║
echo ║  Alice (User):    http://localhost:8083/user                  ║
echo ║  Bob (User):      http://localhost:8084/user                  ║
echo ║  Charlie (User):  http://localhost:8085/user                  ║
echo ║                                                               ║
echo ║  📡 P2P Network: Ports 9080-9085                              ║
echo ║  🌐 HTTP Access: Ports 8080-8085                              ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.
echo 💡 TIP: Open these URLs in your browser to interact with nodes
echo.
echo ⏹️  To stop all nodes: Close all command windows
echo.
pause