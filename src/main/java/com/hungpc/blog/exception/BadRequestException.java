package com.hungpc.blog.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final String errorCode;
    private final String hint;
    private final String suggestion;

    public BadRequestException(String message) {
        super(message);
        this.errorCode = "BAD_REQUEST";
        this.hint = null;
        this.suggestion = null;
    }

    public BadRequestException(String errorCode, String message, String hint, String suggestion) {
        super(message);
        this.errorCode = errorCode;
        this.hint = hint;
        this.suggestion = suggestion;
    }
}
