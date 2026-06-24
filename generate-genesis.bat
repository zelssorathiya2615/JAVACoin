@echo off
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║               JAVACoin Genesis Block Generator                ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Check if JAR exists
if not exist "target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo ❌ JAR file not found. Building project...
    call mvnw.cmd clean package
    if errorlevel 1 (
        echo ❌ Build failed. Please fix compilation errors.
        pause
        exit /b 1
    )
)

echo ✅ JAR found. Running genesis generator...
echo.

java -cp target\javacoin-1.0-SNAPSHOT-jar-with-dependencies.jar com.javacoin.GenerateGenesis

echo.
echo ═══════════════════════════════════════════════════════════════
echo Genesis generation complete!
echo ═══════════════════════════════════════════════════════════════
echo.
echo Next step: Run run-nodes-web.bat to start the network
echo.
pause