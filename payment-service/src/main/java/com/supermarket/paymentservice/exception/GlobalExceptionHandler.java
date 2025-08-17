package com.supermarket.paymentservice.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${spring.application.name}")
    private String serviceName;

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> createErrorBody(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("service", serviceName);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<Object> handleOperationFailedException(OperationFailedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getCause() instanceof FeignException fe) {
            if (fe.status() == -1 || fe.status() >= 500) {
                status = HttpStatus.BAD_GATEWAY;
            }
        }
        Map<String, Object> body = createErrorBody(status, ex.getMessage(), request);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Object> handlePaymentProcessingException(PaymentProcessingException ex, WebRequest request) {
        // Determine status based on the specific exception context if needed, else default
        HttpStatus status = HttpStatus.BAD_REQUEST; // Or derive from exception if it carries status info
        Map<String, Object> body = createErrorBody(status, ex.getMessage(), request);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Object> handleFeignNotFoundException(FeignException.NotFound ex, WebRequest request) {
        // Context: Likely means Order was not found in Billing service
        String message = "Required resource not found in Billing Service. " + ex.getMessage();
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, message, request);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignException(FeignException ex, WebRequest request) {
        HttpStatus status;
        String downstreamServiceName = "Billing Service";
        try {
            downstreamServiceName = ex.request().url().split("/")[2];
        } catch (Exception ignored) {
        }
        String message;

        if (ex.status() >= 500) {
            status = HttpStatus.BAD_GATEWAY;
            message = "Downstream service [" + downstreamServiceName + "] reported an error.";
        } else if (ex.status() > 0) {
            status = HttpStatus.resolve(ex.status()) != null ? HttpStatus.resolve(ex.status()) : HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error communicating with downstream service [" + downstreamServiceName + "]: Client error " + ex.status();
        } else {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream service [" + downstreamServiceName + "] is unavailable.";
        }

        Map<String, Object> body = createErrorBody(status, message, request);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccessException(DataAccessException ex, WebRequest request) {
        String message = "A database error occurred while processing the payment transaction.";
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class) // Handles validation on params/paths
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString().substring(cv.getPropertyPath().toString().lastIndexOf('.') + 1), // Simplified field name
                        cv -> cv.getMessage() != null ? cv.getMessage() : "Invalid value",
                        (existing, replacement) -> existing // In case of duplicates, keep the first one
                ));

        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation failed on request parameters or path variables", request);
        body.put("errors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        String message = "An unexpected internal error occurred in the payment service.";
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

