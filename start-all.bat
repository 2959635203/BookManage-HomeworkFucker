@echo off
chcp 65001 >nul 2>&1
title BookStore System - Start All

echo ========================================
echo   BookStore System - Start All Script
echo ========================================
echo.

REM Check Java environment
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found, please install Java 25
    pause
    exit /b 1
)

REM Create log directories
if not exist "Server\logs" mkdir Server\logs
if not exist "Client\logs" mkdir Client\logs

echo [1/3] Starting server...
start "BookStore-Server" cmd /k "chcp 65001 >nul && cd /d %~dp0Server && gradlew.bat bootRun"

REM Wait for server to start
echo [2/3] Waiting for server to start (10 seconds)...
timeout /t 10 /nobreak >nul

echo [3/3] Starting client...
start "BookStore-Client" cmd /k "chcp 65001 >nul && cd /d %~dp0Client && run-client.bat"

echo.
echo ========================================
echo   Start Complete!
echo   Server Window: BookStore-Server
echo   Client Window: BookStore-Client
echo ========================================
echo.
echo Tip: Close the window to stop the service
pause


