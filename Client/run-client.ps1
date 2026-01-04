# 设置PowerShell编码为UTF-8以支持中文显示
[Console]::OutputEncoding = [System.Text.Encoding]::UTF-8
$OutputEncoding = [System.Text.Encoding]::UTF-8
[Console]::InputEncoding = [System.Text.Encoding]::UTF-8
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
[System.Console]::OutputEncoding = [System.Text.Encoding]::UTF-8
chcp 65001 | Out-Null

# 设置窗口标题
$Host.UI.RawUI.WindowTitle = "BookStore-Client"

# 设置环境变量（确保UTF-8编码）
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.region=CN -Duser.country=CN"
$env:JAVA_OPTS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
$env:GRADLE_OPTS = "-Dfile.encoding=UTF-8"

# 切换到脚本所在目录
Set-Location $PSScriptRoot

# 创建日志目录
if (-not (Test-Path "logs")) { 
    New-Item -ItemType Directory -Path "logs" -Force | Out-Null 
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BookStore System - Client" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "正在启动客户端..." -ForegroundColor Green
Write-Host "日志文件: logs\bookstore-client.log" -ForegroundColor Yellow
Write-Host ""

& .\gradlew.bat --console=plain run

# 如果出错，暂停以便查看错误信息
if ($LASTEXITCODE -ne 0) {
    Write-Host "`n程序执行出错，退出代码: $LASTEXITCODE" -ForegroundColor Red
    Read-Host "按Enter键退出"
    exit $LASTEXITCODE
}

