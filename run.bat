@echo off
REM ==============================================================================
REM Build and Run Script for Personal Expense Tracker (Windows)
REM ==============================================================================

setlocal enabledelayedexpansion

echo ==========================================================
echo     Personal Expense Tracker (Core Java + Web UI)
echo ==========================================================

REM Set up classpath
set CP=bin
if exist lib\*.jar (
    for %%i in (lib\*.jar) do (
        set CP=!CP!;%%i
    )
)

if not exist bin mkdir bin

echo [1/2] Compiling Java source files...
javac -d bin -cp "!CP!" src\com\expensetracker\model\*.java src\com\expensetracker\exception\*.java src\com\expensetracker\util\*.java src\com\expensetracker\dsa\*.java src\com\expensetracker\dao\*.java src\com\expensetracker\service\*.java src\com\expensetracker\web\*.java src\com\expensetracker\test\*.java src\com\expensetracker\*.java
if %errorlevel% neq 0 (
    echo [!] Compilation failed.
    exit /b %errorlevel%
)

echo [✓] Compilation successful!

set MODE=%1
if "%MODE%"=="" set MODE=web

if "%MODE%"=="test" (
    echo [2/2] Running automated test suite...
    java -cp "!CP!" com.expensetracker.Main test
) else if "%MODE%"=="console" (
    echo [2/2] Launching interactive console application...
    java -cp "!CP!" com.expensetracker.Main console
) else (
    set PORT=%2
    if "%PORT%"=="" set PORT=8080
    echo [2/2] Launching Web REST API Server on port !PORT!...
    start http://localhost:!PORT!
    java -cp "!CP!" com.expensetracker.Main web !PORT!
)
