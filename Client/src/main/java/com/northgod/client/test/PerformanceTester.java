package com.northgod.client.test;

import com.northgod.client.service.ApiClient;
import com.northgod.client.util.LogUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceTester {
    private final ApiClient apiClient;
    private final ExecutorService executorService;

    public PerformanceTester() {
        this.apiClient = new ApiClient();
        this.executorService = Executors.newFixedThreadPool(50);
        LogUtil.info("性能测试器初始化完成，线程池大小: 50");
    }

    public void runConcurrentTest(int numRequests) throws InterruptedException {
        LogUtil.info("=== 开始并发性能测试 ===");
        LogUtil.info("请求数量: " + numRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 创建并发请求
        CompletableFuture<?>[] futures = new CompletableFuture[numRequests];
        for (int i = 0; i < numRequests; i++) {
            final int requestId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    String response = apiClient.get("/books");
                    successCount.incrementAndGet();
                    if (requestId % 100 == 0) {
                        LogUtil.debug("请求 " + requestId + " 成功");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    if (requestId % 100 == 0) {
                        LogUtil.warn("请求 " + requestId + " 失败: " + e.getMessage());
                    }
                }
            }, executorService);
        }

        // 等待所有请求完成
        CompletableFuture.allOf(futures).join();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        LogUtil.info("\n=== 测试结果 ===");
        LogUtil.info("成功请求: " + successCount.get());
        LogUtil.info("失败请求: " + failureCount.get());
        LogUtil.info("总耗时: " + totalTime + "ms");
        LogUtil.info("平均响应时间: " + (totalTime / (double) numRequests) + "ms/请求");
        LogUtil.info("吞吐量: " + (numRequests / (totalTime / 1000.0)) + "请求/秒");

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        LogUtil.info("性能测试完成，线程池已关闭");
    }

    public static void main(String[] args) throws InterruptedException {
        PerformanceTester tester = new PerformanceTester();
        tester.runConcurrentTest(1000);
    }
}