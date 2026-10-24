package com.hundunlab.site.common.exception.MyException;

public class SqlOperationException extends RuntimeException {
    public SqlOperationException(String message) {
        super(message);
    }
}
