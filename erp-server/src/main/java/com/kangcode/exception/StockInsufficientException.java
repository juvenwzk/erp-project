package com.kangcode.exception;

public class StockInsufficientException extends BusinessException {

    public StockInsufficientException(String message) {
        super(409, message);
    }
}
