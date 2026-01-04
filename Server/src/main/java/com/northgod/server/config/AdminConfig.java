package com.northgod.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * 管理员账户配置类
 * 从独立的配置文件中读取管理员账户信息
 */
@Configuration
@PropertySource(value = "classpath:admin.properties", encoding = "UTF-8")
public class AdminConfig {

    private static final Logger logger = LoggerFactory.getLogger(AdminConfig.class);

    @Value("${admin.username:admin}")
    private String username;

    @Value("${admin.password:admin123}")
    private String password;

    @Value("${admin.role:ADMIN}")
    private String role;

    @Value("${admin.full-name:系统管理员}")
    private String fullName;

    @Value("${admin.auto-create:true}")
    private boolean autoCreate;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    /**
     * 打印配置信息（隐藏密码）
     */
    public void logConfig() {
        logger.info("管理员配置 - 用户名: {}, 角色: {}, 全名: {}, 自动创建: {}", 
                username, role, fullName, autoCreate);
        logger.debug("管理员密码已配置（已隐藏）");
    }
}



