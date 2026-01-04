package com.northgod.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 数据库预初始化器
 * 在Spring Boot应用上下文初始化之前执行
 * 自动检查并创建数据库（如果不存在）
 */
public class DatabasePreInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePreInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            String databaseType = environment.getProperty("database.type", "postgresql");
            
            logger.info("DatabasePreInitializer 开始执行，数据库类型: {}", databaseType);
            
            if (!"postgresql".equalsIgnoreCase(databaseType)) {
                logger.debug("数据库类型为 {}，跳过自动创建数据库功能（仅支持PostgreSQL）", databaseType);
                return;
            }

            // 优先从spring.datasource配置读取（application.yml），然后从database.properties读取
            String databaseHost = environment.getProperty("spring.datasource.url");
            String databasePort = "5432";
            String databaseName = "bookstore";
            String databaseUsername = environment.getProperty("spring.datasource.username", 
                    environment.getProperty("database.username", "postgres"));
            String databasePassword = environment.getProperty("spring.datasource.password", 
                    environment.getProperty("database.password", ""));

            // 从spring.datasource.url解析数据库信息
            if (databaseHost != null && databaseHost.startsWith("jdbc:postgresql://")) {
                try {
                    // jdbc:postgresql://host:port/database
                    String[] parts = databaseHost.replace("jdbc:postgresql://", "").split("/");
                    if (parts.length == 2) {
                        String[] hostPort = parts[0].split(":");
                        databaseHost = hostPort[0];
                        if (hostPort.length > 1) {
                            databasePort = hostPort[1];
                        }
                        databaseName = parts[1].split("\\?")[0]; // 移除查询参数
                    }
                } catch (Exception e) {
                    logger.warn("无法从spring.datasource.url解析数据库信息，使用默认配置", e);
                    databaseHost = "localhost";
                }
            } else {
                // 如果没有spring.datasource.url，从database.properties读取
                databaseHost = environment.getProperty("database.host", "localhost");
                databasePort = environment.getProperty("database.port", "5432");
                databaseName = environment.getProperty("database.name", "bookstore");
            }

            // 如果环境变量中有配置，优先使用
            String dbUrl = environment.getProperty("DATABASE_URL");
            if (dbUrl != null && !dbUrl.isEmpty()) {
                // 从URL中解析数据库信息
                try {
                    // jdbc:postgresql://host:port/database
                    String[] parts = dbUrl.replace("jdbc:postgresql://", "").split("/");
                    if (parts.length == 2) {
                        String[] hostPort = parts[0].split(":");
                        databaseHost = hostPort[0];
                        if (hostPort.length > 1) {
                            databasePort = hostPort[1];
                        }
                        databaseName = parts[1].split("\\?")[0]; // 移除查询参数
                    }
                } catch (Exception e) {
                    logger.warn("无法从DATABASE_URL解析数据库信息，使用默认配置", e);
                }
            }

            String dbUsername = environment.getProperty("DATABASE_USERNAME");
            if (dbUsername != null && !dbUsername.isEmpty()) {
                databaseUsername = dbUsername;
            }

            String dbPassword = environment.getProperty("DATABASE_PASSWORD");
            if (dbPassword != null && !dbPassword.isEmpty()) {
                databasePassword = dbPassword;
            }
            
            // 如果密码仍然为空，记录警告
            if (databasePassword == null || databasePassword.isEmpty()) {
                logger.warn("数据库密码为空！请检查配置：spring.datasource.password 或 DATABASE_PASSWORD 环境变量");
            }

            try {
                int port = Integer.parseInt(databasePort);
                initializeDatabase(databaseHost, port, databaseName, databaseUsername, databasePassword);
            } catch (NumberFormatException e) {
                logger.error("数据库端口配置错误: {}", databasePort);
            }
        } catch (Throwable t) {
            // 捕获所有异常和错误，防止应用启动失败
            logger.error("数据库预初始化过程中发生错误: {}", t.getMessage(), t);
            // 不重新抛出异常，让应用继续启动
        }
    }

    private void initializeDatabase(String host, int port, String dbName, String username, String password) {
        try {
            // 连接到PostgreSQL的默认数据库（postgres）
            String defaultDbUrl = String.format("jdbc:postgresql://%s:%d/postgres", host, port);
            logger.info("正在检查数据库 '{}' 是否存在...", dbName);

            try (Connection connection = DriverManager.getConnection(defaultDbUrl, username, password)) {
                logger.info("成功连接到PostgreSQL服务器: {}:{}", host, port);

                // 检查目标数据库是否存在
                if (!databaseExists(connection, dbName)) {
                    logger.info("数据库 '{}' 不存在，正在创建...", dbName);
                    createDatabase(connection, dbName);
                    logger.info("数据库 '{}' 创建成功！", dbName);
                } else {
                    logger.info("数据库 '{}' 已存在", dbName);
                }
            }
        } catch (SQLException e) {
            logger.warn("无法连接到PostgreSQL服务器或创建数据库: {}", e.getMessage());
            logger.warn("请确保PostgreSQL服务正在运行，并且用户 '{}' 有创建数据库的权限", username);
            // 不抛出异常，让应用继续启动，可能数据库已经存在
        }
    }

    /**
     * 检查数据库是否存在
     */
    private boolean databaseExists(Connection connection, String dbName) throws SQLException {
        String sql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 创建数据库
     */
    private void createDatabase(Connection connection, String dbName) throws SQLException {
        // PostgreSQL不允许在事务中创建数据库，需要设置autocommit
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(true);
            
            // 转义数据库名称防止SQL注入
            String escapedDbName = escapeIdentifier(dbName);
            String sql = "CREATE DATABASE " + escapedDbName;
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
            
            logger.info("成功创建数据库: {}", dbName);
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * 转义PostgreSQL标识符（防止SQL注入）
     */
    private String escapeIdentifier(String identifier) {
        // PostgreSQL标识符需要用双引号包围，并转义内部的双引号
        // 同时验证标识符只包含允许的字符
        if (!identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("数据库名称包含非法字符: " + identifier);
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
