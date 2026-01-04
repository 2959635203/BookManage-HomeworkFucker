package com.northgod.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient 配置类
 * 为整个应用提供统一配置的 HTTP 客户端 - 已适配 Spring Boot 4.0.1 API
 */
@Configuration
public class RestClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestClientConfig.class);

    /**
     * 配置 RestClient.Builder Bean
     * 其他组件可以通过注入此 Builder 来创建具有统一基础配置的 RestClient 实例
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        logger.info("配置全局 RestClient.Builder");

        // 创建并配置请求工厂
        JdkClientHttpRequestFactory requestFactory = createJdkClientHttpRequestFactory();

        // 构建带有通用配置的 RestClient Builder
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "Bookstore-Server/1.0 (Spring-Boot/4.0.1)")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .requestInterceptor(loggingInterceptor());
    }

    /**
     * 创建并配置 JdkClientHttpRequestFactory
     * 适配 Spring Boot 4.0.1 的 API
     */
    private JdkClientHttpRequestFactory createJdkClientHttpRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();

        // 方法1：使用 Duration 设置超时（推荐，更现代的API）
        try {
            // 尝试使用 Duration 参数的方法
            factory.getClass()
                    .getMethod("setConnectTimeout", Duration.class)
                    .invoke(factory, Duration.ofSeconds(10));

            factory.getClass()
                    .getMethod("setReadTimeout", Duration.class)
                    .invoke(factory, Duration.ofSeconds(30));

            logger.debug("使用 Duration 参数设置 JdkClientHttpRequestFactory 超时");
        } catch (NoSuchMethodException e) {
            // 方法2：回退到旧的毫秒参数方法
            try {
                factory.getClass()
                        .getMethod("setConnectTimeout", int.class)
                        .invoke(factory, 10000); // 10秒

                factory.getClass()
                        .getMethod("setReadTimeout", int.class)
                        .invoke(factory, 30000); // 30秒

                logger.debug("使用 int(毫秒) 参数设置 JdkClientHttpRequestFactory 超时");
            } catch (Exception ex) {
                logger.warn("无法设置 JdkClientHttpRequestFactory 超时，将使用默认值", ex);
            }
        } catch (Exception e) {
            logger.warn("设置 JdkClientHttpRequestFactory 超时时发生异常", e);
        }

        return factory;
    }

    /**
     * 可选：提供一个配置好的 RestClient Bean 直接使用
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        logger.info("创建全局 RestClient Bean");
        return builder.build();
    }

    /**
     * 请求日志拦截器
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
            logger.debug("[{}] RestClient 请求: {} {}", requestId, request.getMethod(), request.getURI());

            long startTime = System.currentTimeMillis();
            try {
                var response = execution.execute(request, body);
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("[{}] 请求成功，状态码: {}，耗时: {}ms",
                        requestId, response.getStatusCode().value(), duration);
                return response;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("[{}] 请求失败，耗时: {}ms，错误: {}",
                        requestId, duration, e.getMessage(), e);
                throw e;
            }
        };
    }

    /**
     * 可选：配置虚拟线程支持的请求工厂（Java 21+）
     */
    @Bean
    public JdkClientHttpRequestFactory virtualThreadRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();

        // 在 Java 21+ 中，JdkClientHttpRequestFactory 默认使用 HttpClient
        // 它已经支持虚拟线程，无需特殊配置

        // 设置合理的超时
        try {
            // 尝试新API
            factory.getClass()
                    .getMethod("setConnectTimeout", Duration.class)
                    .invoke(factory, Duration.ofSeconds(5));

            factory.getClass()
                    .getMethod("setReadTimeout", Duration.class)
                    .invoke(factory, Duration.ofSeconds(15));
        } catch (Exception e) {
            // 忽略，使用默认值
            logger.debug("使用默认超时设置");
        }

        logger.info("创建支持虚拟线程的 JdkClientHttpRequestFactory");
        return factory;
    }
}