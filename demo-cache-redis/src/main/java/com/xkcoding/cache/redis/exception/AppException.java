package com.xkcoding.cache.redis.exception;

public class AppException extends RuntimeException{
    private int errorCode;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(String message, Throwable cause) {
        super(message,cause);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
