# 📚 图书管理系统 (BookManage)

一个基于 **Java 25** 和 **Spring Boot 4.0** 开发的现代化图书管理系统，采用前后端分离架构，提供完整的图书管理、供应商管理、交易管理等功能。

## ✨ 项目特点

- 🚀 **最新技术栈**：Java 25 + Spring Boot 4.0.1 + PostgreSQL
- 🧵 **虚拟线程支持**：充分利用 Java 25 虚拟线程特性，提升并发性能
- 🏗️ **清晰架构**：标准三层架构，前后端完全分离
- 🔒 **安全可靠**：完善的异常处理、数据验证、并发控制
- 📊 **功能完整**：书籍管理、供应商管理、交易管理、库存管理、报表统计
- 📝 **文档完善**：详细的技术文档、配置说明、SQL文档
- 🎯 **开箱即用**：提供一键启动脚本，支持多种启动方式

## 🛠️ 技术栈

### 后端 (Server)
- **框架**：Spring Boot 4.0.1
- **Java版本**：Java 25
- **数据库**：PostgreSQL
- **ORM**：Spring Data JPA
- **缓存**：Caffeine
- **监控**：Spring Boot Actuator

### 前端 (Client)
- **GUI框架**：Java Swing
- **HTTP客户端**：Spring RestClient (Spring Boot 4.0)
- **线程模型**：虚拟线程 (Java 25)

## 📋 功能模块

### 核心功能
- ✅ **书籍管理**：增删改查、搜索、批量操作
- ✅ **供应商管理**：完整的供应商CRUD功能
- ✅ **交易管理**：进货、销售、退货等功能
- ✅ **库存管理**：库存预警、库存统计
- ✅ **报表统计**：销售报表、交易汇总、库存价值计算
- ✅ **回收站功能**：软删除机制，支持恢复

### 系统功能
- ✅ **用户认证**：登录认证机制
- ✅ **健康检查**：完善的健康检查端点
- ✅ **日志系统**：完整的日志记录（普通日志和错误日志分离）
- ✅ **缓存机制**：使用Caffeine缓存提升性能

## 🚀 快速开始

### 环境要求

- **Java**：JDK 25 或更高版本
- **数据库**：PostgreSQL 12 或更高版本
- **操作系统**：Windows / Linux / macOS

### 数据库配置

1. 创建 PostgreSQL 数据库：
```sql
CREATE DATABASE bookstore;
```

2. 配置数据库连接（推荐使用环境变量）：
```bash
# Windows PowerShell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/bookstore"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your_password"

# Linux/Mac
export DATABASE_URL="jdbc:postgresql://localhost:5432/bookstore"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="your_password"
```

或者编辑 `Server/src/main/resources/database.properties` 文件。

### 启动方式

#### 方式一：一键启动（推荐）

**Windows 批处理：**
```bash
start-all.bat
```

**PowerShell：**
```powershell
.\start-all.ps1
```

#### 方式二：单独启动

**启动服务器：**
```bash
cd Server
.\gradlew.bat bootRun
# 或使用批处理文件
run-server.bat
```

**启动客户端：**
```bash
cd Client
.\gradlew.bat run
# 或使用批处理文件
run-client.bat
```

> ⚠️ **注意**：建议先启动服务器，等待10秒后再启动客户端。

### 默认账户

- **用户名**：`admin`
- **密码**：`admin123`

## 📁 项目结构

```
BookManage/
├── Client/                 # 客户端模块（Swing GUI）
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Java源代码
│   │       └── resources/  # 配置文件
│   └── build.gradle        # Gradle构建配置
│
├── Server/                 # 服务端模块（Spring Boot）
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Java源代码
│   │       │   └── com/northgod/server/
│   │       │       ├── config/      # 配置类
│   │       │       ├── controller/  # REST控制器
│   │       │       ├── service/     # 业务逻辑层
│   │       │       ├── repository/  # 数据访问层
│   │       │       ├── entity/      # 实体类
│   │       │       └── exception/   # 异常处理
│   │       └── resources/  # 配置文件
│   └── build.gradle        # Gradle构建配置
│
├── Build/                  # 构建脚本和打包配置
│   ├── build-all.bat      # 一键构建脚本
│   └── dist/              # 构建输出目录
│
├── README.md              # 项目说明（本文件）
├── README-启动说明.md     # 详细启动说明
├── SQL语句整理.md         # SQL语句文档
├── 项目优点总结.md        # 项目技术亮点
└── .gitignore             # Git忽略配置
```

