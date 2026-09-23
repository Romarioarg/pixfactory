package com.pixfactory.exception;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final int status;
    private final Map<String, Object> details;

    public ApiException(int status, String message) {
        this(status, message, null);
    }

    public ApiException(int status, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public int getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
