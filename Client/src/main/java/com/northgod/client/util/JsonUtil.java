package com.northgod.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 将对象转换为JSON字符串
     * @param obj 要转换的对象
     * @return JSON字符串
     * @throws JsonProcessingException 如果转换失败
     */
    public static String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 将对象转换为JSON字符串（安全版本，失败返回空JSON）
     * @param obj 要转换的对象
     * @return JSON字符串，失败时返回"{}"
     */
    public static String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LogUtil.error("JSON序列化失败: " + obj.getClass().getSimpleName(), e);
            return "{}";
        }
    }

    /**
     * 解析JSON字符串为Map
     * @param json JSON字符串
     * @return Map对象，解析失败返回null
     */
    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            LogUtil.warn("尝试解析空JSON字符串");
            return null;
        }
        
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            LogUtil.error("JSON解析失败: " + json.substring(0, Math.min(100, json.length())), e);
            return null;
        }
    }

    /**
     * 解析JSON字符串为指定类型
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 解析后的对象，失败返回null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            LogUtil.warn("尝试解析空JSON字符串为 " + clazz.getSimpleName());
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            LogUtil.error("JSON解析失败，目标类型: " + clazz.getSimpleName(), e);
            return null;
        }
    }
}