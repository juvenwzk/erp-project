package com.kangcode.exception;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(400, message);
    }
}
