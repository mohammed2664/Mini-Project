#!/usr/bin/env bash
# RentSphere Automated Test Runner (Unix/Mac/Linux)
set -e

cd "$(dirname "$0")"

echo "================================================================="
echo "  RentSphere Automated Verification Test Runner (163 Tests)"
echo "================================================================="

if ! command -v javac &> /dev/null; then
    echo "[ERROR] 'javac' compiler not found on PATH. Please install JDK 17+."
    exit 1
fi

mkdir -p bin
echo "[1/2] Compiling source files and test suite..."
javac -d bin -encoding UTF-8 @sources.txt
echo "[OK] Compilation successful."

echo "[2/2] Running 17 Test Suites (163 Tests)..."
java -cp bin tests.RentSphereVerificationTest
