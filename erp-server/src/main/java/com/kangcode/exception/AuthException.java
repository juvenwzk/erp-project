package com.kangcode.exception;

public class AuthException extends BusinessException {

    public AuthException(String message) {
        super(401, message);
    }
}
