package com.northgod.server.controller;

import com.northgod.server.service.CacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器 - 已适配 Spring Boot 4.0
 * 移除了 RestTemplate 依赖，为虚拟线程环境优化了线程信息展示
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationAvailability availability;
    private final MeterRegistry meterRegistry;
    private final CacheService cacheService;

    public HealthController(JdbcTemplate jdbcTemplate,
                            ApplicationAvailability availability,
                            MeterRegistry meterRegistry,
                            CacheService cacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.availability = availability;
        this.meterRegistry = meterRegistry;
        this.cacheService = cacheService;
    }

    /**
     * 综合健康检查端点
     * 检查数据库、缓存、应用状态和系统资源
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "bookstore-server");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().format(FORMATTER));

        // 应用状态
        AvailabilityState readinessState = availability.getReadinessState();
        health.put("status", readinessState == ReadinessState.ACCEPTING_TRAFFIC ? "UP" : "DOWN");
        health.put("readiness", readinessState.toString());

        // 数据库连接检查
        Map<String, Object> databaseStatus = checkDatabase();
        health.put("database", databaseStatus);

        // 缓存状态
        Map<String, Object> cacheStatus = checkCache();
        health.put("cache", cacheStatus);

        // 系统信息（适配虚拟线程环境）
        Map<String, Object> systemInfo = getSystemInfo();
        health.put("system", systemInfo);

        // 应用指标
        Map<String, Object> metrics = getMetrics();
        health.put("metrics", metrics);

        // 总体健康状态
        boolean isHealthy = "UP".equals(health.get("status"))
                && "UP".equals(databaseStatus.get("status"))
                && "UP".equals(cacheStatus.get("status"));

        health.put("healthy", isHealthy);
        health.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() + "ms");

        logger.debug("健康检查完成，状态: {}", isHealthy ? "健康" : "不健康");

        return health;
    }

    /**
     * 就绪检查端点 (Kubernetes Readiness Probe)
     * 检查应用是否准备好接收流量
     */
    @GetMapping("/ready")
    public Map<String, Object> readiness() {
        Map<String, Object> response = new HashMap<>();
        AvailabilityState readinessState = availability.getReadinessState();

        boolean isReady = readinessState == ReadinessState.ACCEPTING_TRAFFIC;
        response.put("status", isReady ? "READY" : "NOT_READY");
        response.put("timestamp", LocalDateTime.now().format(FORMATTER));
        response.put("service", "bookstore-server");

        // 详细的就绪检查
        Map<String, Object> checks = new HashMap<>();

        // 数据库检查
        try {
            jdbcTemplate.execute("SELECT 1");
            checks.put("database", Map.of("status", "UP", "message", "连接正常"));
        } catch (DataAccessException e) {
            checks.put("database", Map.of("status", "DOWN", "message", e.getMessage()));
            isReady = false;
        }

        // 缓存检查
        try {
            cacheService.getCacheStats();
            checks.put("cache", Map.of("status", "UP", "message", "缓存正常"));
        } catch (Exception e) {
            checks.put("cache", Map.of("status", "DOWN", "message", e.getMessage()));
            isReady = false;
        }

        response.put("checks", checks);
        response.put("ready", isReady);

        return response;
    }

    /**
     * 存活检查端点 (Kubernetes Liveness Probe)
     * 检查应用是否仍在运行
     */
    @GetMapping("/live")
    public Map<String, Object> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ALIVE");
        response.put("timestamp", LocalDateTime.now().format(FORMATTER));
        response.put("service", "bookstore-server");
        response.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() + "ms");

        return response;
    }

    /**
     * 应用信息端点
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Bookstore Server");
        info.put("description", "图书进销存管理系统后端服务 - Spring Boot 4.0 版本");
        info.put("version", "1.0.0");
        // 从Spring Boot获取实际版本号，而不是硬编码
        try {
            String springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();
            info.put("springBootVersion", springBootVersion);
        } catch (Exception e) {
            logger.warn("无法获取Spring Boot版本号", e);
            info.put("springBootVersion", "4.0.1");
        }
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("environment", System.getProperty("spring.profiles.active", "default"));
        info.put("timestamp", LocalDateTime.now().format(FORMATTER));

        // 虚拟线程环境信息
        info.put("virtualThreadsEnabled", Boolean.valueOf(System.getProperty("spring.threads.virtual.enabled", "false")));

        return info;
    }

    /**
     * 检查数据库连接状态
     */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbStatus = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            jdbcTemplate.execute("SELECT 1");
            long endTime = System.currentTimeMillis();

            // 获取数据库信息
            String productName = jdbcTemplate.queryForObject("SELECT version()", String.class);

            // 获取连接池信息
            Integer activeConnections = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE application_name LIKE '%Bookstore%'",
                    Integer.class);

            dbStatus.put("status", "UP");
            dbStatus.put("responseTime", (endTime - startTime) + "ms");
            dbStatus.put("product", productName);
            dbStatus.put("activeConnections", activeConnections != null ? activeConnections : 0);
            dbStatus.put("message", "数据库连接正常");

        } catch (Exception e) {
            logger.error("数据库健康检查失败", e);
            dbStatus.put("status", "DOWN");
            dbStatus.put("error", e.getMessage());
            dbStatus.put("message", "数据库连接失败");
        }

        return dbStatus;
    }

    /**
     * 检查缓存状态
     */
    private Map<String, Object> checkCache() {
        Map<String, Object> cacheStatus = new HashMap<>();

        try {
            Map<String, Object> cacheStats = cacheService.getCacheStats();
            cacheStatus.put("status", "UP");
            cacheStatus.put("caches", cacheStats.keySet().size());
            cacheStatus.put("stats", cacheStats);
            cacheStatus.put("message", "缓存系统正常");
        } catch (Exception e) {
            logger.error("缓存健康检查失败", e);
            cacheStatus.put("status", "DOWN");
            cacheStatus.put("error", e.getMessage());
            cacheStatus.put("message", "缓存系统异常");
        }

        return cacheStatus;
    }

    /**
     * 获取系统信息（适配虚拟线程环境）
     */
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> system = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // 内存信息
        system.put("memory", Map.of(
                "free", formatBytes(runtime.freeMemory()),
                "total", formatBytes(runtime.totalMemory()),
                "max", formatBytes(runtime.maxMemory()),
                "used", formatBytes(runtime.totalMemory() - runtime.freeMemory()),
                "usagePercent", String.format("%.1f%%",
                        (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100)
        ));

        // CPU信息
        system.put("cpu", Map.of(
                "processors", runtime.availableProcessors(),
                "systemLoad", osBean.getSystemLoadAverage(),
                "architecture", osBean.getArch(),
                "os", osBean.getName() + " " + osBean.getVersion()
        ));

// 线程信息（兼容 Java 21+ 的不同环境）
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("live", threadBean.getThreadCount());
        threadInfo.put("daemon", threadBean.getDaemonThreadCount());
        threadInfo.put("peak", threadBean.getPeakThreadCount());
        threadInfo.put("totalStarted", threadBean.getTotalStartedThreadCount());

// 安全地检查虚拟线程支持（兼容不同JDK实现）
        boolean virtualThreadsSupported = true; // Java 21+ 默认支持
        try {
            // 尝试通过反射调用方法，如果不存在则捕获异常
            java.lang.reflect.Method method = threadBean.getClass().getMethod("isVirtualThreadsSupported");
            virtualThreadsSupported = (Boolean) method.invoke(threadBean);
        } catch (NoSuchMethodException e) {
            // 方法不存在，对于 Java 21+，我们假设支持虚拟线程
            logger.trace("ThreadMXBean.isVirtualThreadsSupported() 方法不存在，假设运行环境支持虚拟线程。");
        } catch (Exception e) {
            logger.warn("检查虚拟线程支持时发生异常: {}", e.getMessage());
        }
        threadInfo.put("virtualThreadsSupported", virtualThreadsSupported);

        system.put("threads", threadInfo);

        return system;
    }

    /**
     * 获取应用指标
     */
    private Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // HTTP请求指标
            Counter counter = meterRegistry.counter("http.server.requests",
                    "method", "GET",
                    "uri", "/api/books");

            metrics.put("http.requests.books", counter.count());

            // JVM指标
            metrics.put("jvm.memory.used",
                    ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
            metrics.put("jvm.threads.live",
                    ManagementFactory.getThreadMXBean().getThreadCount());
            metrics.put("jvm.gc.count",
                    ManagementFactory.getGarbageCollectorMXBeans().stream()
                            .mapToLong(gc -> gc.getCollectionCount())
                            .sum());

        } catch (Exception e) {
            logger.warn("获取指标失败", e);
            metrics.put("error", "无法获取指标数据");
        }

        return metrics;
    }

    /**
     * 格式化字节大小为可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}