package com.northgod.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库初始化器
 * 在应用启动时自动检查并创建数据库（如果不存在）
 * 注意：此初始化器在数据源初始化之后执行，如果数据库不存在导致连接失败，
 * 应用会先失败，然后需要手动创建数据库。更好的方法是在应用启动前手动创建数据库。
 * 
 * 此类的目的是在应用启动后检查数据库状态，如果数据库已存在但表不存在，
 * JPA的ddl-auto: update会自动创建表。
 */
@Component
@Order(1) // 确保在其他组件之前执行
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${database.type:postgresql}")
    private String databaseType;

    @Value("${database.host:localhost}")
    private String databaseHost;

    @Value("${database.port:5432}")
    private int databasePort;

    @Value("${database.name:bookstore}")
    private String databaseName;

    @Value("${database.username:postgres}")
    private String databaseUsername;

    @Value("${database.password:}")
    private String databasePassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!"postgresql".equalsIgnoreCase(databaseType)) {
            logger.info("数据库类型为 {}，跳过自动创建数据库功能（仅支持PostgreSQL）", databaseType);
            return;
        }

        try {
            // 连接到PostgreSQL的默认数据库（postgres）
            String defaultDbUrl = String.format("jdbc:postgresql://%s:%d/postgres", databaseHost, databasePort);
            logger.info("正在连接到PostgreSQL服务器: {}:{}", databaseHost, databasePort);

            try (Connection connection = DriverManager.getConnection(defaultDbUrl, databaseUsername, databasePassword)) {
                logger.info("成功连接到PostgreSQL服务器");

                // 检查目标数据库是否存在
                if (!databaseExists(connection, databaseName)) {
                    logger.info("数据库 '{}' 不存在，正在创建...", databaseName);
                    createDatabase(connection, databaseName);
                    logger.info("数据库 '{}' 创建成功", databaseName);
                } else {
                    logger.info("数据库 '{}' 已存在", databaseName);
                }
            }
        } catch (SQLException e) {
            logger.error("数据库初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，让应用继续启动，可能数据库已经存在或配置有误
            logger.warn("将继续尝试启动应用，如果数据库不存在，请手动创建数据库 '{}'", databaseName);
        } catch (Exception e) {
            logger.error("数据库初始化过程中发生未知错误: {}", e.getMessage(), e);
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
            
            // 使用参数化查询防止SQL注入
            String sql = String.format("CREATE DATABASE %s", escapeIdentifier(dbName));
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
     * 注意：这里简化处理，实际应该使用更严格的验证
     */
    private String escapeIdentifier(String identifier) {
        // PostgreSQL标识符需要用双引号包围，并转义内部的双引号
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}

