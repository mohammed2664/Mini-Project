#!/usr/bin/env bash
# RentSphere Unix/Mac/Linux Launcher
set -e

cd "$(dirname "$0")"

echo "================================================================="
echo "  RentSphere P2P Equipment Rental Platform - System Launcher"
echo "================================================================="

if ! command -v javac &> /dev/null; then
    echo "[ERROR] 'javac' compiler not found on PATH. Please install JDK 17+."
    exit 1
fi

mkdir -p bin
echo "[1/2] Compiling RentSphere Java source files..."
javac -d bin -encoding UTF-8 @sources.txt
echo "[OK] Compilation successful."

echo "[2/2] Starting RentSphere Server on http://localhost:8080/ ..."
java -cp bin rentsphere.api.MainServer 8080 &
SERVER_PID=$!

sleep 2

# Open browser depending on OS
if command -v xdg-open &> /dev/null; then
    xdg-open "http://localhost:8080/"
elif command -v open &> /dev/null; then
    open "http://localhost:8080/"
fi

echo "RentSphere is running on http://localhost:8080/ (PID: $SERVER_PID)"
echo "Press Ctrl+C to stop the server."
wait $SERVER_PID
