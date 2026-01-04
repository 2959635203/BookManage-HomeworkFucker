package com.northgod.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 配置加载器
 * 在应用启动时验证和打印配置信息
 */
@Component
public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private final Environment environment;
    private final DatabaseConfig databaseConfig;
    private final AdminConfig adminConfig;

    public ConfigLoader(Environment environment, DatabaseConfig databaseConfig, AdminConfig adminConfig) {
        this.environment = environment;
        this.databaseConfig = databaseConfig;
        this.adminConfig = adminConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadAndValidateConfig() {
        logger.info("=== 配置加载完成 ===");
        
        // 打印数据库配置（隐藏敏感信息）
        String dbUrl = environment.getProperty("spring.datasource.url", "未配置");
        String dbUser = environment.getProperty("spring.datasource.username", "未配置");
        logger.info("数据库连接 - URL: {}, 用户: {}", 
                dbUrl.replaceAll("password=[^&]*", "password=***"), dbUser);
        
        // 打印数据库配置类信息
        logger.info("数据库配置类 - 类型: {}, 地址: {}:{}, 数据库: {}", 
                databaseConfig.getType(), databaseConfig.getHost(), 
                databaseConfig.getPort(), databaseConfig.getName());
        
        // 打印管理员配置
        adminConfig.logConfig();
        
        logger.info("=== 配置验证完成 ===");
    }
}

