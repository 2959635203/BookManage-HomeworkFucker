@echo off
REM 设置代码页为UTF-8以支持中文显示
chcp 65001 >nul 2>&1
title BookStore-Client

REM 设置编码环境变量
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.region=CN -Duser.country=CN
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8
set GRADLE_OPTS=-Dfile.encoding=UTF-8

REM 切换到脚本所在目录
cd /d %~dp0

REM 创建日志目录
if not exist "logs" mkdir logs

echo ========================================
echo   BookStore System - Client
echo ========================================
echo.
echo 正在启动客户端...
echo 日志文件: logs\bookstore-client.log
echo.

REM 使用gradlew运行，并确保编码正确
call gradlew.bat --console=plain run

if %errorlevel% neq 0 (
    echo.
    echo 错误: 客户端启动失败，退出代码: %errorlevel%
    pause
    exit /b %errorlevel%
)

pause

