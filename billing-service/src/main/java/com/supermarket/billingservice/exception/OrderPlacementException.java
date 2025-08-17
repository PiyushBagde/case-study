package com.supermarket.billingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class OrderPlacementException extends RuntimeException {
    public OrderPlacementException(String message) {
        super(message);
    }
    public OrderPlacementException(String message, Throwable cause) {
        super(message, cause);
    }
}