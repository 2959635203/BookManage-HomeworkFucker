package com.northgod.server.service;

import com.northgod.server.config.AdminConfig;
import com.northgod.server.entity.SystemUser;
import com.northgod.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final AdminConfig adminConfig;
    
    public AuthService(UserRepository userRepository, AdminConfig adminConfig) {
        this.userRepository = userRepository;
        this.adminConfig = adminConfig;
    }
    
    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录结果，包含token和用户信息，如果验证失败返回null
     */
    public LoginResult login(String username, String password) {
        try {
            SystemUser user = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (user == null) {
                logger.warn("登录失败：用户不存在 - {}", username);
                return null;
            }
            
            if (user.getIsActive() == null || !user.getIsActive()) {
                logger.warn("登录失败：用户已被禁用 - {}", username);
                return null;
            }
            
            // 验证密码（简单SHA-256哈希，生产环境应使用BCrypt等更安全的方式）
            String passwordHash = hashPassword(password);
            if (!passwordHash.equals(user.getPasswordHash())) {
                logger.warn("登录失败：密码错误 - {}", username);
                return null;
            }
            
            // 生成简单的token（生产环境应使用JWT）
            String token = generateToken(user);
            logger.info("用户登录成功 - {}", username);
            
            // 返回登录结果，包含token和用户信息
            LoginResult result = new LoginResult();
            result.setToken(token);
            result.setUser(user);
            return result;
            
        } catch (Exception e) {
            logger.error("登录验证异常", e);
            return null;
        }
    }
    
    /**
     * 登录结果类
     */
    public static class LoginResult {
        private String token;
        private SystemUser user;
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public SystemUser getUser() {
            return user;
        }
        
        public void setUser(SystemUser user) {
            this.user = user;
        }
    }
    
    /**
     * 验证token是否有效
     * @param token token字符串
     * @return 用户信息，如果token无效返回null
     */
    public SystemUser validateToken(String token) {
        try {
            // 简单实现：从token中提取用户ID（生产环境应使用JWT）
            if (token == null || token.isEmpty()) {
                return null;
            }
            
            // 这里简化处理，实际应该解析token获取用户信息
            // 为了简单，我们假设token格式为：username:timestamp:signature
            String[] parts = token.split(":");
            if (parts.length < 2) {
                return null;
            }
            
            String username = parts[0];
            return userRepository.findByUsername(username)
                    .filter(user -> user.getIsActive() != null && user.getIsActive())
                    .orElse(null);
                    
        } catch (Exception e) {
            logger.error("Token验证异常", e);
            return null;
        }
    }
    
    /**
     * 密码哈希（SHA-256）
     * 注意：生产环境应使用BCrypt等更安全的哈希算法
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("密码哈希失败", e);
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 生成token（简单实现）
     * 生产环境应使用JWT
     */
    private String generateToken(SystemUser user) {
        long timestamp = System.currentTimeMillis();
        String signature = UUID.randomUUID().toString().substring(0, 8);
        return user.getUsername() + ":" + timestamp + ":" + signature;
    }
    
    /**
     * 创建默认管理员账户（如果不存在）
     * 使用 AdminConfig 中的配置
     */
    public void createDefaultAdminIfNotExists() {
        if (!adminConfig.isAutoCreate()) {
            logger.info("自动创建管理员账户已禁用");
            return;
        }
        
        String adminUsername = adminConfig.getUsername();
        Optional<SystemUser> existingAdmin = userRepository.findByUsername(adminUsername);
        
        if (existingAdmin.isEmpty()) {
            // 创建新管理员
            SystemUser admin = new SystemUser();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(hashPassword(adminConfig.getPassword()));
            admin.setRole(adminConfig.getRole());
            admin.setFullName(adminConfig.getFullName());
            admin.setIsActive(true);
            userRepository.save(admin);
            logger.info("创建默认管理员账户：{}/{}", adminUsername, "***");
            adminConfig.logConfig();
        } else {
            // 管理员已存在，检查并更新fullName
            SystemUser admin = existingAdmin.get();
            String expectedFullName = adminConfig.getFullName();
            if (admin.getFullName() == null || admin.getFullName().isEmpty() || 
                !admin.getFullName().equals(expectedFullName)) {
                admin.setFullName(expectedFullName);
                userRepository.save(admin);
                logger.info("更新管理员账户姓名：{} -> {}", adminUsername, expectedFullName);
            }
            logger.debug("管理员账户已存在，跳过创建");
        }
    }
}

