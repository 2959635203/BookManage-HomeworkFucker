# 设置PowerShell编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF-8
$OutputEncoding = [System.Text.Encoding]::UTF-8
[Console]::InputEncoding = [System.Text.Encoding]::UTF-8

# 设置窗口标题
$Host.UI.RawUI.WindowTitle = "BookStore System - Start All"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BookStore System - Start All Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查Java环境
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Java not found"
    }
} catch {
    Write-Host "[ERROR] Java not found, please install Java 25" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# 创建日志目录
$serverLogDir = Join-Path $PSScriptRoot "Server\logs"
$clientLogDir = Join-Path $PSScriptRoot "Client\logs"
if (-not (Test-Path $serverLogDir)) { New-Item -ItemType Directory -Path $serverLogDir -Force | Out-Null }
if (-not (Test-Path $clientLogDir)) { New-Item -ItemType Directory -Path $clientLogDir -Force | Out-Null }

Write-Host "[1/3] Starting server..." -ForegroundColor Green
$serverPath = Join-Path $PSScriptRoot "Server"
$serverScript = Join-Path $serverPath "run-server.ps1"
Start-Process powershell -ArgumentList "-NoExit", "-File", $serverScript -WindowStyle Normal

Write-Host "[2/3] Waiting for server to start (10 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "[3/3] Starting client..." -ForegroundColor Green
$clientPath = Join-Path $PSScriptRoot "Client"
$clientScript = Join-Path $clientPath "run-client.ps1"
Start-Process powershell -ArgumentList "-NoExit", "-File", $clientScript -WindowStyle Normal

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Start Complete!" -ForegroundColor Green
Write-Host "  Server Window: BookStore-Server" -ForegroundColor Yellow
Write-Host "  Client Window: BookStore-Client" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Tip: Close the window to stop the service" -ForegroundColor Gray
Read-Host "Press Enter to exit"


