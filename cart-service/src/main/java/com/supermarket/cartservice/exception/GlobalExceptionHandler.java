package com.supermarket.cartservice.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
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
        log.warn("[{}] Resource not found: {}", serviceName, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<Object> handleOperationFailedException(OperationFailedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getCause() instanceof FeignException) {
            FeignException fe = (FeignException) ex.getCause();
            if (fe.status() == -1 || fe.status() >= 500) {
                status = HttpStatus.BAD_GATEWAY;
            }
        }
        Map<String, Object> body = createErrorBody(status, ex.getMessage(), request);
        log.error("[{}] Operation failed: {}", serviceName, ex.getMessage());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(CartOperationException.class)
    public ResponseEntity<Object> handleCartOperationException(CartOperationException ex, WebRequest request) {
        // Determine status based on the nature of the exception if needed, default to BAD_REQUEST
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> body = createErrorBody(status, ex.getMessage(), request);
        log.warn("[{}] Cart operation exception: {}", serviceName, ex.getMessage());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        log.warn("[{}] Invalid argument: {}", serviceName, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FeignException.NotFound.class) // Specifically handle 404 from downstream service
    public ResponseEntity<Object> handleFeignNotFoundException(FeignException.NotFound ex, WebRequest request) {
        String message = "Required resource not found in downstream service. " + ex.getMessage();
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, message, request);
        log.warn("[{}] Downstream resource not found: {}", serviceName, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FeignException.class) // Handle other Feign errors (like 5xx, connection refused)
    public ResponseEntity<Object> handleFeignException(FeignException ex, WebRequest request) {
        HttpStatus status;
        String message;
        // Distinguish between server errors from downstream and connection issues
        if (ex.status() >= 500) {
            status = HttpStatus.BAD_GATEWAY; // Or SERVICE_UNAVAILABLE
            message = "Downstream service [" + ex.request().url().split("/")[2] + "] reported an error.";
        } else if (ex.status() > 0) { // Handle other client errors (4xx other than 404) if needed
            status = HttpStatus.resolve(ex.status()) != null ? HttpStatus.resolve(ex.status()) : HttpStatus.INTERNAL_SERVER_ERROR; // Resolve status or default
            message = "Error communicating with downstream service [" + ex.request().url().split("/")[2] + "]: Client error " + ex.status();
        }
        else { // status = -1 typically means connection refused or timeout
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream service [" + ex.request().url().split("/")[2] + "] is unavailable.";
        }

        Map<String, Object> body = createErrorBody(status, message, request);
        log.error("[{}] Feign error ({}): {}", serviceName, ex.status(), ex.getMessage(), ex);
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccessException(DataAccessException ex, WebRequest request) {
        String message = "A database error occurred. Please try again later.";
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
        log.error("[{}] Database access error: {}", serviceName, ex.getMessage(), ex);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // validation exception
    @Override // Override default handler for better structure
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation failed on request body", request);
        body.put("errors", errors); // Add detailed field errors

        log.warn("[{}] Validation failed for @RequestBody: {}", serviceName, errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class) // Handles validation on params/paths
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        // Collect errors: field -> message
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString().substring(cv.getPropertyPath().toString().lastIndexOf('.') + 1), // Simplified field name
                        cv -> cv.getMessage() != null ? cv.getMessage() : "Invalid value",
                        (existing, replacement) -> existing // In case of duplicates, keep the first one
                ));

        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation failed on request parameters or path variables", request);
        body.put("errors", errors);

        log.warn("[{}] Validation failed for parameters/path variables: {}", serviceName, errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        String message = "An unexpected internal error occurred in the cart service.";
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
        log.error("[{}] Unhandled exception occurred: {}", serviceName, ex.getMessage(), ex);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
