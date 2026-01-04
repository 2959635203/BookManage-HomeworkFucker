package com.northgod.client.service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northgod.client.config.AppConfig;
import com.northgod.client.network.HttpConnectionManager;
import com.northgod.client.util.ContextManager;
import com.northgod.client.util.JsonUtil;
import com.northgod.client.util.LogUtil;
import com.northgod.client.util.ThreadPoolManager;

public class ApiClient {
    private final String baseUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;
    private String token;

    // 请求重试配置
    private final int maxRetries;
    private final Duration retryDelay;

    // 连接超时时间
    private final Duration requestTimeout;

    // 请求统计
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    public ApiClient() {
        this.baseUrl = AppConfig.getBaseUrl();
        this.httpClient = HttpConnectionManager.getHttpClient();
        this.maxRetries = AppConfig.getMaxRetries();
        this.retryDelay = Duration.ofMillis(AppConfig.getRetryDelay());
        this.requestTimeout = Duration.ofSeconds(AppConfig.getRequestTimeout());

        LogUtil.debug("ApiClient初始化完成，BaseURL: " + baseUrl);
    }

    public void setToken(String token) {
        this.token = token;
        LogUtil.debug("Token已设置");
    }

    // 同步GET请求
    public String get(String endpoint) throws Exception {
        totalRequests.incrementAndGet();
        // 设置请求ID用于追踪
        String requestId = "GET-" + System.currentTimeMillis();
        ContextManager.setRequestId(requestId);
        try {
            // 构建请求
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + endpoint))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Bookstore-Client/1.0")
                    .timeout(requestTimeout);

            addAuthHeader(builder);

            HttpRequest request = builder.build();

            LogUtil.debug("发送HTTP GET请求: " + endpoint);

            // 带重试机制的请求
            HttpResponse<String> response = sendWithRetry(request);

            checkResponse(response, endpoint, "GET");

