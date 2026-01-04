package com.northgod.server.controller;

import com.northgod.server.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
        // 启动时创建默认管理员账户
        authService.createDefaultAdminIfNotExists();
    }
    
    /**
     * 用户登录接口
     * POST /auth/login
     * 
     * 请求体：
     * {
     *   "username": "admin",
     *   "password": "admin123"
     * }
     * 
     * 响应：
     * {
     *   "success": true,
     *   "message": "登录成功",
     *   "data": {
     *     "token": "admin:1234567890:abc12345",
     *     "username": "admin",
     *     "role": "ADMIN"
     *   }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.debug("用户登录尝试: {}", request.getUsername());
            
            var loginResult = authService.login(request.getUsername(), request.getPassword());
            
            if (loginResult == null || loginResult.getToken() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "用户名或密码错误");
                errorResponse.put("code", "LOGIN_FAILED");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            
            // 返回完整的用户信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", loginResult.getToken());
            data.put("username", loginResult.getUser().getUsername());
            data.put("role", loginResult.getUser().getRole() != null ? loginResult.getUser().getRole() : "STAFF");
            data.put("fullName", loginResult.getUser().getFullName() != null ? loginResult.getUser().getFullName() : "");
            response.put("data", data);
            
            logger.info("用户登录成功: {}", request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("登录处理异常", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "登录失败: " + e.getMessage());
            errorResponse.put("code", "LOGIN_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 验证token接口
     * GET /auth/validate
     * Header: Authorization: Bearer {token}
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.put("success", false);
            response.put("message", "缺少认证信息");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        String token = authorization.substring(7);
        var user = authService.validateToken(token);
        
        if (user == null) {
            response.put("success", false);
            response.put("message", "Token无效或已过期");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        response.put("success", true);
        response.put("message", "Token有效");
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        
        @NotBlank(message = "密码不能为空")
        private String password;
        
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
}

