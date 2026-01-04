# 图书管理系统 - 启动说明

## 一键启动（推荐）

### Windows 批处理（.bat）
双击运行 `start-all.bat`，将自动启动服务器和客户端。

### PowerShell 脚本（.ps1）
在 PowerShell 中运行：
```powershell
.\start-all.ps1
```

## 单独启动

### 启动服务器

**方式1：使用批处理文件**
```bash
cd Server
run-server.bat
```

**方式2：使用 PowerShell 脚本**
```powershell
cd Server
.\run-server.ps1
```

**方式3：使用 Gradle**
```bash
cd Server
.\gradlew.bat bootRun
```

### 启动客户端

**方式1：使用批处理文件**
```bash
cd Client
run-client.bat
```

**方式2：使用 PowerShell 脚本**
```powershell
cd Client
.\run-client.ps1
```

**方式3：使用 Gradle**
```bash
cd Client
.\gradlew.bat run
```

## 日志文件位置

### 服务器日志
- 位置：`Server/logs/bookstore-server.log`
- 包含：服务器运行日志、API请求日志、错误日志等
- 配置：`Server/src/main/resources/application.yml`

### 客户端日志
- 位置：`Client/logs/bookstore-client.log`（普通日志）
- 位置：`Client/logs/bookstore-client-error.log`（错误日志）
- 包含：客户端运行日志、网络请求日志、错误日志等

## 注意事项

1. **启动顺序**：建议先启动服务器，等待10秒后再启动客户端
2. **端口占用**：确保8080端口未被占用
3. **数据库**：确保PostgreSQL数据库已启动并可连接
4. **Java版本**：需要Java 25或更高版本

## 默认账户

- 用户名：`admin`
- 密码：`admin123`

## 故障排查

1. **服务器启动失败**
   - 检查8080端口是否被占用
   - 检查数据库连接配置
   - 查看 `Server/logs/bookstore-server.log`

2. **客户端连接失败**
   - 确认服务器已启动
   - 检查 `Client/src/main/resources/application.properties` 中的服务器地址
   - 查看 `Client/logs/bookstore-client.log`

3. **中文乱码**
   - 使用提供的启动脚本（已配置UTF-8编码）
   - 确保终端支持UTF-8编码



















