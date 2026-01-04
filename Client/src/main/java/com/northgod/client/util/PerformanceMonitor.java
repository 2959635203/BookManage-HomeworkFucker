package com.northgod.client.util;

import javax.swing.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMonitor {
    private static final Map<String, RequestStats> requestStats = new ConcurrentHashMap<>();
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final AtomicLong failedRequests = new AtomicLong(0);

    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public static void recordRequest(String endpoint, long duration, boolean success) {
        totalRequests.incrementAndGet();
        if (!success) {
            failedRequests.incrementAndGet();
        }

        requestStats.computeIfAbsent(endpoint, k -> new RequestStats())
                .record(duration, success);

        LogUtil.performance("请求记录 - 端点: " + endpoint + ", 耗时: " + duration + "ms, 成功: " + success);
    }

    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 内存统计
        stats.put("heapUsed", memoryMXBean.getHeapMemoryUsage().getUsed());
        stats.put("heapMax", memoryMXBean.getHeapMemoryUsage().getMax());
        stats.put("nonHeapUsed", memoryMXBean.getNonHeapMemoryUsage().getUsed());

        // 线程统计
        stats.put("threadCount", threadMXBean.getThreadCount());
        stats.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());

        // 请求统计
        long total = totalRequests.get();
        long failed = failedRequests.get();
        stats.put("totalRequests", total);
        stats.put("failedRequests", failed);
        stats.put("successRate", total > 0 ?
                (double) (total - failed) / total * 100 : 0);

        // 各端点统计
        Map<String, Map<String, Object>> endpointStats = new HashMap<>();
        requestStats.forEach((endpoint, endpointStat) -> {
            Map<String, Object> endpointData = new HashMap<>();
            endpointData.put("count", endpointStat.getCount());
            endpointData.put("avgDuration", endpointStat.getAverageDuration());
            endpointData.put("successRate", endpointStat.getSuccessRate());
            endpointStats.put(endpoint, endpointData);
        });
        stats.put("endpointStats", endpointStats);

        return stats;
    }

    public static void showStatsDialog(JComponent parent) {
        Map<String, Object> stats = getStats();
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(2);

        StringBuilder message = new StringBuilder();
        message.append("=== 性能统计 ===\n\n");

        // 内存信息
        long heapUsedMB = ((Long) stats.get("heapUsed")) / 1024 / 1024;
        long heapMaxMB = ((Long) stats.get("heapMax")) / 1024 / 1024;
        message.append("内存使用:\n");
        message.append("  堆内存: ").append(format.format(heapUsedMB)).append(" MB / ")
                .append(format.format(heapMaxMB)).append(" MB\n");

        // 线程信息
        message.append("\n线程信息:\n");
        message.append("  活动线程: ").append(stats.get("threadCount")).append("\n");
        message.append("  守护线程: ").append(stats.get("daemonThreadCount")).append("\n");

        // 请求信息
        message.append("\n请求统计:\n");
        message.append("  总请求数: ").append(stats.get("totalRequests")).append("\n");
        message.append("  失败请求: ").append(stats.get("failedRequests")).append("\n");
        message.append("  成功率: ").append(format.format(stats.get("successRate"))).append("%\n");

        JOptionPane.showMessageDialog(parent, message.toString(), "性能统计",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static class RequestStats {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);

        public void record(long duration, boolean success) {
            count.incrementAndGet();
            totalDuration.addAndGet(duration);
            if (success) {
                successCount.incrementAndGet();
            }
        }

        public long getCount() {
            return count.get();
        }

        public double getAverageDuration() {
            return count.get() > 0 ? (double) totalDuration.get() / count.get() : 0;
        }

        public double getSuccessRate() {
            return count.get() > 0 ? (double) successCount.get() / count.get() * 100 : 0;
        }
    }

    // 简单的性能测量工具
    public static class Timer {
        private final Instant start;
        private final String operation;

        private Timer(String operation) {
            this.start = Instant.now();
            this.operation = operation;
            LogUtil.debug("开始计时: " + operation);
        }

        public static Timer start(String operation) {
            return new Timer(operation);
        }

        public long stop() {
            Duration duration = Duration.between(start, Instant.now());
            long millis = duration.toMillis();
            LogUtil.performance(operation + " 耗时: " + millis + "ms");
            return millis;
        }

        public long stopAndRecord() {
            long duration = stop();
            recordRequest(operation, duration, true);
            return duration;
        }
    }
}