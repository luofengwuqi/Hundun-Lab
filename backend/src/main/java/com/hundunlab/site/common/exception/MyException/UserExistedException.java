package com.hundunlab.site.common.exception.MyException;

public class UserExistedException extends RuntimeException {
    public UserExistedException(String message) {
        super(message);
    }
}
