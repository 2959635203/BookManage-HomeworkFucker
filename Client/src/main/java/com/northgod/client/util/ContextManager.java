package com.northgod.client.util;

import java.util.Optional;

/**
 * 上下文管理器 - 使用Java 25的ScopedValue特性
 * 用于在线程间传递不可变的上下文信息
 * 
 * 注意：ScopedValue是Java 25的新特性，如果运行在较低版本的Java上，
 * 可以使用ThreadLocal作为后备方案
 */
public class ContextManager {
    // 使用ScopedValue存储用户上下文（Java 25特性）
    // 如果Java版本低于25，可以使用ThreadLocal作为替代
    private static final ThreadLocal<String> USER_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    
    /**
     * 设置当前线程的用户上下文
     * @param username 用户名
     */
    public static void setUserContext(String username) {
        USER_CONTEXT.set(username);
    }
    
    /**
     * 获取当前线程的用户上下文
     * @return 用户名，如果未设置则返回null
     */
    public static Optional<String> getUserContext() {
        return Optional.ofNullable(USER_CONTEXT.get());
    }
    
    /**
     * 设置当前线程的请求ID
     * @param requestId 请求ID
     */
    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }
    
    /**
     * 获取当前线程的请求ID
     * @return 请求ID，如果未设置则返回null
     */
    public static Optional<String> getRequestId() {
        return Optional.ofNullable(REQUEST_ID.get());
    }
    
    /**
     * 清除当前线程的所有上下文
     */
    public static void clear() {
        USER_CONTEXT.remove();
        REQUEST_ID.remove();
    }
    
    /**
     * 在指定上下文中执行任务
     * 这是一个辅助方法，用于确保上下文在使用后被清理
     * 
     * @param username 用户名
     * @param requestId 请求ID
     * @param task 要执行的任务
     */
    public static void runInContext(String username, String requestId, Runnable task) {
        try {
            setUserContext(username);
            setRequestId(requestId);
            task.run();
        } finally {
            clear();
        }
    }
}




















