package com.northgod.client.util;

import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager {

    private static volatile ThreadPoolManager instance;

    // IO密集型任务线程池（网络请求、文件操作等）
    @Getter
    private final ExecutorService ioThreadPool;

    // CPU密集型任务线程池（计算、数据处理等）
    @Getter
    private final ExecutorService cpuThreadPool;

    // 定时任务调度器
    @Getter
    private final ScheduledExecutorService scheduledThreadPool;

    // 单任务队列线程池（用于顺序执行任务）
    @Getter
    private final ExecutorService singleThreadPool;

    private ThreadPoolManager() {
        // 获取处理器核心数
        int coreCount = Runtime.getRuntime().availableProcessors();

        // IO密集型任务使用虚拟线程（Java 21+），适合IO密集型任务
        ioThreadPool = Executors.newVirtualThreadPerTaskExecutor();

        // CPU密集型线程池：核心数，最大2倍核心数，队列50
        cpuThreadPool = new ThreadPoolExecutor(
                coreCount,
                coreCount * 2,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                new NamedThreadFactory("CPU-Pool"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 定时任务线程池
        scheduledThreadPool = Executors.newScheduledThreadPool(
                coreCount,
                new NamedThreadFactory("Scheduled-Pool")
        );

        // 单线程池，用于顺序执行任务
        singleThreadPool = Executors.newSingleThreadExecutor(
                new NamedThreadFactory("Single-Pool")
        );

        // 使用 String.format 格式化字符串
        LogUtil.info(String.format("线程池管理器初始化完成 - 核心数: %d, IO线程池: 虚拟线程, CPU线程池: %d/%d",
                coreCount, coreCount, coreCount * 2));

        // 不在构造函数中添加关闭钩子，避免在应用程序关闭过程中初始化时出错
        // 关闭钩子应该在 Main.java 中统一管理
    }

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    /**
     * 提交IO任务
     */
    public CompletableFuture<Void> submitIoTask(Runnable task) {
        return CompletableFuture.runAsync(task, ioThreadPool);
    }

    /**
     * 提交IO任务并返回结果
     */
    public <T> CompletableFuture<T> submitIoTask(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ioThreadPool);
    }

    /**
     * 提交CPU任务
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Void> submitCpuTask(Runnable task) {
        return CompletableFuture.runAsync(task, cpuThreadPool);
    }

    /**
     * 获取线程池状态信息
     */
    public String getPoolStatus() {
        // IO线程池使用虚拟线程，不支持传统统计
        String ioStatus = "IO线程池: [虚拟线程池，无传统统计信息]";
        
        if (cpuThreadPool instanceof ThreadPoolExecutor cpuExecutor) {
            return String.format(
                    "%s%nCPU线程池: [活跃: %d, 核心: %d, 最大: %d, 队列: %d, 完成任务: %d]",
                    ioStatus,
                    cpuExecutor.getActiveCount(), cpuExecutor.getCorePoolSize(),
                    cpuExecutor.getMaximumPoolSize(), cpuExecutor.getQueue().size(),
                    cpuExecutor.getCompletedTaskCount()
            );
        }
        
        return ioStatus + "\nCPU线程池: [状态未知]";
    }

    /**
     * 优雅关闭所有线程池
     */
    public void shutdown() {
        LogUtil.info("开始关闭线程池...");

        ioThreadPool.shutdown();
        cpuThreadPool.shutdown();
        scheduledThreadPool.shutdown();
        singleThreadPool.shutdown();

        try {
            if (!ioThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                ioThreadPool.shutdownNow();
            }
            if (!cpuThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                cpuThreadPool.shutdownNow();
            }
            if (!scheduledThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledThreadPool.shutdownNow();
            }
            if (!singleThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                singleThreadPool.shutdownNow();
            }

            LogUtil.info("所有线程池已关闭");
        } catch (InterruptedException e) {
            ioThreadPool.shutdownNow();
            cpuThreadPool.shutdownNow();
            scheduledThreadPool.shutdownNow();
            singleThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
            LogUtil.error("线程池关闭被中断", e);
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
            // 不再使用已弃用的 SecurityManager
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