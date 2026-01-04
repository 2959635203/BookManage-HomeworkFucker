package com.northgod.client.config;

import com.northgod.client.util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Properties;

/**
 * 应用程序配置管理类
 * 支持从外部配置文件、内部配置文件、环境变量和系统属性读取配置
 * 优先级：外部配置文件 > 环境变量 > 系统属性 > 内部配置文件 > 默认值
 */
public class AppConfig {
    private static final String CONFIG_FILE = "/application.properties";
    private static final String EXTERNAL_CONFIG_FILE = "config/application.properties";
    private static final Properties properties = new Properties();
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        // 加载默认配置
        properties.setProperty("api.base.url", "http://localhost:8080/api");
        properties.setProperty("api.connect.timeout", "30");
        properties.setProperty("api.request.timeout", "60");
        properties.setProperty("api.max.retries", "3");
        properties.setProperty("api.retry.delay", "500");
        
        // 从内部配置文件加载（jar包内的配置文件）
        try (InputStream inputStream = AppConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                Properties internalProps = new Properties();
                internalProps.load(inputStream);
                properties.putAll(internalProps);
                LogUtil.info("已加载内部配置文件: " + CONFIG_FILE);
            }
        } catch (IOException e) {
            LogUtil.warn("无法加载内部配置文件，使用默认配置: " + e.getMessage());
        }
        
        // 从外部配置文件加载（应用目录下的config/application.properties）
        // 外部配置会覆盖内部配置
        File externalConfigFile = getExternalConfigFile();
        if (externalConfigFile != null && externalConfigFile.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(externalConfigFile)) {
                Properties externalProps = new Properties();
                externalProps.load(fileInputStream);
                properties.putAll(externalProps);
                LogUtil.info("已加载外部配置文件: " + externalConfigFile.getAbsolutePath());
            } catch (IOException e) {
                LogUtil.warn("无法加载外部配置文件: " + e.getMessage());
            }
        } else {
            LogUtil.info("外部配置文件不存在，使用内部配置: " + (externalConfigFile != null ? externalConfigFile.getAbsolutePath() : "null"));
        }
        
        // 从环境变量覆盖
        String envBaseUrl = System.getenv("API_BASE_URL");
        if (envBaseUrl != null && !envBaseUrl.isEmpty()) {
            properties.setProperty("api.base.url", envBaseUrl);
            LogUtil.info("使用环境变量 API_BASE_URL: " + envBaseUrl);
        }
        
        // 从系统属性覆盖（最高优先级）
        String sysBaseUrl = System.getProperty("api.base.url");
        if (sysBaseUrl != null && !sysBaseUrl.isEmpty()) {
            properties.setProperty("api.base.url", sysBaseUrl);
            LogUtil.info("使用系统属性 api.base.url: " + sysBaseUrl);
        }
    }
    
    /**
     * 获取外部配置文件路径
     * 优先查找应用目录下的config/application.properties
     * 支持jpackage打包后的应用结构：应用根目录/app/xxx.jar 和 应用根目录/config/application.properties
     */
    private static File getExternalConfigFile() {
        try {
            // 方法1：从jar文件路径推断应用根目录（最可靠的方法）
            // jpackage打包后，jar文件在 app/ 目录下，config在根目录
            try {
                java.net.URL codeSourceUrl = AppConfig.class.getProtectionDomain().getCodeSource().getLocation();
                if (codeSourceUrl != null) {
                    String jarPath = codeSourceUrl.getPath();
                    if (jarPath != null && !jarPath.isEmpty()) {
                        // 处理Windows路径问题（file:/C:/path 或 /C:/path）
                        if (jarPath.startsWith("file:/")) {
                            jarPath = jarPath.substring(6);
                        }
                        // 处理Unix风格的路径（/C:/path -> C:/path）
                        if (jarPath.startsWith("/") && jarPath.length() > 3 && jarPath.charAt(2) == ':') {
                            jarPath = jarPath.substring(1);
                        }
                        // URL解码
                        jarPath = URLDecoder.decode(jarPath, "UTF-8");
                        
                        File jarFile = new File(jarPath);
                        if (jarFile.exists()) {
                            File parentDir = jarFile.getParentFile();
                            if (parentDir != null) {
                                // 如果jar在 app/ 目录下（jpackage打包的应用）
                                if ("app".equals(parentDir.getName())) {
                                    // 应用根目录是 app 的父目录
                                    File appRootDir = parentDir.getParentFile();
                                    if (appRootDir != null) {
                                        File configFile = new File(appRootDir, EXTERNAL_CONFIG_FILE);
                                        LogUtil.info("从jar路径推断配置文件（jpackage应用）: " + configFile.getAbsolutePath());
                                        return configFile;
                                    }
                                }
                                // 如果jar不在app目录，尝试在jar所在目录查找config
                                File configFile = new File(parentDir, EXTERNAL_CONFIG_FILE);
                                if (configFile.exists()) {
                                    LogUtil.info("在jar同目录找到配置文件: " + configFile.getAbsolutePath());
                                    return configFile;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.warn("从jar路径推断配置文件失败: " + e.getMessage());
            }
            
            // 方法2：检查APPDIR环境变量（jpackage设置）
            String appDirEnv = System.getenv("APPDIR");
            if (appDirEnv != null && !appDirEnv.isEmpty()) {
                File appDir = new File(appDirEnv);
                File configFile = new File(appDir, EXTERNAL_CONFIG_FILE);
                if (configFile.exists()) {
                    LogUtil.info("从APPDIR环境变量找到配置文件: " + configFile.getAbsolutePath());
                    return configFile;
                }
            }
            
            // 方法3：检查jpackage.app-path系统属性
            String appDirPath = System.getProperty("jpackage.app-path");
            if (appDirPath != null && !appDirPath.isEmpty()) {
                File appDir = new File(appDirPath);
                File configFile = new File(appDir, EXTERNAL_CONFIG_FILE);
                if (configFile.exists()) {
                    LogUtil.info("从jpackage.app-path系统属性找到配置文件: " + configFile.getAbsolutePath());
                    return configFile;
                }
            }
            
            // 方法4：从classpath推断应用目录
            String classPath = System.getProperty("java.class.path");
            if (classPath != null && classPath.contains("app")) {
                String[] paths = classPath.split(File.pathSeparator);
                for (String path : paths) {
                    if (path.contains("app") && path.endsWith(".jar")) {
                        File jarFile = new File(path);
                        if (jarFile.exists()) {
                            File parentDir = jarFile.getParentFile();
                            if (parentDir != null && "app".equals(parentDir.getName())) {
                                File appRootDir = parentDir.getParentFile();
                                if (appRootDir != null) {
                                    File configFile = new File(appRootDir, EXTERNAL_CONFIG_FILE);
                                    if (configFile.exists()) {
                                        LogUtil.info("从classpath推断配置文件: " + configFile.getAbsolutePath());
                                        return configFile;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 方法5：使用当前工作目录（如果配置文件存在）
            File currentDir = new File(System.getProperty("user.dir"));
            File configFile = new File(currentDir, EXTERNAL_CONFIG_FILE);
            if (configFile.exists()) {
                LogUtil.info("从当前工作目录找到配置文件: " + configFile.getAbsolutePath());
                return configFile;
            }
            
            // 方法6：尝试在用户目录查找（开发环境）
            File userHome = new File(System.getProperty("user.home"));
            File userConfigFile = new File(userHome, ".bookstore-client/" + EXTERNAL_CONFIG_FILE);
            if (userConfigFile.exists()) {
                LogUtil.info("从用户目录找到配置文件: " + userConfigFile.getAbsolutePath());
                return userConfigFile;
            }
            
        } catch (Exception e) {
            LogUtil.warn("无法确定外部配置文件路径: " + e.getMessage());
        }
        
        // 如果所有方法都失败，返回一个默认路径（即使不存在，也返回以便日志记录）
        File defaultConfigFile = new File(System.getProperty("user.dir"), EXTERNAL_CONFIG_FILE);
        LogUtil.info("使用默认配置文件路径: " + defaultConfigFile.getAbsolutePath());
        return defaultConfigFile;
    }
    
    public static String getBaseUrl() {
        return properties.getProperty("api.base.url");
    }
    
    public static int getConnectTimeout() {
        return Integer.parseInt(properties.getProperty("api.connect.timeout", "30"));
    }
    
    public static int getRequestTimeout() {
        return Integer.parseInt(properties.getProperty("api.request.timeout", "60"));
    }
    
    public static int getMaxRetries() {
        return Integer.parseInt(properties.getProperty("api.max.retries", "3"));
    }
    
    public static int getRetryDelay() {
        return Integer.parseInt(properties.getProperty("api.retry.delay", "500"));
    }
    
    public static String getConfigFilePath() {
        File externalFile = getExternalConfigFile();
        return externalFile != null ? externalFile.getAbsolutePath() : "内部配置文件";
    }
}