## ⚙️ 配置说明

### 数据库配置

配置文件位置：`Server/src/main/resources/database.properties`

```properties
database.type=postgresql
database.host=localhost
database.port=5432
database.name=bookstore
database.username=postgres
database.password=your_password
```

**配置优先级**：
1. 环境变量（最高优先级）
2. `database.properties` 文件
3. `application.yml` 默认值

详细配置说明请参考：[Server/配置说明.md](Server/配置说明.md)

### 服务器配置

- **端口**：8080
- **上下文路径**：/api
- **API地址**：http://localhost:8080/api

### 客户端配置

配置文件位置：`Client/src/main/resources/application.properties`

```properties
api.base.url=http://localhost:8080/api
```

## 📦 构建和打包

### 构建可执行文件

```bash
cd Build
build-all.bat
```

构建完成后，可执行文件位于：
- **客户端**：`Build/dist/BookStore-Client/BookStore-Client.exe`
- **服务端**：`Build/dist/BookStore-Server/BookStore-Server.exe`

详细构建说明请参考：[Build/README.md](Build/README.md)

## 📚 文档

- [启动说明](README-启动说明.md) - 详细的启动指南和故障排查
- [配置说明](Server/配置说明.md) - 完整的配置文档
- [SQL语句整理](SQL语句整理.md) - 所有SQL语句的详细说明
- [项目优点总结](项目优点总结.md) - 项目技术亮点分析
- [构建说明](Build/README.md) - 构建和打包指南
- [配置指南](Build/CONFIG-GUIDE.md) - 打包后应用的配置指南

## 🔧 开发

### 构建项目

```bash
# 构建服务端
cd Server
./gradlew build

# 构建客户端
cd Client
./gradlew build
```

### 运行测试

```bash
# 服务端测试
cd Server
./gradlew test

# 客户端测试
cd Client
./gradlew test
```

## 🐛 故障排查

### 服务器启动失败

1. 检查 8080 端口是否被占用
2. 检查数据库连接配置
3. 查看日志：`Server/logs/bookstore-server.log`

### 客户端连接失败

1. 确认服务器已启动
2. 检查 `Client/src/main/resources/application.properties` 中的服务器地址
3. 查看日志：`Client/logs/bookstore-client.log`

### 中文乱码

- 使用提供的启动脚本（已配置UTF-8编码）
- 确保终端支持UTF-8编码

## 🔒 安全建议

1. **生产环境**：
   - 使用环境变量存储敏感信息（数据库密码、管理员密码）
   - 不要将包含密码的配置文件提交到版本控制系统
   - 定期更换密码

2. **开发环境**：
   - 可以使用配置文件，但不要使用生产环境的密码
   - 建议将包含敏感信息的配置文件添加到 `.gitignore`

## 🌟 技术亮点

### Java 25 新特性应用
- ✅ **虚拟线程（Virtual Threads）**：大幅提升I/O密集型并发性能
- ✅ **现代Java语法**：Switch表达式、文本块等
- ✅ **JVM优化**：紧凑对象头减少内存占用

### Spring Boot 4.0 新特性应用
- ✅ **RestClient**：使用Spring Boot 4.0新增的HTTP客户端
- ✅ **JdkClientHttpRequestFactory**：使用Java 21+的原生HTTP客户端

### 架构设计
- ✅ **标准三层架构**：Controller → Service → Repository
- ✅ **异常处理完善**：全局异常处理器，统一的错误响应
- ✅ **数据验证严格**：参数验证和业务规则验证
- ✅ **并发安全**：悲观锁、事务管理、库存安全更新

### 性能优化
- ✅ **数据库优化**：参数化查询、原生SQL优化、批量操作
- ✅ **缓存策略**：Caffeine缓存提升性能
- ✅ **线程池管理**：虚拟线程和传统线程池的合理使用

## 📄 许可证

本项目仅供学习和研究使用。

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

## 📞 联系方式

如有问题或建议，请提交 Issue。

---

**注意**：本项目使用 Java 25 和 Spring Boot 4.0，请确保开发环境满足要求。

