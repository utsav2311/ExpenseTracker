#!/usr/bin/env bash
# ==============================================================================
# Build & Run Script for Personal Expense Tracker (Core Java)
# Supports: Web Server Mode, Interactive CLI Console, and Automated Test Suite
# ==============================================================================

set -e

# Change directory to the project root folder
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "=========================================================="
echo "    Personal Expense Tracker (Core Java + Web UI)         "
echo "=========================================================="

# Build classpath: include bin/ and any JAR files in lib/
CP="bin"
if [ -d "lib" ]; then
    for jar in lib/*.jar; do
        if [ -f "$jar" ]; then
            CP="$CP:$jar"
        fi
    done
fi

# Step 1: Ensure bin directory exists
mkdir -p bin

# Step 2: Compile all Java sources
echo "[1/2] Compiling Java source files..."
javac -d bin -cp "$CP" \
    src/com/expensetracker/model/*.java \
    src/com/expensetracker/exception/*.java \
    src/com/expensetracker/util/*.java \
    src/com/expensetracker/dsa/*.java \
    src/com/expensetracker/dao/*.java \
    src/com/expensetracker/service/*.java \
    src/com/expensetracker/web/*.java \
    src/com/expensetracker/test/*.java \
    src/com/expensetracker/*.java

echo "[✓] Compilation successful!"

# Step 3: Run application mode based on argument
MODE="${1:-web}"

if [ "$MODE" == "test" ]; then
    echo "[2/2] Running automated test suite..."
    echo ""
    java -cp "$CP" com.expensetracker.Main test
elif [ "$MODE" == "console" ] || [ "$MODE" == "cli" ]; then
    echo "[2/2] Launching interactive console application..."
    echo ""
    java -cp "$CP" com.expensetracker.Main console
else
    PORT="${2:-8080}"
    echo "[2/2] Launching Web REST API Server on port $PORT..."
    echo ""
    # Open browser on macOS after short delay if available
    (sleep 1 && open "http://localhost:$PORT" 2>/dev/null || true) &
    java -cp "$CP" com.expensetracker.Main web "$PORT"
fi