            return response.body();
        } finally {
            ContextManager.clear();
        }
    }

    // 异步GET请求（使用自定义线程池）
    public CompletableFuture<String> getAsync(String endpoint) {
        return ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                return get(endpoint);
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                LogUtil.error("异步GET请求失败: " + endpoint, e);
                throw new RuntimeException(getUserFriendlyErrorMessage(e, endpoint, "GET"), e);
            }
        });
    }

    public String post(String endpoint, Object body) throws Exception {
        totalRequests.incrementAndGet();
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            LogUtil.error("序列化请求体失败: " + endpoint, e);
            throw new Exception("请求体序列化失败: " + e.getMessage(), e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Bookstore-Client/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(requestTimeout);

        addAuthHeader(builder);

        HttpRequest request = builder.build();
        LogUtil.debug("发送HTTP POST请求: " + endpoint + " (body大小: " + jsonBody.length() + " 字符)");

        HttpResponse<String> response = sendWithRetry(request);

        checkResponse(response, endpoint, "POST");

        return response.body();
    }

    // 异步POST请求（使用自定义线程池）
    public CompletableFuture<String> postAsync(String endpoint, Object body) {
        return ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                return post(endpoint, body);
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                LogUtil.error("异步POST请求失败: " + endpoint, e);
                throw new RuntimeException(getUserFriendlyErrorMessage(e, endpoint, "POST"), e);
            }
        });
    }

    public String put(String endpoint, Object body) throws Exception {
        totalRequests.incrementAndGet();
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            LogUtil.error("序列化请求体失败: " + endpoint, e);
            throw new Exception("请求体序列化失败: " + e.getMessage(), e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Bookstore-Client/1.0")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(requestTimeout);

        addAuthHeader(builder);

        HttpRequest request = builder.build();
        LogUtil.debug("发送HTTP PUT请求: " + endpoint);

        HttpResponse<String> response = sendWithRetry(request);

        checkResponse(response, endpoint, "PUT");

        return response.body();
    }

    // 异步PUT请求
    public CompletableFuture<String> putAsync(String endpoint, Object body) {
        return ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                return put(endpoint, body);
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                LogUtil.error("异步PUT请求失败: " + endpoint, e);
                throw new RuntimeException(getUserFriendlyErrorMessage(e, endpoint, "PUT"), e);
            }
        });
    }

    public String delete(String endpoint) throws Exception {
        totalRequests.incrementAndGet();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Accept", "application/json")
                .header("User-Agent", "Bookstore-Client/1.0")
                .DELETE()
                .timeout(requestTimeout);

        addAuthHeader(builder);

        HttpRequest request = builder.build();
        LogUtil.debug("发送HTTP DELETE请求: " + endpoint);

        HttpResponse<String> response = sendWithRetry(request);

        checkResponse(response, endpoint, "DELETE");

        return response.body();
    }

    // 异步DELETE请求
    public CompletableFuture<String> deleteAsync(String endpoint) {
        return ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                return delete(endpoint);
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                LogUtil.error("异步DELETE请求失败: " + endpoint, e);
                throw new RuntimeException(getUserFriendlyErrorMessage(e, endpoint, "DELETE"), e);
            }
        });
    }

    /**
     * 搜索书籍（服务端搜索）
     */
    public String searchBooks(String keyword) throws Exception {
        totalRequests.incrementAndGet();
        String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        String endpoint = "/books/search?keyword=" + encodedKeyword;
        return get(endpoint);
    }

    /**
     * 异步搜索书籍
     */
    public CompletableFuture<String> searchBooksAsync(String keyword) {
        return ThreadPoolManager.getInstance().submitIoTask(() -> {
            try {
                return searchBooks(keyword);
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                LogUtil.error("搜索书籍失败: " + keyword, e);
                throw new RuntimeException(getUserFriendlyErrorMessage(e, "/books/search", "GET"), e);
            }
        });
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryable(Exception e) {
        // 连接异常、超时异常、IO异常通常可以重试
        if (e instanceof ConnectException || 
            e instanceof HttpTimeoutException || 
            e instanceof IOException) {
            return true;
        }
        
        // 检查异常消息中是否包含可重试的关键词
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network") ||
                   lowerMessage.contains("reset") ||
                   lowerMessage.contains("refused");
        }
        
        return false;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        Exception lastException = null;
        HttpResponse<String> lastResponse = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // 检查HTTP状态码，4xx错误通常不应该重试（除了408和429）
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return response; // 成功，直接返回
                }
                
                // 4xx错误通常不应该重试（除了408超时和429限流）
                if (statusCode >= 400 && statusCode < 500 && statusCode != 408 && statusCode != 429) {
                    LogUtil.warn("收到客户端错误状态码 " + statusCode + "，不进行重试");
                    return response; // 返回响应，让调用者处理
                }
                
                // 5xx错误和408、429可以重试
                lastResponse = response;
                if (attempt < maxRetries) {
                    LogUtil.warn("请求返回状态码 " + statusCode + "，第 " + attempt + " 次重试");
                }
                
            } catch (Exception e) {
                lastException = e;
                
                // 检查是否可重试
                if (!isRetryable(e)) {
                    LogUtil.warn("遇到不可重试的异常: " + e.getClass().getSimpleName());
                    throw e; // 不可重试的异常直接抛出
                }
                
                String errorType = getErrorType(e);
                LogUtil.warn("请求失败，第 " + attempt + " 次重试 (" + errorType + "): " +
                        getUserFriendlyErrorMessage(e, request.uri().toString(), ""));

                // 不是最后一次重试时等待
                if (attempt < maxRetries) {
                    long delay = retryDelay.toMillis() * attempt; // 指数退避
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("请求被中断", ie);
                    }
                }
            }
        }

        // 如果最后一次尝试有响应，返回响应
        if (lastResponse != null) {
            return lastResponse;
        }
        
        // 否则抛出异常
        LogUtil.error("请求失败，重试 " + maxRetries + " 次后仍然失败");
        throw new RuntimeException("请求失败，重试 " + maxRetries + " 次后仍然失败", lastException);
    }

    private void checkResponse(HttpResponse<String> response, String endpoint, String method) throws Exception {
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            failedRequests.incrementAndGet();
            String errorBody = response.body();
            StringBuilder errorMessage = new StringBuilder();

            errorMessage.append("HTTP请求失败 (").append(method).append(" ").append(endpoint).append(")");
            errorMessage.append("\n状态码: ").append(statusCode);

            // 根据状态码提供更友好的错误信息
            errorMessage.append("\n").append(getHttpStatusMessage(statusCode));

            if (errorBody != null && !errorBody.isEmpty()) {
                try {
                    Map<String, Object> error = JsonUtil.parseJson(errorBody);
                    if (error != null && error.containsKey("message")) {
                        errorMessage.append("\n错误详情: ").append(error.get("message"));
                    } else {
                        errorMessage.append("\n响应内容: ").append(errorBody.substring(0, Math.min(200, errorBody.length())));
                        if (errorBody.length() > 200) errorMessage.append("...");
                    }
                } catch (Exception e) {
                    errorMessage.append("\n响应内容: ").append(errorBody.substring(0, Math.min(200, errorBody.length())));
                    if (errorBody.length() > 200) errorMessage.append("...");
                }
            }

            // 添加建议解决方案
            errorMessage.append("\n\n建议解决方案:");
            if (statusCode == 401) {
                errorMessage.append("\n1. 请检查登录状态，重新登录");
                errorMessage.append("\n2. 检查token是否已过期");
            } else if (statusCode == 403) {
                errorMessage.append("\n1. 检查是否有权限访问该资源");
                errorMessage.append("\n2. 联系管理员获取权限");
            } else if (statusCode == 404) {
                errorMessage.append("\n1. 检查请求地址是否正确");
                errorMessage.append("\n2. 确认资源是否存在");
            } else if (statusCode == 500) {
                errorMessage.append("\n1. 服务器内部错误，请稍后重试");
                errorMessage.append("\n2. 联系管理员检查服务器状态");
            } else if (statusCode == 502 || statusCode == 503 || statusCode == 504) {
                errorMessage.append("\n1. 服务器暂时不可用，请稍后重试");
                errorMessage.append("\n2. 检查网络连接是否正常");
                errorMessage.append("\n3. 联系管理员确认服务器状态");
            } else {
                errorMessage.append("\n1. 检查网络连接");
                errorMessage.append("\n2. 稍后重试");
                errorMessage.append("\n3. 联系技术支持");
            }

            LogUtil.error("HTTP请求失败: " + errorMessage.toString());
            throw new Exception(errorMessage.toString());
        }
    }

    /**
     * 获取HTTP状态码的友好描述
     */
    private String getHttpStatusMessage(int statusCode) {
        switch (statusCode) {
            case 400: return "请求参数错误，请检查输入";
            case 401: return "未授权，需要登录或token无效";
            case 403: return "禁止访问，权限不足";
            case 404: return "资源未找到";
            case 405: return "请求方法不允许";
            case 408: return "请求超时";
            case 413: return "请求数据过大";
            case 415: return "不支持的媒体类型";
            case 429: return "请求过于频繁，请稍后重试";
            case 500: return "服务器内部错误";
            case 502: return "网关错误";
            case 503: return "服务不可用";
            case 504: return "网关超时";
            default:
                if (statusCode >= 400 && statusCode < 500) return "客户端错误";
                if (statusCode >= 500) return "服务器错误";
                return "未知错误";
        }
    }

    /**
     * 获取异常类型
     */
    private String getErrorType(Exception e) {
        String className = e.getClass().getSimpleName();
        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            if (message.contains("connection") && message.contains("refused")) {
                return "连接被拒绝";
            } else if (message.contains("timeout")) {
                return "连接超时";
            } else if (message.contains("reset")) {
                return "连接被重置";
            } else if (message.contains("not found")) {
                return "地址未找到";
            } else if (message.contains("ssl")) {
                return "SSL证书错误";
            } else if (message.contains("dns")) {
                return "DNS解析失败";
            }
        }
        return className;
    }

    /**
     * 生成用户友好的错误信息
     */
    private String getUserFriendlyErrorMessage(Exception e, String endpoint, String method) {
        StringBuilder message = new StringBuilder();

        String errorType = getErrorType(e);
        message.append("网络请求失败 (").append(errorType).append(")\n");
        message.append("操作: ").append(method).append(" ").append(endpoint).append("\n");

        if (e.getMessage() != null) {
            String detail = e.getMessage();
            if (detail.contains("Connection refused")) {
                message.append("错误原因: 无法连接到服务器\n");
                message.append("可能原因:\n");
                message.append("1. 服务器未启动\n");
                message.append("2. 服务器地址配置错误\n");
                message.append("3. 防火墙阻止了连接\n");
                message.append("\n解决方案:\n");
                message.append("1. 确认服务器是否已启动\n");
                message.append("2. 检查配置文件中的服务器地址\n");
                message.append("3. 检查网络连接和防火墙设置\n");
            } else if (detail.contains("connect timed out")) {
                message.append("错误原因: 连接超时\n");
                message.append("可能原因:\n");
                message.append("1. 网络延迟过高\n");
                message.append("2. 服务器响应缓慢\n");
                message.append("3. 服务器负载过高\n");
                message.append("\n解决方案:\n");
                message.append("1. 检查网络连接\n");
                message.append("2. 稍后重试\n");
                message.append("3. 联系管理员检查服务器状态\n");
            } else if (detail.contains("connection reset")) {
                message.append("错误原因: 连接被重置\n");
                message.append("可能原因:\n");
                message.append("1. 服务器意外断开\n");
                message.append("2. 网络不稳定\n");
                message.append("3. 防火墙或代理问题\n");
                message.append("\n解决方案:\n");
                message.append("1. 重新连接\n");
                message.append("2. 检查网络稳定性\n");
                message.append("3. 检查防火墙设置\n");
            } else if (detail.contains("SSL")) {
                message.append("错误原因: SSL证书错误\n");
                message.append("可能原因:\n");
                message.append("1. 证书过期或无效\n");
                message.append("2. 证书链不完整\n");
                message.append("3. 服务器配置错误\n");
                message.append("\n解决方案:\n");
                message.append("1. 联系管理员更新证书\n");
                message.append("2. 检查系统时间是否正确\n");
                message.append("3. 联系技术支持\n");
            } else if (detail.contains("Unknown host")) {
                message.append("错误原因: 无法解析服务器地址\n");
                message.append("可能原因:\n");
                message.append("1. 服务器地址配置错误\n");
                message.append("2. DNS解析失败\n");
                message.append("3. 网络配置问题\n");
                message.append("\n解决方案:\n");
                message.append("1. 检查服务器地址配置\n");
                message.append("2. 检查DNS设置\n");
                message.append("3. 尝试使用IP地址\n");
            } else {
                message.append("错误详情: ").append(detail).append("\n");
                message.append("\n建议解决方案:\n");
                message.append("1. 检查网络连接\n");
                message.append("2. 重新尝试操作\n");
                message.append("3. 联系技术支持\n");
            }
        } else {
            message.append("未知错误，请联系技术支持\n");
        }

        return message.toString();
    }

    /**
     * 获取请求统计信息
     */
    public Map<String, Object> getRequestStats() {
        long total = totalRequests.get();
        long failed = failedRequests.get();
        double successRate = total > 0 ? (total - failed) * 100.0 / total : 100.0;

        return Map.of(
                "totalRequests", total,
                "failedRequests", failed,
                "successRate", String.format("%.2f%%", successRate)
        );
    }

    public Map<String, Object> login(String username, String password) throws Exception {
        LogUtil.debug("用户登录尝试: " + username);
        
        // 使用ContextManager设置用户上下文
        ContextManager.setUserContext(username);
        String requestId = "LOGIN-" + System.currentTimeMillis();
        ContextManager.setRequestId(requestId);

        try {
            Map<String, String> credentials = Map.of(
                    "username", username,
                    "password", password
            );

            String response = post("/auth/login", credentials);
            Map<String, Object> result = JsonUtil.parseJson(response);

            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                this.token = (String) data.get("token");
                LogUtil.info("用户登录成功: " + username);
            } else {
                LogUtil.warn("用户登录失败: " + username);
            }

            return result;
        } finally {
            ContextManager.clear();
        }
    }
}