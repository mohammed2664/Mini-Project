@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo =================================================================
echo   RentSphere Automated Verification Test Runner (163 Tests)
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
echo [1/2] Compiling Java source files and test suite...
javac -d bin -encoding UTF-8 @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo [2/2] Executing RentSphere Verification Test Suite (17 Suites, 163 Tests)...
echo.
java -cp bin tests.RentSphereVerificationTest

echo.
pause
