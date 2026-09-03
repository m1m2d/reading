package com.cloudread.common;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(Result.CODE_BAD_REQUEST, message);
    }

    public int getCode() {
        return code;
    }
}
