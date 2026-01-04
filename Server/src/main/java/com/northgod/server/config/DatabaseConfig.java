package com.northgod.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;

/**
 * 数据库配置类
 * 从独立的配置文件中读取数据库连接信息
 * 注意：实际的数据源配置在 application.yml 中，这里主要用于读取和验证配置
 */
@Configuration
@PropertySource("classpath:database.properties")
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${database.type:postgresql}")
    private String type;

    @Value("${database.host:localhost}")
    private String host;

    @Value("${database.port:5432}")
    private int port;

    @Value("${database.name:bookstore}")
    private String name;

    @Value("${database.username:postgres}")
    private String username;

    @Value("${database.password:}")
    private String password;

    /**
     * 应用启动后打印数据库配置信息（隐藏密码）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseConfig() {
        String jdbcUrl = getJdbcUrl();
        logger.info("数据库配置 - 类型: {}, 地址: {}:{}, 数据库: {}, 用户: {}", 
                type, host, port, name, username);
        logger.debug("数据库连接URL: {}", jdbcUrl);
    }

    /**
     * 构建数据库连接URL
     */
    public String getJdbcUrl() {
        String jdbcUrl;
        switch (type.toLowerCase()) {
            case "postgresql":
                jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, name);
                break;
            case "mysql":
                jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, name);
                break;
            case "h2":
                jdbcUrl = String.format("jdbc:h2:mem:%s", name);
                break;
            default:
                jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, name);
        }
        return jdbcUrl;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

