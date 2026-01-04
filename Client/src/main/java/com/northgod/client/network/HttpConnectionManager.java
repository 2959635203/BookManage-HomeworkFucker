package com.northgod.client.network;

import com.northgod.client.config.AppConfig;
import com.northgod.client.util.LogUtil;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpConnectionManager {
    private static volatile HttpClient httpClient;
    private static final Object lock = new Object();
    private static ExecutorService executorService;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private HttpConnectionManager() {
        // 私有构造器
    }

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (lock) {
                if (httpClient == null) {
                    // 使用虚拟线程（Java 21+）优化IO密集型任务
                    executorService = Executors.newVirtualThreadPerTaskExecutor();

                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(AppConfig.getConnectTimeout()))
                            .executor(executorService)
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .version(HttpClient.Version.HTTP_2)
                            .priority(1)
                            .build();

                    LogUtil.info("HTTP连接池初始化完成，使用虚拟线程");
                }
            }
        }
        return httpClient;
    }

    /**
     * 获取HTTP客户端统计信息
     */
    public static String getHttpClientStats() {
        if (executorService != null && !executorService.isShutdown()) {
            // 虚拟线程池不支持传统线程池的统计信息
            return "HTTP客户端使用虚拟线程池，已初始化";
        }
        return "HTTP线程池未初始化";
    }

    /**
     * 优雅关闭HTTP连接池
     */
    public static void shutdown() {
        synchronized (lock) {
            if (executorService != null && !executorService.isShutdown()) {
                LogUtil.info("开始关闭HTTP连接池...");
                executorService.shutdown();
                
                try {
                    // 等待任务完成，最多等待30秒
                    if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        LogUtil.warn("HTTP连接池关闭超时，强制关闭");
                        executorService.shutdownNow();
                        
                        // 再等待5秒
                        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                            LogUtil.error("HTTP连接池强制关闭失败");
                        }
                    }
                    LogUtil.info("HTTP连接池已关闭");
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                    LogUtil.error("HTTP连接池关闭被中断", e);
                }
            }
        }
    }

    /**
     * 自定义线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final ThreadGroup group;

        NamedThreadFactory(String poolName) {
            // 避免使用已弃用的SecurityManager
            group = Thread.currentThread().getThreadGroup();
            namePrefix = poolName + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }
}