package com.supermarket.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Defaulting to BAD_REQUEST, adjust if needed for specific scenarios
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message) {
        super(message);
    }
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}