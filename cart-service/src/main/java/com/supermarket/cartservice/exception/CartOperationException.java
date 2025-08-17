package com.supermarket.cartservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Using BAD_REQUEST, could also be CONFLICT depending on the specific violation
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CartOperationException extends RuntimeException {

    public CartOperationException(String message) {
        super(message);
    }

    public CartOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}