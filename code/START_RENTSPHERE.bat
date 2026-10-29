@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo =================================================================
echo   RentSphere P2P Equipment Rental Platform - System Launcher
echo =================================================================

:: 1. Check if javac is already on PATH
where javac >nul 2>&1
if %errorlevel% equ 0 goto :FOUND_JAVA

:: 2. Check JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "PATH=%JAVA_HOME%\bin;!PATH!"
        goto :FOUND_JAVA
    )
)

:: 3. Scan C:\Program Files\Java for any installed JDK
for /d %%D in ("C:\Program Files\Java\jdk*") do (
    if exist "%%D\bin\javac.exe" (
        set "PATH=%%D\bin;!PATH!"
        goto :FOUND_JAVA
    )
)

:: 4. Scan C:\Program Files (x86)\Java for any installed JDK
for /d %%D in ("C:\Program Files (x86)\Java\jdk*") do (
    if exist "%%D\bin\javac.exe" (
        set "PATH=%%D\bin;!PATH!"
        goto :FOUND_JAVA
    )
)

echo [ERROR] Java Development Kit (JDK 17+) not found on your system.
echo Please install JDK 17, 21, or 25 from https://adoptium.net/ or Oracle.
echo.
pause
exit /b 1

:FOUND_JAVA
echo [OK] Using Java Compiler:
javac -version

if not exist sources.txt (
    echo [ERROR] Could not find sources.txt in the current folder.
    pause
    exit /b 1
)

if not exist bin mkdir bin
echo [1/2] Compiling RentSphere Java source files...
javac -d bin -encoding UTF-8 @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)
echo [OK] Compilation successful (Exit Code 0).

echo [2/2] Starting RentSphere Server on http://localhost:8080/ ...
start "RentSphere Java Server (Port 8080)" /min java -cp bin rentsphere.api.MainServer 8080
timeout /t 2 /nobreak >nul
start "" "http://localhost:8080/"
echo.
echo =================================================================
echo   RentSphere is running! Browser opened to: http://localhost:8080/
echo   Leave the minimized server window open while using the app.
echo   To stop the server, close the minimized command window.
echo =================================================================
echo.
pause
