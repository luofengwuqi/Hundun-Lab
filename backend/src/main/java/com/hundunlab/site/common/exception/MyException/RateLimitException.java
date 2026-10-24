package com.hundunlab.site.common.exception.MyException;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
