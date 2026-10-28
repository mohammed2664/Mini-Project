@echo off
setlocal
cd /d "%~dp0"

if not exist sources.txt (
    echo Could not find sources.txt. Please open this file from the project folder.
    pause
    exit /b 1
)

if not exist bin mkdir bin
javac -d bin -encoding UTF-8 @sources.txt
if errorlevel 1 (
    echo Java compilation failed. Check that JDK 21 or newer is installed.
    pause
    exit /b 1
)

start "RentSphere Java Server" /min java -cp bin rentsphere.api.MainServer 8080
timeout /t 2 /nobreak >nul
start "" "http://localhost:8080/"
echo RentSphere is opening at http://localhost:8080/
echo Keep the RentSphere Java Server window open while using the app.
