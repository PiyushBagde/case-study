package com.supermarket.inventoryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR) // Optional
public class OperationFailedException extends RuntimeException {

    public OperationFailedException(String message) {
        super(message);
    }
}