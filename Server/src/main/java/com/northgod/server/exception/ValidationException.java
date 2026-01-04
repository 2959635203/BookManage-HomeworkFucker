package com.northgod.server.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, Object> details;

    public ValidationException(String message) {
        super(message);
        this.details = null;
    }

    public ValidationException(String message, Map<String, Object> details) {
        super(message);
        this.details = details;
    }

    public ValidationException(String message, Throwable cause, Map<String, Object> details) {
        super(message, cause);
        this.details = details;
    }
}