@echo off
setlocal
cd /d "%~dp0"

if exist "C:\Program Files\Java\jdk-25.0.3\bin\javac.exe" (
    set "PATH=C:\Program Files\Java\jdk-25.0.3\bin;%PATH%"
)

if not exist sources.txt (
    echo Could not find sources.txt. Please open this file from the project folder.
    pause
    exit /b 1
)

if not exist bin mkdir bin
echo =================================================================
echo   Compiling RentSphere Verification Test Suite...
echo =================================================================
javac -d bin -encoding UTF-8 @sources.txt
if errorlevel 1 (
    echo Java compilation failed. Check that JDK 21 or newer is installed.
    pause
    exit /b 1
)

echo =================================================================
echo   Running RentSphere Automated Verification Test Suite (17 Suites)
echo =================================================================
java -cp bin tests.RentSphereVerificationTest

echo.
pause
